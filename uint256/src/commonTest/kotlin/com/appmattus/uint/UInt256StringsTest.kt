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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UInt256StringsTest {

    private val maxDecimal = "115792089237316195423570985008687907853269984665640564039457584007913129639935"

    @Test
    fun maxValue() {
        assertEquals(maxDecimal, UInt256.MAX_VALUE.toString())
        assertEquals("f".repeat(64), UInt256.MAX_VALUE.toString(16))
        assertEquals("1".repeat(256), UInt256.MAX_VALUE.toString(2))
        assertEquals(UInt256.MAX_VALUE, maxDecimal.toUInt256())
    }

    @Test
    fun maxValueRoundTripsInEveryRadix() {
        for (radix in UInt256.MIN_RADIX..UInt256.MAX_RADIX) {
            val max = UInt256.MAX_VALUE.toString(radix)
            assertEquals(UInt256.MAX_VALUE, max.toUInt256(radix), "radix $radix")
            // One more digit always overflows
            assertNull((max + "0").toUInt256OrNull(radix), "radix $radix")
        }
    }

    @Test
    fun valuesOfEveryWidthRoundTrip() {
        var value = UInt256.ONE
        repeat(UInt256.SIZE_BITS) {
            for (radix in listOf(2, 7, 10, 16, 36)) {
                assertEquals(value, value.toString(radix).toUInt256(radix))
                assertEquals(value - 1u, (value - 1u).toString(radix).toUInt256(radix))
            }
            value = value shl 1
        }
    }

    @Test
    fun rejectsMaxValuePlusOne() {
        assertNull("115792089237316195423570985008687907853269984665640564039457584007913129639936".toUInt256OrNull())
        assertNull(("1" + "0".repeat(64)).toUInt256OrNull(16))
    }

    @Test
    fun smallValues() {
        assertEquals("0", UInt256.ZERO.toString())
        assertEquals("12345", 12345u.toUInt256().toString())
        assertEquals(255u.toUInt256(), "ff".toUInt256(16))
        assertEquals(255u.toUInt256(), "FF".toUInt256(16))
        assertEquals(42u.toUInt256(), "+42".toUInt256())
    }

    @Test
    fun hexAcrossBothHalves() {
        val hex = "210439592369239639725932969795495939596"
        val value = hex.toUInt256(16)
        assertEquals(UInt256("2104395".toUInt128(16), "92369239639725932969795495939596".toUInt128(16)), value)
        assertEquals(hex, value.toString(16))
    }

    @Test
    fun leadingZerosSpanningSeveralChunks() {
        assertEquals(UInt256.MAX_VALUE, ("0".repeat(100) + maxDecimal).toUInt256())
        assertEquals(UInt256.ZERO, "0".repeat(300).toUInt256(2))
    }

    @Test
    fun invalidStrings() {
        assertNull("".toUInt256OrNull())
        assertNull("+".toUInt256OrNull())
        assertNull("-1".toUInt256OrNull())
        assertNull("12a".toUInt256OrNull())
        assertNull("2".toUInt256OrNull(2))
        // There's no 0x prefix, matching String.toULong(16)
        assertNull("0x".toUInt256OrNull(16))
        assertFailsWith<NumberFormatException> { "x".toUInt256() }
    }

    @Test
    fun invalidRadix() {
        assertFailsWith<IllegalArgumentException> { UInt256.ONE.toString(1) }
        assertFailsWith<IllegalArgumentException> { UInt256.ONE.toString(37) }
        assertFailsWith<IllegalArgumentException> { "1".toUInt256(37) }
    }
}
