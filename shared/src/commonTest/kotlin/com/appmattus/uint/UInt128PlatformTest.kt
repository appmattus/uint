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

package com.appmattus.uint

import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

class UInt128PlatformTest {

    private val random = Random(42)

    // Mixes full-width values with ones that fit in a single word, to exercise the carry and short-divisor paths
    private fun randomUInt128(): UInt128 = when (random.nextInt(3)) {
        0 -> UInt128(random.nextULong(), random.nextULong())
        1 -> UInt128(0uL, random.nextULong())
        else -> UInt128(random.nextULong() shr random.nextInt(64), random.nextULong())
    }

    @Test
    fun timesMatchesPortableImplementation() {
        repeat(10_000) {
            val a = randomUInt128()
            val b = randomUInt128()
            assertMatches(uint128TimesPortable(a, b), uint128Times(a, b)) { "$a * $b" }
        }
    }

    @Test
    fun divModMatchesPortableImplementation() {
        repeat(10_000) {
            val a = randomUInt128()
            val b = randomUInt128().takeIf { it != UInt128.ZERO } ?: UInt128.ONE
            assertMatches(uint128DivModPortable(a, b), uint128PlatformDivMod(a, b)) { "$a / $b" }
        }
    }

    // Only builds the message on failure, as toString is expensive with the portable implementation
    private fun <T> assertMatches(expected: T, actual: T, message: () -> String) {
        if (expected != actual) assertEquals(expected, actual, message())
    }
}
