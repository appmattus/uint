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
import kotlin.test.fail

/**
 * Compares [UInt256] against [BigInteger] reduced modulo 2^256 for randomly generated values, biased towards the
 * edge cases where carries, borrows and normalisation tend to go wrong.
 */
class UInt256BigIntegerFuzzTest {

    private val mask = BigInteger.ONE.shiftLeft(UInt256.SIZE_BITS) - BigInteger.ONE
    private val mask128 = BigInteger.ONE.shiftLeft(UInt128.SIZE_BITS) - BigInteger.ONE
    private val wordMask = BigInteger.ONE.shiftLeft(ULong.SIZE_BITS) - BigInteger.ONE

    private val random = Random(seed = 42)
    private val failures = mutableMapOf<String, String>()

    @Test
    fun arithmetic() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()

        check("plus", a, b, { a + b }, { (ab + bb).toUInt256Wrapped() })
        check("minus", a, b, { a - b }, { (ab - bb).toUInt256Wrapped() })
        check("times", a, b, { a * b }, { (ab * bb).toUInt256Wrapped() })
        check("div", a, b, { a / b }, { (ab / bb).toUInt256Wrapped() })
        check("rem", a, b, { a % b }, { (ab % bb).toUInt256Wrapped() })
        check("unaryMinus", a, null, { -a }, { ab.negate().toUInt256Wrapped() })
        check("inc", a, null, { a.inc() }, { (ab + BigInteger.ONE).toUInt256Wrapped() })
        check("dec", a, null, { a.dec() }, { (ab - BigInteger.ONE).toUInt256Wrapped() })
        check("pow", a, null, { a.pow(b.toInt() and 7) }, { ab.pow(b.toInt() and 7).toUInt256Wrapped() })
        check("isqrt", a, null, { a.isqrt() }, { ab.sqrt().toUInt256Wrapped() })
    }

    @Test
    fun scalars() = fuzz { a, b ->
        val ab = a.toBigInteger()
        // Small scalars hit the single-word paths, and zero checks the division by zero errors
        val l = when (b.toULong() % 4uL) {
            0uL -> b.toULong()
            1uL -> b.toULong() shr 32
            2uL -> b.toULong() and 0xFFu
            else -> 0uL
        }
        val lb = BigInteger(l.toString())
        val ub = BigInteger(l.toUInt().toString())

        check("plus ULong", a, b, { a + l }, { (ab + lb).toUInt256Wrapped() })
        check("minus ULong", a, b, { a - l }, { (ab - lb).toUInt256Wrapped() })
        check("times ULong", a, b, { a * l }, { (ab * lb).toUInt256Wrapped() })
        check("div ULong", a, b, { a / l }, { (ab / lb).toUInt256Wrapped() })
        check("rem ULong", a, b, { a % l }, { (ab % lb).toUInt256Wrapped() })
        check("mod ULong", a, b, { a.mod(l) }, { (ab % lb).toLong().toULong() })
        check("floorDiv ULong", a, b, { a.floorDiv(l) }, { (ab / lb).toUInt256Wrapped() })
        check("compareTo ULong", a, b, { a.compareTo(l).coerceIn(-1, 1) }, { ab.compareTo(lb) })

        check("plus UInt", a, b, { a + l.toUInt() }, { (ab + ub).toUInt256Wrapped() })
        check("minus UInt", a, b, { a - l.toUInt() }, { (ab - ub).toUInt256Wrapped() })
        check("times UInt", a, b, { a * l.toUInt() }, { (ab * ub).toUInt256Wrapped() })
        check("div UInt", a, b, { a / l.toUInt() }, { (ab / ub).toUInt256Wrapped() })
        check("rem UInt", a, b, { a % l.toUInt() }, { (ab % ub).toUInt256Wrapped() })
        check("mod UInt", a, b, { a.mod(l.toUInt()) }, { (ab % ub).toInt().toUInt() })
        check("mod UShort", a, b, { a.mod(l.toUShort()) }, { (ab % BigInteger(l.toUShort().toString())).toInt().toUShort() })
        check("mod UByte", a, b, { a.mod(l.toUByte()) }, { (ab % BigInteger(l.toUByte().toString())).toInt().toUByte() })
        check("compareTo UInt", a, b, { a.compareTo(l.toUInt()).coerceIn(-1, 1) }, { ab.compareTo(ub) })
    }

    @Test
    fun uint128Operands() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val c = b.lower
        val cb = b.toBigInteger().and(mask128)

        check("plus UInt128", a, b, { a + c }, { (ab + cb).toUInt256Wrapped() })
        check("minus UInt128", a, b, { a - c }, { (ab - cb).toUInt256Wrapped() })
        check("times UInt128", a, b, { a * c }, { (ab * cb).toUInt256Wrapped() })
        check("div UInt128", a, b, { a / c }, { (ab / cb).toUInt256Wrapped() })
        check("rem UInt128", a, b, { a % c }, { (ab % cb).toUInt256Wrapped() })
        check("mod UInt128", a, b, { a.mod(c) }, { (ab % cb).toUInt256Wrapped().lower })
        check("compareTo UInt128", a, b, { a.compareTo(c).coerceIn(-1, 1) }, { ab.compareTo(cb) })
        check("toUInt128", a, null, { a.toUInt128() }, { ab.and(mask128).toUInt256Wrapped().lower })
        check("UInt128.toUInt256", a, b, { c.toUInt256() }, { cb.toUInt256Wrapped() })
        check("upper", a, null, { a.upper }, { ab.shiftRight(UInt128.SIZE_BITS).toUInt256Wrapped().lower })
        check("constructor", a, null, { UInt256(a.upper, a.lower) }, { a })
    }

    @Test
    fun comparison() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()

        check("compareTo", a, b, { a.compareTo(b).coerceIn(-1, 1) }, { ab.compareTo(bb) })
        check("equals", a, b, { a == b }, { ab == bb })
        check("hashCode", a, null, { a.hashCode() }, { UInt256(a.upper, a.lower).hashCode() })
    }

    @Test
    fun bits() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()
        // Any Int, as only the eight lowest-order bits are used
        val n = b.toInt()
        val distance = n and (UInt256.SIZE_BITS - 1)

        check("and", a, b, { a and b }, { (ab and bb).toUInt256Wrapped() })
        check("or", a, b, { a or b }, { (ab or bb).toUInt256Wrapped() })
        check("xor", a, b, { a xor b }, { (ab xor bb).toUInt256Wrapped() })
        check("inv", a, null, { a.inv() }, { ab.xor(mask).toUInt256Wrapped() })
        check("shl", a, b, { a shl n }, { ab.shiftLeft(distance).toUInt256Wrapped() })
        check("shr", a, b, { a shr n }, { ab.shiftRight(distance).toUInt256Wrapped() })
        check("rotateLeft", a, b, { a.rotateLeft(n) }, {
            (ab.shiftLeft(distance) or ab.shiftRight(UInt256.SIZE_BITS - distance)).toUInt256Wrapped()
        })
        check("rotateRight", a, b, { a.rotateRight(n) }, {
            (ab.shiftRight(distance) or ab.shiftLeft(UInt256.SIZE_BITS - distance)).toUInt256Wrapped()
        })
        check("countOneBits", a, null, { a.countOneBits() }, { ab.bitCount() })
        check("countLeadingZeroBits", a, null, { a.countLeadingZeroBits() }, { UInt256.SIZE_BITS - ab.bitLength() })
        check("countTrailingZeroBits", a, null, { a.countTrailingZeroBits() }, {
            if (ab.signum() == 0) UInt256.SIZE_BITS else ab.lowestSetBit
        })
        check("takeHighestOneBit", a, null, { a.takeHighestOneBit() }, {
            if (ab.signum() == 0) UInt256.ZERO else BigInteger.ONE.shiftLeft(ab.bitLength() - 1).toUInt256Wrapped()
        })
        check("takeLowestOneBit", a, null, { a.takeLowestOneBit() }, { ab.and(ab.negate()).toUInt256Wrapped() })
    }

    @Test
    fun strings() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val radix = UInt256.MIN_RADIX + b.toInt().mod(UInt256.MAX_RADIX - UInt256.MIN_RADIX + 1)
        val overflow = ab + mask + BigInteger.ONE

        check("toString", a, null, { a.toString() }, { ab.toString() })
        check("toString(radix)", a, b, { a.toString(radix) }, { ab.toString(radix) })
        check("toUInt256(radix)", a, b, { ab.toString(radix).toUInt256(radix) }, { a })
        check("toUInt256(radix) upper case", a, b, { ab.toString(radix).uppercase().toUInt256(radix) }, { a })
        check("toUInt256OrNull(radix) overflow", a, b, { overflow.toString(radix).toUInt256OrNull(radix) }, { null })
    }

    private fun fuzz(block: (UInt256, UInt256) -> Unit) {
        repeat(ITERATIONS) { block(randomUInt256(), randomUInt256()) }
        if (failures.isNotEmpty()) fail(failures.entries.joinToString("\n") { "${it.key}: ${it.value}" })
    }

    private fun randomWord(): ULong = when (random.nextInt(8)) {
        0 -> 0uL
        1 -> 1uL
        2 -> ULong.MAX_VALUE
        3 -> ULong.MAX_VALUE - 1uL
        4 -> 1uL shl 63
        5 -> (1uL shl 63) - 1uL
        6 -> (1uL shl 63) + 1uL
        else -> random.nextULong()
    }

    private fun randomUInt256(): UInt256 = when (random.nextInt(13)) {
        0 -> UInt256.ZERO
        1 -> UInt256(0uL, 0uL, 0uL, random.nextULong())
        2 -> UInt256(random.nextULong(), 0uL, 0uL, 0uL)
        3 -> random.nextULong(1000uL).toUInt256()
        4 -> UInt256.MAX_VALUE - random.nextULong(1000uL)
        5 -> UInt256.ONE shl random.nextInt(UInt256.SIZE_BITS)
        6 -> UInt256(random.nextULong(), random.nextULong(), random.nextULong(), random.nextULong()) shr random.nextInt(UInt256.SIZE_BITS)
        7 -> UInt256(randomWord(), randomWord(), randomWord(), randomWord())
        // Two and three word values, so division runs Algorithm D with every divisor length
        8 -> UInt256(0uL, 0uL, random.nextULong(), random.nextULong())
        9 -> UInt256(0uL, random.nextULong(), random.nextULong(), random.nextULong())
        // Structured words of each length, which reach Algorithm D's rare add back step
        10 -> UInt256(0uL, 0uL, randomWord(), randomWord())
        11 -> UInt256(0uL, randomWord(), randomWord(), randomWord())
        else -> UInt256(random.nextULong(), random.nextULong(), random.nextULong(), random.nextULong())
    }

    /** Records the first mismatch for each operation, treating any exception thrown as part of the result. */
    private fun check(operation: String, a: UInt256, b: UInt256?, actual: () -> Any?, expected: () -> Any?) {
        if (operation in failures) return
        val expectedResult = runCatching(expected).getOrElse { "throws ${it::class.simpleName}" }
        val actualResult = runCatching(actual).getOrElse { "throws ${it::class.simpleName}" }
        if (expectedResult != actualResult) {
            failures[operation] = "a=$a, b=$b, expected=$expectedResult, actual=$actualResult"
        }
    }

    private fun UInt256.toBigInteger(): BigInteger =
        BigInteger(1, ByteArray(UInt256.SIZE_BYTES) { (this shr (UInt256.SIZE_BITS - Byte.SIZE_BITS * (it + 1))).toByte() })

    private fun BigInteger.toUInt256Wrapped(): UInt256 {
        val value = and(mask)
        fun word(index: Int) = value.shiftRight(ULong.SIZE_BITS * index).and(wordMask).toLong().toULong()
        return UInt256(UInt128(word(3), word(2)), UInt128(word(1), word(0)))
    }

    private companion object {
        const val ITERATIONS = 20_000
    }
}
