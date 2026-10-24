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

import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

class UInt128JvmIntrinsicsTest {

    private val random = Random(42)

    private fun expectedHigh(x: ULong, y: ULong): ULong =
        BigInteger(x.toString()).multiply(BigInteger(y.toString())).shiftRight(ULong.SIZE_BITS).toString().toULong()

    // Covers the signed-to-unsigned correction for each combination of top bits
    private val edges = listOf(0uL, 1uL, Long.MAX_VALUE.toULong(), 1uL shl 63, ULong.MAX_VALUE)

    @Test
    fun unsignedMultiplyHighMatchesBigIntegerForEdgeValues() {
        edges.forEach { x ->
            edges.forEach { y ->
                assertEquals(expectedHigh(x, y), uint64MultiplyHigh(x, y), "$x * $y")
            }
        }
    }

    @Test
    fun unsignedMultiplyHighMatchesBigIntegerForRandomValues() {
        repeat(10_000) {
            val x = random.nextULong()
            val y = random.nextULong()
            assertEquals(expectedHigh(x, y), uint64MultiplyHigh(x, y), "$x * $y")
        }
    }
}
