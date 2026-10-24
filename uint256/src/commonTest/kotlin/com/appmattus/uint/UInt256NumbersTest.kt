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

class UInt256NumbersTest {

    @Test
    fun countBits() {
        assertEquals(0, ZERO.countOneBits())
        assertEquals(256, MAX_VALUE.countOneBits())
        assertEquals(256, ZERO.countTrailingZeroBits())
        assertEquals(0, MAX_VALUE.countTrailingZeroBits())

        assertEquals(256, ZERO.countLeadingZeroBits())
        assertEquals(255, 1.toUInt256().countLeadingZeroBits())
        assertEquals(248, 255.toUInt256().countLeadingZeroBits())
        assertEquals(247, 256.toUInt256().countLeadingZeroBits())
        assertEquals(240, 0xff00.toUInt256().countLeadingZeroBits())
        assertEquals(239, 0x10000.toUInt256().countLeadingZeroBits())
        assertEquals(231, 0x1000000.toUInt256().countLeadingZeroBits())
        assertEquals(0, MAX_VALUE.countLeadingZeroBits())

        for (n in 0 until 256) {
            val bit = ONE shl n
            assertEquals(1, bit.countOneBits())
            assertEquals(255 - n, bit.countLeadingZeroBits())
            assertEquals(n, bit.countTrailingZeroBits())
            assertEquals(bit, (bit or ONE).takeHighestOneBit())
            assertEquals(bit, (bit or (ONE shl 255)).takeLowestOneBit())
        }
        assertEquals(ZERO, ZERO.takeHighestOneBit())
        assertEquals(ZERO, ZERO.takeLowestOneBit())
    }

    @Test
    fun rotate() {
        val value = (ONE shl 255) or ONE
        assertEquals(ONE shl 1 or ONE, value.rotateLeft(1))
        assertEquals((ONE shl 255) or (ONE shl 254), value.rotateRight(1))
        assertEquals(value, value.rotateLeft(256))
        assertEquals(value.rotateRight(3), value.rotateLeft(-3))
        for (n in 0..300 step 7) assertEquals(value, value.rotateLeft(n).rotateRight(n))
    }
}
