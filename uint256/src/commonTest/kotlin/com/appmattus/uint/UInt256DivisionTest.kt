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

class UInt256DivisionTest {

    private val random = Random(42)

    /** Checks q * b + r == a and r < b, which together pin down the quotient and remainder exactly. */
    private fun assertDivision(a: UInt256, b: UInt256) {
        val q = a / b
        val r = a % b
        if (r >= b || q * b + r != a) fail("${a.toString(16)} / ${b.toString(16)} = ${q.toString(16)} remainder ${r.toString(16)}")
        assertEquals(q, a.floorDiv(b))
        assertEquals(r, a.mod(b))
    }

    // Found by instrumenting Algorithm D: each of these needs its rare add back step, where the corrected quotient
    // estimate is still one too large
    private val addBackCases = listOf(
        "ffffffffffffffffffffffffffffffff7ffffffffffffffffffffffffffffffe" to "1ffffffffffffffffffffffffffffffff2d48ce1a10fba8eb",
        "ffffffffffffffff00000000000000016da5db6a0646a11822980eae67e7d16c" to "1000000000000000000000000000000018000000000000001",
        "7fffffffffffffff00000000000000006bdb87dc0dcb313cffffffffffffffff" to "800000000000000000000000000000007dc9951a84d0b2ab",
        "7fffffffffffffff00000000000000008000000000000001e5ead8e18869988e" to "fffffffffffffffe0000000000000001fffffffffffffffe",
        "80000000000000007fffffffffffffff0000000000000000ced9c258539bee08" to "ffffffffffffffff0000000000000000533b3eec16f8f874",
        "39ebd277b0c11fbfffffffffffffffffc9d6ed11c374cb14f79650203dff2df6" to "ffffffffffffffffffffffffffffffff7fffffffffffffff",
    )

    @Test
    fun addBackCases() {
        addBackCases.forEach { (a, b) -> assertDivision(a.toUInt256(16), b.toUInt256(16)) }
    }

    private val words = listOf(0uL, 1uL, ULong.MAX_VALUE, ULong.MAX_VALUE - 1uL, 1uL shl 63, (1uL shl 63) - 1uL)

    private fun word(): ULong = if (random.nextInt(3) == 0) random.nextULong() else words[random.nextInt(words.size)]

    private fun value(length: Int): UInt256 = UInt256(
        UInt128(if (length > 3) word() else 0uL, if (length > 2) word() else 0uL),
        UInt128(if (length > 1) word() else 0uL, word())
    )

    @Test
    fun everyDividendAndDivisorLength() {
        for (dividendLength in 1..4) {
            for (divisorLength in 1..dividendLength) {
                repeat(500) {
                    val b = value(divisorLength).takeIf { it != UInt256.ZERO } ?: UInt256.ONE
                    assertDivision(value(dividendLength), b)
                }
            }
        }
    }

    @Test
    fun dividendsEitherSideOfAMultiple() {
        repeat(2_000) {
            val b = value(1 + random.nextInt(4)).takeIf { it != UInt256.ZERO } ?: UInt256.ONE
            val multiple = b * value(1 + random.nextInt(4))
            assertDivision(multiple, b)
            assertDivision(multiple - 1u, b)
            assertDivision(multiple + 1u, b)
        }
    }

    @Test
    fun smallValues() {
        assertEquals(UInt256.ONE, 5678.toUInt256() / 5678.toUInt256())
        assertEquals(UInt256.ZERO, UInt256.ZERO / 5678.toUInt256())
        assertEquals(UInt256.ONE, 7000.toUInt256() / 5678.toUInt256())
        assertEquals(284.toUInt256(), 284.toUInt256() % 5678.toUInt256())
        assertEquals(UInt256.ZERO, 5678.toUInt256() % 5678.toUInt256())
        assertEquals((7000 - 5678).toUInt256(), 7000.toUInt256() % 5678.toUInt256())
        assertEquals(UInt256.ZERO, 17034.toUInt256() % 5678.toUInt256()) // 17034 == 5678 * 3
    }

    @Test
    fun scalarDivisionMatchesWidened() {
        repeat(2_000) {
            val a = value(1 + random.nextInt(4))
            val d = word().coerceAtLeast(1uL)
            assertEquals(a / d.toUInt256(), a / d)
            assertEquals(a % d.toUInt256(), a % d)
            assertEquals((a % d.toUInt256()).toULong(), a.mod(d))
            assertEquals(a / d.toUInt256(), a / d.toUInt128())
        }
    }
}
