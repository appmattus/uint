/*
 * Copyright 2022 Appmattus Limited
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

import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class UInt128MathTest {

    @Test
    fun testPow() {
        val two = 2u.toUInt128()
        val four = 4u.toUInt128()

        for (j in 0..127) {
            assertEquals(two.pow(j), UInt128.ONE shl j)
            if (j < 64) assertEquals(four.pow(j), UInt128.ONE shl 2 * j)
        }

        for (i in 5..9) {
            for (j in 0..40) {
                assertEquals(slowPow(i.toUInt128(), j), i.toUInt128().pow(j))
            }
        }
    }

    private fun slowPow(a: UInt128, j: Int): UInt128 {
        var acu = UInt128.ONE
        for (i in 1..j) {
            acu *= a
        }
        return acu
    }

    @Test
    fun testIsqrt() {
        assertEquals("4294967295".toUInt128(), "18446744073709551615".toUInt128().isqrt())
        assertEquals("4294967296".toUInt128(), "18446744073709551616".toUInt128().isqrt())

        for (n in 0..(1 shl 53).toLong() step 997) {
            val lsqrt = floor(sqrt(n.toDouble())).toLong()
            assertEquals(lsqrt.toUInt128(), n.toUInt128().isqrt())
        }

        var num = UInt128.ONE
        var square = num * num
        while (square != UInt128.ZERO) {
            // exact value
            assertEquals(num, square.isqrt())

            // rounds down
            assertEquals(num, (square + 1u).isqrt())

            num = num shl 1
            square = num * num
        }
    }

    /** Checks root * root <= n < (root + 1) * (root + 1), exactly and without overflow as root < 2^64. */
    private fun assertIsqrt(n: UInt128) {
        val root = n.isqrt()
        val next = root + 1u
        val belowNext = root.upper == 0uL && (root.lower == ULong.MAX_VALUE || next * next > n)
        if (root * root > n || !belowNext) fail("isqrt($n) = $root")
    }

    @Test
    fun isqrtAroundPerfectSquares() {
        val random = Random(42)
        repeat(5_000) {
            // Roots of every width, so the squares cover both the 64-bit and the Newton paths
            val root = UInt128(0uL, random.nextULong() shr random.nextInt(64))
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
        repeat(5_000) {
            assertIsqrt(UInt128(random.nextULong(), random.nextULong()) shr random.nextInt(UInt128.SIZE_BITS))
        }
    }

    @Test
    fun isqrtAtTheEdges() {
        assertEquals(UInt128(0uL, ULong.MAX_VALUE), UInt128.MAX_VALUE.isqrt())
        val maxRoot = UInt128(0uL, ULong.MAX_VALUE)
        assertEquals(maxRoot, (maxRoot * maxRoot).isqrt())
        assertEquals(maxRoot - 1u, (maxRoot * maxRoot - 1u).isqrt())
        assertEquals(UInt128.ZERO, UInt128.ZERO.isqrt())
        assertEquals(UInt128.ONE, UInt128.ONE.isqrt())
        assertEquals(UInt128(0uL, 0xFFFFFFFFuL), UInt128(0uL, ULong.MAX_VALUE).isqrt())
        assertEquals(UInt128(0uL, 1uL shl 32), UInt128(1uL, 0uL).isqrt())
        assertEquals(UInt128(0uL, 1uL shl 32), UInt128(1uL, 1uL).isqrt())
        // Where Doubles stop representing every integer
        for (shift in 50..64) {
            for (delta in -3..3) {
                assertIsqrt((UInt128.ONE shl shift) + delta.toLong().toUInt128())
                assertIsqrt((UInt128.ONE shl (2 * shift)) + delta.toLong().toUInt128())
            }
        }
    }
}
