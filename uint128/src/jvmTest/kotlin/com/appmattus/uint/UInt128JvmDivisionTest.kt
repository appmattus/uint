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

class UInt128JvmDivisionTest {

    private val random = Random(42)

    // Covers dividends and divisors either side of the sign bit, where the halving and correction can go wrong
    private val edges = listOf(1uL, 2uL, 3uL, Long.MAX_VALUE.toULong(), 1uL shl 63, (1uL shl 63) + 1uL, ULong.MAX_VALUE - 1uL, ULong.MAX_VALUE)

    private fun check(dividend: ULong, divisor: ULong) {
        val expected = java.lang.Long.divideUnsigned(dividend.toLong(), divisor.toLong()).toULong()
        assertEquals(expected, divideUnsignedWithoutBigInteger(dividend, divisor), "$dividend / $divisor")
    }

    @Test
    fun divideUnsignedWithoutBigIntegerMatchesLongForEdgeValues() {
        (edges + 0uL).forEach { dividend -> edges.forEach { divisor -> check(dividend, divisor) } }
    }

    @Test
    fun divideUnsignedWithoutBigIntegerMatchesLongForRandomValues() {
        repeat(10_000) {
            val divisor = (random.nextULong() shr random.nextInt(64)).coerceAtLeast(1uL)
            check(random.nextULong(), divisor)
        }
    }
}
