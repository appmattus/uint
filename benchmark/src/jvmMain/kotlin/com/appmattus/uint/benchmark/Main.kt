/*
 * Copyright 2026 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appmattus.uint.benchmark

import java.io.File

private const val FORKS = 2

/**
 * Runs each benchmark in its own fresh JVM, as JMH does, so the JIT's profile of one benchmark can't slow down
 * another. Each benchmark runs in [FORKS] separate JVMs and reports the best.
 */
fun main(args: Array<String>) {
    if (args.isNotEmpty()) return runBenchmarks(args.toList())

    val java = File(System.getProperty("java.home"), "bin/java").absolutePath
    val classpath = System.getProperty("java.class.path")
    println("benchmark,ns/op")
    benchmarkNames.forEach { name ->
        val best = List(FORKS) {
            val process = ProcessBuilder(java, "-cp", classpath, "com.appmattus.uint.benchmark.MainKt", name)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            val output = process.inputStream.bufferedReader().readLines()
            check(process.waitFor() == 0) { "Benchmark '$name' failed" }
            output.first { it.startsWith("$name,") }.substringAfter(',').toDouble()
        }.min()
        println("$name,$best")
    }
}
