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

import com.appmattus.uint.UInt256.Companion.MAX_VALUE
import com.appmattus.uint.UInt256.Companion.ONE
import com.appmattus.uint.UInt256.Companion.ZERO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UInt256Test {

    private val max128 = UInt128.MAX_VALUE
    private val value = UInt256(UInt128(0x0123456789abcdefuL, 0xfedcba9876543210uL), UInt128(0x0f1e2d3c4b5a6978uL, 0x8796a5b4c3d2e1f0uL))

    @Test
    fun constants() {
        assertEquals(256, UInt256.SIZE_BITS)
        assertEquals(32, UInt256.SIZE_BYTES)
        assertEquals(ZERO, UInt256.MIN_VALUE)
        assertEquals(UInt256(max128, max128), MAX_VALUE)
        assertEquals(UInt256(UInt128.ZERO, UInt128.ONE), ONE)
    }

    @Test
    fun upperAndLowerRoundTrip() {
        assertEquals(UInt128(0x0123456789abcdefuL, 0xfedcba9876543210uL), value.upper)
        assertEquals(UInt128(0x0f1e2d3c4b5a6978uL, 0x8796a5b4c3d2e1f0uL), value.lower)
        assertEquals(value, UInt256(value.upper, value.lower))
    }

    @Test
    fun conversionsFromUnsignedTypes() {
        assertEquals(UInt256(UInt128.ZERO, 255u.toUInt128()), UByte.MAX_VALUE.toUInt256())
        assertEquals(UInt256(UInt128.ZERO, 65535u.toUInt128()), UShort.MAX_VALUE.toUInt256())
        assertEquals(UInt256(UInt128.ZERO, UInt.MAX_VALUE.toUInt128()), UInt.MAX_VALUE.toUInt256())
        assertEquals(UInt256(UInt128.ZERO, ULong.MAX_VALUE.toUInt128()), ULong.MAX_VALUE.toUInt256())
        assertEquals(UInt256(UInt128.ZERO, max128), max128.toUInt256())
    }

    @Test
    fun conversionsFromSignedTypesSignExtend() {
        assertEquals(MAX_VALUE, (-1).toByte().toUInt256())
        assertEquals(MAX_VALUE, (-1).toShort().toUInt256())
        assertEquals(MAX_VALUE, (-1).toUInt256())
        assertEquals(MAX_VALUE, (-1L).toUInt256())
        assertEquals(127u.toUInt256(), Byte.MAX_VALUE.toUInt256())
        assertEquals(Long.MAX_VALUE.toULong().toUInt256(), Long.MAX_VALUE.toUInt256())
        assertEquals(UInt256(UInt128.ZERO, 23u.toUInt128()), 23.toUInt256())
    }

    @Test
    fun conversionsToSmallerTypesTruncate() {
        assertEquals(0xf0.toByte(), value.toByte())
        assertEquals(0xe1f0.toShort(), value.toShort())
        assertEquals(0xc3d2e1f0.toInt(), value.toInt())
        assertEquals(0x8796a5b4c3d2e1f0uL.toLong(), value.toLong())
        assertEquals(0xf0u.toUByte(), value.toUByte())
        assertEquals(0xe1f0u.toUShort(), value.toUShort())
        assertEquals(0xc3d2e1f0u, value.toUInt())
        assertEquals(0x8796a5b4c3d2e1f0uL, value.toULong())
        assertEquals(value.lower, value.toUInt128())
    }

    @Test
    fun arithmeticWrapsAround() {
        assertEquals(ZERO, MAX_VALUE + ONE)
        assertEquals(MAX_VALUE, ZERO - ONE)
        assertEquals(ZERO, MAX_VALUE.inc())
        assertEquals(MAX_VALUE, ZERO.dec())
        assertEquals(ONE, MAX_VALUE * MAX_VALUE)
        assertEquals(MAX_VALUE, -ONE)
        assertEquals(value, +value)
        assertEquals(ZERO, value + -value)
        assertEquals(ZERO, MAX_VALUE + 1u)
        assertEquals(ZERO - (5678 - 1234).toUInt256(), 1234.toUInt256() - 5678.toUInt256())
        assertEquals(MAX_VALUE - ONE, MAX_VALUE * 2.toUInt256())
        assertEquals(MAX_VALUE - ONE, 2.toUInt256() * MAX_VALUE)
    }

    @Test
    fun smallValues() {
        assertEquals((5678 + 1234).toUInt256(), 5678.toUInt256() + 1234.toUInt256())
        assertEquals((5678 + 1234).toUInt256(), 1234.toUInt256() + 5678.toUInt256())
        assertEquals((5678 - 1234).toUInt256(), 5678.toUInt256() - 1234.toUInt256())
        assertEquals((1234 * 5678).toUInt256(), 1234.toUInt256() * 5678.toUInt256())
        assertEquals((1234 * 5678).toUInt256(), 5678.toUInt256() * 1234.toUInt256())
        assertEquals(ZERO, ZERO * 5678.toUInt256())
        assertEquals(ZERO, 5678.toUInt256() * ZERO)
    }

    @Test
    fun carriesCrossEveryWord() {
        for (shift in listOf(64, 128, 192)) {
            val boundary = ONE shl shift
            assertEquals(boundary, (boundary - 1u) + 1u)
            assertEquals(boundary - 1u, boundary - 1uL)
            assertEquals(boundary, (boundary - ONE) + ONE)
        }
    }

    @Test
    fun multiplicationAcrossWords() {
        val twoPow128 = ONE shl 128
        assertEquals(ONE shl 192, twoPow128 * (ONE shl 64))
        assertEquals(ZERO, twoPow128 * twoPow128)
        // (2^128 - 1)^2 = 2^256 - 2^129 + 1
        assertEquals(ZERO - (ONE shl 129) + ONE, max128.toUInt256() * max128.toUInt256())
    }

    @Test
    fun shifts() {
        assertEquals(value, value shl 0)
        assertEquals(value, value shl 256)
        assertEquals(value shl 1, value shl 257)
        assertEquals(value shl 1, value shl -255) // Only the eight lowest-order bits of the distance are used
        assertEquals(ONE shl 255, ONE shl 255)
        assertEquals(ONE, (ONE shl 255) shr 255)
        assertEquals(ZERO, ONE shr 1)
        for (n in listOf(1, 63, 64, 65, 127, 128, 129, 191, 192, 193, 255)) {
            assertEquals(value, ((value shr n) shl n) or (value and ((ONE shl n) - ONE)), "shift $n")
        }
    }

    @Test
    fun bitwise() {
        assertEquals(ZERO, value and value.inv())
        assertEquals(MAX_VALUE, value or value.inv())
        assertEquals(MAX_VALUE, value xor value.inv())
        assertEquals(value, value.inv().inv())
    }

    @Test
    fun comparisons() {
        assertTrue(MAX_VALUE > ZERO)
        assertTrue(ONE shl 192 > MAX_VALUE shr 64)
        assertTrue(value > ULong.MAX_VALUE)
        assertTrue(value > max128)
        assertTrue(ULong.MAX_VALUE.toUInt256() < (ONE shl 64))
        assertEquals(0, 5u.toUInt256().compareTo(5u))
        assertEquals(0, max128.toUInt256().compareTo(max128))
        assertTrue(5u.toUInt256() < 6uL)
        assertTrue(5u.toUInt256() > 4u.toUByte())
        assertEquals(5678.toUInt256(), maxOf(1234.toUInt256(), 5678.toUInt256()))
        assertEquals(5678.toUInt256(), maxOf(5678.toUInt256(), 1234.toUInt256()))
        assertEquals(1234.toUInt256(), minOf(1234.toUInt256(), 5678.toUInt256()))
        assertEquals(1234.toUInt256(), minOf(5678.toUInt256(), 1234.toUInt256()))
    }

    @Test
    fun equalsAndHashCode() {
        assertEquals(value, UInt256(value.upper, value.lower))
        assertEquals(value.hashCode(), UInt256(value.upper, value.lower).hashCode())
        assertNotEquals(value, value + 1u)
        assertNotEquals<Any>(value, value.lower)
        assertNotEquals(UInt256(UInt128.ONE, UInt128.ZERO), UInt256(UInt128.ZERO, UInt128.ONE))
    }

    @Test
    fun divisionByZeroThrows() {
        assertFailsWith<ArithmeticException> { value / ZERO }
        assertFailsWith<ArithmeticException> { value % ZERO }
        assertFailsWith<ArithmeticException> { value / 0uL }
        assertFailsWith<ArithmeticException> { value.mod(0u) }
        assertFailsWith<ArithmeticException> { value / UInt128.ZERO }
    }

    @Test
    fun oneDividedByTwoToThe64() {
        val divisor = ONE shl 64
        assertEquals(ZERO, ONE / divisor)
        assertEquals(ONE, ONE % divisor)
    }

    @Test
    fun bytesInBigEndianOrder() {
        val input = "102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f".toUInt256(16)
        for (i in 0 until 32) {
            assertEquals(i.toUByte(), (input shr (8 * (31 - i))).toUByte(), "byte $i")
        }
    }

    @Test
    fun smallPowers() {
        assertEquals((2 * 2).toUInt256(), 2.toUInt256().pow(2))
        assertEquals((4 * 4 * 4).toUInt256(), 4.toUInt256().pow(3))
        assertEquals((6 * 6 * 6 * 6 * 6).toUInt256(), 6.toUInt256().pow(5))
        assertEquals((3 * 3 * 3).toUInt256(), 3.toUInt256().pow(3))
    }

    @Test
    fun unsignedComparisonOfTopValues() {
        val nearMax = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe".toUInt256(16)
        assertTrue(nearMax > ZERO)
        assertTrue(ZERO < nearMax)
    }

    // Two arbitrary values, with known results for them in the following tests

    private val r1 = "7D1DE5EAF9B156D53208F033B5AA8122D2d2355d5e12292b121156cfdb4a529c".toUInt256(16)
    private val r2 = "d781cab4f072134971da2d19a3473013bfb69ca6c30a7e26406ba5477c1d3270".toUInt256(16)
    private val r1Low64 = 0x121156cfdb4a529cuL
    private val half = ONE shl 255

    private fun UInt256.hex() = toString(16).padStart(64, '0')

    @Test
    fun vectorBasics() {
        assertNotEquals(r1, r2)
        assertNotEquals(ZERO, ONE)
        assertNotEquals(MAX_VALUE, ZERO)
        assertEquals(ZERO, MAX_VALUE.inv())
        assertEquals(r2, (r1 xor r2) xor r1)

        val tmp64 = 0xc4dab720d9c7acaauL
        for (i in 0 until 256) {
            assertNotEquals(ZERO, ONE shl i)
            assertNotEquals(r1, r1 xor (ONE shl i))
            assertNotEquals(tmp64.toUInt256(), tmp64.toUInt256() xor (ONE shl i))
        }
        for (value in listOf(r1, r2)) assertEquals(value, value.hex().toUInt256(16))

        assertEquals(r1Low64.toUInt256(), r1 and ULong.MAX_VALUE.toUInt256())
    }

    @Test
    fun vectorShifts() {
        for (i in 0 until 256) {
            assertEquals(ONE shl i, half shr (255 - i))
        }
        val c1 = 0x0123456789abcdefuL.toUInt256()
        val c2 = c1 shl 128
        for (i in 0 until 128) assertEquals(c1 shl i, c2 shr (128 - i))
        for (i in 128 until 256) assertEquals(c1 shl i, c2 shl (i - 128))
    }

    @Test
    fun vectorUnaryOperators() {
        assertEquals(MAX_VALUE, ZERO.inv())
        assertEquals(r1 xor MAX_VALUE, r1.inv())
        assertEquals(ZERO, -ZERO)
        assertEquals(r1.inv() + ONE, -r1)
        for (i in 0 until 256) assertEquals(MAX_VALUE shl i, -(ONE shl i))
    }

    @Test
    fun vectorComparison() {
        for (i in 0 until 256) {
            val tmp = ONE shl i
            assertTrue(tmp >= ZERO)
            assertTrue(tmp > ZERO)
            assertTrue(ZERO < tmp)
            assertTrue(tmp >= 0u)
            assertTrue(tmp > 0u)
            val withR1 = tmp or r1
            assertTrue(withR1 >= r1)
            assertTrue((withR1 == r1) != (withR1 > r1))
            assertTrue(withR1 == r1 || !(withR1 <= r1))
            assertTrue(r1 <= withR1)
            assertFalse(withR1 < r1)
            assertFalse(r1 > withR1)
        }
    }

    @Test
    fun vectorPlusMinus() {
        assertEquals("549fb09fea236a1ea3e31d4d58f1b1369288d204211ca751527cfc175767850c".toUInt256(16), r1 + r2)
        assertEquals(ZERO, ONE + MAX_VALUE)
        for (i in 1 until 256) {
            assertEquals(half shr (i - 1), (MAX_VALUE shr i) + ONE)
            assertEquals(half shr (i - 1), ONE + (MAX_VALUE shr i))
            assertEquals(half shr (i - 1), (MAX_VALUE shr i) + 1u)
            assertEquals(half shr (i - 1), (MAX_VALUE shr i).inc())
            assertEquals(half shr (i - 1), (MAX_VALUE shr i) - (-ONE))
            assertEquals(MAX_VALUE shr i, (half shr (i - 1)) - ONE)
            assertEquals(MAX_VALUE shr i, (half shr (i - 1)).dec())
        }
        val a = 0xbedc77e27940a7uL
        val b = 0xee8d836fce66fbuL
        assertEquals((a + b).toUInt256(), a.toUInt256() + b)
        assertEquals(a.toUInt256(), (a + b).toUInt256() - b)
        assertEquals(r1 + ONE, r1.inc())
        assertEquals(r1 + r2, r1 - (-r2))
        assertEquals(r1 + ONE, r1 - (-ONE))
        assertEquals(r1 + (-ONE), r1 - ONE)
        assertEquals(r1 - ONE, r1.dec())
    }

    @Test
    fun vectorMultiply() {
        assertEquals("62a38c0486f01e45879d7910a7761bf30d5237e9873f9bff3642a732c4d84f10", (r1 * r1).hex())
        assertEquals("de37805e9986996cfba76ff6ba51c008df851987d9dd323f0e5de07760529c40", (r1 * r2).hex())
        assertEquals("ac8c010096767d3cae5005dec28bb2b45a1d85ab7996ccd3e102a650f74ff100", (r2 * r2).hex())
        assertEquals(r1 * r2, r2 * r1)
        for (r in listOf(r1, r2)) {
            assertEquals(ZERO, r * ZERO)
            assertEquals(r, r * ONE)
            assertEquals(-r, r * MAX_VALUE)
        }
        assertEquals(ZERO, r1 * 0u)
        assertEquals(r1, r1 * 1u)
        assertEquals("7759b1c0ed14047f961ad09b20ff83687876a0181a367b813634046f91def7d4", (r1 * 3u).hex())
        assertEquals("23f7816e30c4ae2017257b7a0fa64d60402f5234d46e746b61c960d09a26d070", (r2 * 0x87654321u).hex())
    }

    @Test
    fun vectorDivide() {
        val d1 = "00000000000000000000000000000000000000000000000ad7133ac1977fa2b7".toUInt256(16)
        val d2 = "0000000000000000000000000000000000000000000000000000000ecd751716".toUInt256(16)
        assertEquals("00000000000000000b8ac01106981635d9ed112290f8895545a7654dde28fb3a", (r1 / d1).hex())
        assertEquals("000000000873ce8efec5b67150bad3aa8c5fcb70e947586153bf2cec7c37c57a", (r1 / d2).hex())
        assertEquals("000000000000000013e1665895a1cc981de6d93670105a6b3ec3b73141b3a3c5", (r2 / d1).hex())
        assertEquals("000000000e8f0abe753bb0afe2e9437ee85d280be60882cf0bd1aaf7fa3cc2c4", (r2 / d2).hex())
        for (r in listOf(r1, r2)) {
            assertEquals(r, r / ONE)
            assertEquals(ZERO, r / MAX_VALUE)
        }
        assertEquals(2u.toUInt256(), MAX_VALUE / r1)
        assertEquals(ONE, MAX_VALUE / r2)
    }

    @Test
    fun vectorOperatorWithSelf() {
        var v = 2u.toUInt256()
        v *= v
        assertEquals(4u.toUInt256(), v)
        v /= v
        assertEquals(ONE, v)
        v += v
        assertEquals(2u.toUInt256(), v)
        v -= v
        assertEquals(ZERO, v)
    }
}
