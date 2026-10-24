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
import kotlin.test.assertFailsWith
import kotlin.test.fail

/** Checks each scalar operator against the same operation on the scalar widened to a [UInt128]. */
class UInt128ScalarTest {

    private val random = Random(42)

    private fun randomUInt128(): UInt128 = when (random.nextInt(3)) {
        0 -> UInt128(random.nextULong(), random.nextULong())
        1 -> UInt128(0uL, random.nextULong())
        else -> UInt128.MAX_VALUE - random.nextULong(16uL)
    }

    private fun randomULong(): ULong = when (random.nextInt(3)) {
        0 -> random.nextULong()
        1 -> random.nextULong() shr 32
        else -> random.nextULong(256uL)
    }.coerceAtLeast(1uL)

    @Test
    fun scalarOperatorsMatchWidenedOperators() {
        repeat(1_000) {
            val a = randomUInt128()
            val l = randomULong()
            val w = l.toUInt128()
            if (a + l != a + w || a - l != a - w || a * l != a * w || a / l != a / w || a % l != a % w ||
                a.mod(l) != (a % w).toULong() || a.compareTo(l).coerceIn(-1, 1) != a.compareTo(w).coerceIn(-1, 1)
            ) {
                fail("$a op $l")
            }
        }
    }

    @Test
    fun narrowScalarsMatchULong() {
        val a = UInt128(0x0123456789abcdefuL, 0xfedcba9876543210uL)
        assertEquals(a % 1_000_000_007uL, a % 1_000_000_007u)
        assertEquals((a % 65_521uL).toULong().toUShort(), a.mod(65_521u.toUShort()))
        assertEquals((a % 251uL).toULong().toUByte(), a.mod(251u.toUByte()))
        assertEquals(a * 3uL, a * 3u.toUByte())
        assertEquals(a / 7uL, a / 7u.toUShort())
    }

    @Test
    fun carriesAndBorrowsCrossWords() {
        assertEquals(UInt128(1uL, 0uL), UInt128(0uL, ULong.MAX_VALUE) + 1uL)
        assertEquals(UInt128(0uL, ULong.MAX_VALUE), UInt128(1uL, 0uL) - 1uL)
        assertEquals(UInt128.ZERO, UInt128.MAX_VALUE + 1u)
        assertEquals(UInt128.MAX_VALUE, UInt128.ZERO - 1u)
        assertEquals(UInt128(1uL, 0uL), UInt128(0uL, ULong.MAX_VALUE).inc())
        assertEquals(UInt128.MAX_VALUE, UInt128.ZERO.dec())
    }

    @Test
    fun divisionByZeroScalarThrows() {
        val a = UInt128(1uL, 2uL)
        assertFailsWith<ArithmeticException> { a / 0uL }
        assertFailsWith<ArithmeticException> { a % 0u }
        assertFailsWith<ArithmeticException> { a.mod(0u.toUByte()) }
        assertFailsWith<ArithmeticException> { UInt128.ONE / 0uL }
    }
}
