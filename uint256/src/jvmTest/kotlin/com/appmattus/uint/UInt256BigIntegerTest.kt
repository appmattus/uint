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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class UInt256BigIntegerTest {

    private val max = BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935")

    private val modulus = BigInteger.ONE.shiftLeft(UInt256.SIZE_BITS)
    private val random = Random(42)

    private val r1 = "7d1de5eaf9b156d53208f033b5aa8122d2d2355d5e12292b121156cfdb4a529b".toUInt256(16)
    private val r2 = "d781cab4f072134971da2d19a3473013bfb69ca6c30a7e26406ba5477c1d3271".toUInt256(16)

    @Test
    fun roundTripsThroughBigInteger() {
        assertEquals(BigInteger.TWO.pow(256) - BigInteger.ONE, max)
        assertEquals(UInt256.MAX_VALUE, max.toString().toUInt256())
        assertEquals(max, BigInteger(UInt256.MAX_VALUE.toString()))
        assertEquals(max.toString(16), UInt256.MAX_VALUE.toString(16))
    }

    @Test
    fun highAndLowMatchBigIntegerShifts() {
        val value = BigInteger("210439592369239639725932969795495939596", 16)
        val uint256 = value.toString().toUInt256()
        assertEquals(value.shiftRight(128), BigInteger(uint256.upper.toString()))
        assertEquals(value.and(BigInteger.TWO.pow(128) - BigInteger.ONE), BigInteger(uint256.lower.toString()))
    }

    @Test
    fun randomBitLengthOperands() {
        repeat(10_000) { checkArithmetic(randomBitLength(), randomBitLength()) }
    }

    @Test
    fun structuredOperandsInEveryOrder() {
        for (i in 0 until UInt256.SIZE_BITS) {
            val power = UInt256.ONE shl i
            val structured =
                listOfNotNull(power, power xor UInt256.MAX_VALUE, power + 1u, if (i == 0) null else power - 1u)
            for (p in structured) {
                val r2 = randomNonZero()
                val r3 = randomNonZero()
                for ((a, b) in listOf(p to r2, r2 to p, p to r3, r3 to p, r2 to r3)) checkArithmetic(a, b)
                assertBinary(
                    "times then rem",
                    p,
                    r2,
                    (p * r2) % r3,
                    (p.toBigInteger() * r2.toBigInteger()).mod(modulus) % r3.toBigInteger()
                )
            }
        }
    }

    @Test
    fun addAndSubtractULong() {
        val cases = listOf(
            "0" to 1uL, "1" to 0uL, "1" to 1uL, "1" to 3uL,
            "10000000000000000" to 1uL, "100000000000000000000000000000000" to 1uL,
            "0" to 0xffffffffffffffffuL, "1" to 0xffffffffuL,
            "ffffffffffffffff" to 1uL, "ffffffffffffffff" to 0xffffffffffffffffuL,
            "fffffffffffffffffffffffffffffffff" to 1uL, "fffffffffffffffffffffffffffffffff" to 2uL,
        )
        for ((arg, n) in cases) {
            val a = arg.toUInt256(16)
            val nb = BigInteger(n.toString())
            assertBinary("minus ULong", a, n.toUInt256(), a - n, a.toBigInteger() - nb)
            assertBinary("plus ULong", a, n.toUInt256(), a + n, a.toBigInteger() + nb)
        }
    }

    @Test
    fun compareAroundPowersOfTwo() {
        for (i in 0 until 255) {
            val z = UInt256.ONE shl i
            val zb = z.toBigInteger()
            for (x in listOf(z.toULong(), (z + 1u).toULong(), (z - 1u).toULong(), (z shr 64).toULong())) {
                assertEquals(zb.compareTo(BigInteger(x.toString())), z.compareTo(x).coerceIn(-1, 1), "2^$i vs $x")
            }
            for (y in listOf(z, z + 1u, z - 1u)) {
                assertEquals(zb.compareTo(y.toBigInteger()), z.compareTo(y).coerceIn(-1, 1), "2^$i vs $y")
            }
        }
    }

    // Only the eight lowest-order bits of the distance are used, so the shifts by 256 and 300 are by 0 and 44
    @Test
    fun shiftVectors() {
        val values = listOf(
            "ffffeeeeddddccccbbbbaaaa9999888877776666555544443333222211110000",
            "8000000000000000000000000000000000000000000000000000000000000000",
            "7fffeeeeddddccccbbbbaaaa9999888877776666555544443333222211110000",
        ).map { it.toUInt256(16) }
        for (value in values) {
            for (n in listOf(0, 16, 64, 96, 127, 128, 129, 192, 254, 255, 256, 300)) {
                val distance = n and (UInt256.SIZE_BITS - 1)
                assertBinary("shr $n", value, n.toUInt256(), value shr n, value.toBigInteger().shiftRight(distance))
                assertBinary("shl $n", value, n.toUInt256(), value shl n, value.toBigInteger().shiftLeft(distance))
            }
        }
    }

    // The low 256 bits of the full 512-bit sums and products
    @Test
    fun add512AndMul512LowHalves() {
        val max = UInt256.MAX_VALUE
        val values = listOf(UInt256.ZERO, UInt256.ONE, 17u.toUInt256(), 42u.toUInt256(), max - 1u, max)
        for (a in values) {
            for (b in values) {
                assertEquals((a.toBigInteger() + b.toBigInteger()).toUInt256Wrapped(), a + b, "$a + $b")
                assertEquals((a.toBigInteger() * b.toBigInteger()).toUInt256Wrapped(), a * b, "$a * $b")
            }
        }
    }

    @Test
    fun bitwiseOperators() {
        val operands = listOf(r1, r2, UInt256.ZERO, UInt256.MAX_VALUE, r1.inv(), r2.inv())
        for (a in operands) {
            for (b in operands) {
                assertEquals(a.toBigInteger().or(b.toBigInteger()).toUInt256Wrapped(), a or b)
                assertEquals(a.toBigInteger().xor(b.toBigInteger()).toUInt256Wrapped(), a xor b)
                assertEquals(a.toBigInteger().and(b.toBigInteger()).toUInt256Wrapped(), a and b)
            }
        }
        val tmp64 = 0xe1db685c9a0b47a2uL
        assertEquals(r1.toBigInteger().or(BigInteger(tmp64.toString())).toUInt256Wrapped(), r1 or tmp64.toUInt256())
        assertEquals(r1.toBigInteger().xor(BigInteger(tmp64.toString())).toUInt256Wrapped(), r1 xor tmp64.toUInt256())
    }

    @Test
    fun shifts() {
        for (value in listOf(UInt256.ONE, r1, UInt256.MAX_VALUE)) {
            for (i in 0 until 256) {
                assertEquals(value.toBigInteger().shiftLeft(i).toUInt256Wrapped(), value shl i, "$value shl $i")
                assertEquals(value.toBigInteger().shiftRight(i).toUInt256Wrapped(), value shr i, "$value shr $i")
            }
        }
    }

    private fun UInt256.toBigInteger(): BigInteger = BigInteger(toString())

    private fun BigInteger.toUInt256Wrapped(): UInt256 = mod(modulus).toString().toUInt256()

    private fun randomBitLength(): UInt256 {
        val bits = random.nextInt(UInt256.SIZE_BITS + 1)
        return BigInteger(bits, java.util.Random(random.nextLong())).toUInt256Wrapped()
    }

    private fun randomNonZero(): UInt256 {
        while (true) randomBitLength().let { if (it != UInt256.ZERO) return it }
    }

    private fun assertBinary(name: String, a: UInt256, b: UInt256, actual: UInt256, expected: BigInteger) {
        val want = expected.toUInt256Wrapped()
        if (actual != want) fail(
            "$name: a=${a.toString(16)} b=${b.toString(16)} want=${want.toString(16)} have=${
                actual.toString(
                    16
                )
            }"
        )
    }

    private fun checkArithmetic(a: UInt256, b: UInt256) {
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()
        assertBinary("plus", a, b, a + b, ab + bb)
        assertBinary("minus", a, b, a - b, ab - bb)
        assertBinary("times", a, b, a * b, ab * bb)
        if (b != UInt256.ZERO) {
            assertBinary("div", a, b, a / b, ab / bb)
            assertBinary("rem", a, b, a % b, ab % bb)
        }
    }
}
