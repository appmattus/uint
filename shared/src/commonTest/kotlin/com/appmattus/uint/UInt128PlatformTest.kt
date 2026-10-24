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
import kotlin.test.fail

class UInt128PlatformTest {

    private val random = Random(42)

    // Mixes full-width values with ones that fit in a single word, to exercise the carry and short-divisor paths
    private fun randomUInt128(): UInt128 = when (random.nextInt(3)) {
        0 -> UInt128(random.nextULong(), random.nextULong())
        1 -> UInt128(0uL, random.nextULong())
        else -> UInt128(random.nextULong() shr random.nextInt(64), random.nextULong())
    }

    @Test
    fun multiplyHighMatchesPortableImplementation() {
        repeat(10_000) {
            val x = random.nextULong()
            val y = random.nextULong()
            assertMatches(uint64MultiplyHighPortable(x, y), uint64MultiplyHigh(x, y)) { "$x * $y" }
        }
    }

    @Test
    fun divRemSatisfiesDivisionIdentity() {
        repeat(10_000) {
            // Dividends just either side of a multiple of the divisor exercise the quotient estimate's corrections
            val b = randomUInt128().takeIf { it != UInt128.ZERO } ?: UInt128.ONE
            val multiple = b * randomUInt128()
            val a = when (random.nextInt(3)) {
                0 -> multiple
                1 -> multiple - UInt128.ONE
                else -> multiple + random.nextULong(16uL)
            }
            val q = a / b
            val r = a % b
            if (r >= b || q * b + r != a) fail("$a / $b = $q remainder $r")
        }
    }

    // Only builds the message on failure, to keep the loop cheap on slower platforms
    private fun <T> assertMatches(expected: T, actual: T, message: () -> String) {
        if (expected != actual) assertEquals(expected, actual, message())
    }
}
