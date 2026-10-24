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

import com.appmattus.uint.UInt128
import com.appmattus.uint.pow
import com.appmattus.uint.toUInt128
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.time.TimeSource

private const val INPUTS = 1024
private const val WARMUP_ROUNDS = 3
private const val MEASURED_ROUNDS = 5
private const val TARGET_NANOS_PER_ROUND = 500_000_000L

private val random = Random(42)
private val wide = Array(INPUTS) { UInt128(random.nextULong(), random.nextULong()) }
private val narrow = Array(INPUTS) { UInt128(0uL, random.nextULong() or 1uL) }
private val small = Array(INPUTS) { UInt128(0uL, random.nextULong() shr 32 or 1uL) }
private val strings = Array(INPUTS) { wide[it].toString() }

/** Accumulates results so the work can't be optimised away. */
private var sink = 0uL

private val benchmarks: List<Pair<String, (Int) -> Unit>> = listOf(
    "plus (unchanged)" to { i -> sink += (wide[i] + wide[i xor 1]).lower },
    "times 128x128" to { i -> sink += (wide[i] * wide[i xor 1]).upper },
    "times 64x64" to { i -> sink += (narrow[i] * narrow[i xor 1]).upper },
    "div 128/64" to { i -> sink += (wide[i] / narrow[i]).lower },
    "div 128/128" to { i -> sink += (wide[i] / (wide[i xor 1] shr 7)).lower },
    "rem 128/32" to { i -> sink += (wide[i] % small[i]).lower },
    "pow(3)" to { i -> sink += narrow[i].pow(3).upper },
    "toString()" to { i -> sink += wide[i].toString().length.toULong() },
    "String.toUInt128()" to { i -> sink += strings[i].toUInt128().lower },
)

/** Returns the best nanoseconds per operation over the measured rounds. */
private fun measure(op: (Int) -> Unit): Double {
    // Calibrate the number of operations per round to roughly hit the target duration
    var ops = INPUTS
    while (true) {
        val start = TimeSource.Monotonic.markNow()
        for (n in 0 until ops) op(n and (INPUTS - 1))
        val elapsed = start.elapsedNow().inWholeNanoseconds
        if (elapsed >= TARGET_NANOS_PER_ROUND / 10) {
            ops = (ops.toLong() * TARGET_NANOS_PER_ROUND / elapsed).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
            break
        }
        ops *= 4
    }

    var best = Double.MAX_VALUE
    repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
        val start = TimeSource.Monotonic.markNow()
        for (n in 0 until ops) op(n and (INPUTS - 1))
        val nanosPerOp = start.elapsedNow().inWholeNanoseconds.toDouble() / ops
        if (round >= WARMUP_ROUNDS && nanosPerOp < best) best = nanosPerOp
    }
    return best
}

/** The names of all benchmarks, in the order they run. */
val benchmarkNames: List<String> = benchmarks.map { it.first }

/** Runs the [names] benchmarks in this process, printing one `name,ns/op` line for each. */
fun runBenchmarks(names: List<String> = benchmarkNames) {
    benchmarks.filter { it.first in names }.forEach { (name, op) ->
        println("$name,${(measure(op) * 100).toLong() / 100.0}")
    }
    println("# sink $sink")
}
