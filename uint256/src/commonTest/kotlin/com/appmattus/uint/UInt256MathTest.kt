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

class UInt256MathTest {

    private val maxRoot = UInt256(UInt128.ZERO, UInt128.MAX_VALUE)

    /** Checks root * root <= n < (root + 1) * (root + 1), exactly and without overflow as root < 2^128. */
    private fun assertIsqrt(n: UInt256) {
        val root = n.isqrt()
        val next = root + 1u
        val belowNext = root <= maxRoot && (root == maxRoot || next * next > n)
        if (root * root > n || !belowNext) fail("isqrt($n) = $root")
    }

    private fun randomUInt256(random: Random): UInt256 =
        UInt256(UInt128(random.nextULong(), random.nextULong()), UInt128(random.nextULong(), random.nextULong())) shr
                random.nextInt(UInt256.SIZE_BITS)

    @Test
    fun isqrtAroundPerfectSquares() {
        val random = Random(42)
        repeat(3_000) {
            // Roots of every width, so the squares cover the UInt128 and the Newton paths
            val root = randomUInt256(random) shr UInt128.SIZE_BITS
            val square = root * root
            assertEquals(root, square.isqrt())
            assertIsqrt(square - 1u)
            assertIsqrt(square + 1u)
            assertIsqrt(square + root + root) // (root + 1)^2 - 1
        }
    }

    @Test
    fun isqrtOfRandomValues() {
        val random = Random(7)
        repeat(3_000) { assertIsqrt(randomUInt256(random)) }
    }

    @Test
    fun isqrtAtTheEdges() {
        assertEquals(maxRoot, UInt256.MAX_VALUE.isqrt())
        assertEquals(maxRoot, (maxRoot * maxRoot).isqrt())
        assertEquals(maxRoot - 1u, (maxRoot * maxRoot - 1u).isqrt())
        assertEquals(UInt256.ZERO, UInt256.ZERO.isqrt())
        assertEquals(UInt256.ONE, UInt256.ONE.isqrt())
        assertEquals(UInt256.ONE shl 64, (UInt256.ONE shl 128).isqrt())
        assertEquals(UInt256.ONE shl 64, ((UInt256.ONE shl 128) + 1u).isqrt())
        for (shift in 100..127) {
            for (delta in -3..3) {
                assertIsqrt((UInt256.ONE shl (2 * shift)) + delta.toLong().toUInt256())
            }
        }
    }

    @Test
    fun isqrtOfSmallValues() {
        val cases = listOf(
            2L to 1L, 3L to 1L, 4L to 2L, 144L to 12L,
            999999L to 999L, 1000000L to 1000L, 1000001L to 1000L, 1002000L to 1000L, 1002001L to 1001L,
        )
        for ((n, root) in cases) assertEquals(root.toUInt256(), n.toUInt256().isqrt(), "sqrt($n)")
    }

    @Test
    fun pow() {
        val two = 2u.toUInt256()
        assertEquals(UInt256.ONE, two.pow(0))
        assertEquals(UInt256.ONE shl 255, two.pow(255))
        assertEquals(UInt256.ZERO, two.pow(256))
        assertEquals(UInt256.ONE, UInt256.MAX_VALUE.pow(2))
        assertEquals(
            "1000000000000000000000000000000000000000000000000000000000000000000000000000".toUInt256(),
            10u.toUInt256().pow(75)
        )
        assertFailsWith<IllegalArgumentException> { two.pow(-1) }
    }
}
