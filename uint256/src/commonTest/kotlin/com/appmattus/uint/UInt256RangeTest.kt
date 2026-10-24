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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UInt256RangeTest {

    private val twoPow128 = ONE shl 128

    @Test
    fun rangeTo() {
        val all = ZERO..MAX_VALUE
        assertTrue(ZERO in all)
        assertTrue(twoPow128 in all)
        assertTrue(MAX_VALUE in all)

        val boundary = (twoPow128 - 1u)..twoPow128
        assertFalse((twoPow128 - 2u) in boundary)
        assertTrue((twoPow128 - 1u) in boundary)
        assertTrue(twoPow128 in boundary)
        assertFalse((twoPow128 + 1u) in boundary)
    }

    @Test
    fun until() {
        val range = ZERO until MAX_VALUE
        assertTrue((MAX_VALUE - 1u) in range)
        assertFalse(MAX_VALUE in range)
        assertTrue((ZERO until ZERO).isEmpty())
        assertEquals(UInt256Range.EMPTY, MAX_VALUE until ZERO)
    }

    @Test
    fun positiveStep() {
        val ten = 10u.toUInt256()
        assertEquals(listOf(0, 3, 6, 9).map { it.toUInt256() }, (ZERO..ten step 3).toList())
        assertEquals(listOf(MAX_VALUE - 2u, MAX_VALUE - 1u, MAX_VALUE), (MAX_VALUE - 2u..MAX_VALUE).toList())
    }

    @Test
    fun negativeStep() {
        val ten = 10u.toUInt256()
        assertEquals((10 downTo 0).map { it.toUInt256() }, UInt256Progression.fromClosedRange(ten, ZERO, -1).toList())
        assertEquals(listOf(10, 7, 4, 1).map { it.toUInt256() }, UInt256Progression.fromClosedRange(ten, ZERO, -3).toList())
        assertEquals(
            listOf(twoPow128 + 1u, twoPow128, twoPow128 - 1u),
            UInt256Progression.fromClosedRange(twoPow128 + 1u, twoPow128 - 1u, -1).toList()
        )
    }

    @Test
    fun equalityAndToString() {
        assertEquals(ZERO..ONE, ZERO..ONE)
        assertEquals((ONE..ZERO), UInt256Range.EMPTY)
        assertEquals("0..1", (ZERO..ONE).toString())
        assertEquals("10 downTo 0 step 2", UInt256Progression.fromClosedRange(10u.toUInt256(), ZERO, -2).toString())
    }
}
