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
 * Compares [UInt128] against [BigInteger] reduced modulo 2^128 for randomly generated values, biased towards the
 * edge cases where carries, borrows and normalisation tend to go wrong.
 */
class UInt128BigIntegerFuzzTest {

    private val mask = BigInteger.ONE.shiftLeft(UInt128.SIZE_BITS) - BigInteger.ONE
    private val wordMask = BigInteger.ONE.shiftLeft(ULong.SIZE_BITS) - BigInteger.ONE

    private val random = Random(seed = 42)
    private val failures = mutableMapOf<String, String>()

    @Test
    fun arithmetic() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()

        check("plus", a, b, { a + b }, { (ab + bb).toUInt128Wrapped() })
        check("minus", a, b, { a - b }, { (ab - bb).toUInt128Wrapped() })
        check("times", a, b, { a * b }, { (ab * bb).toUInt128Wrapped() })
        check("div", a, b, { a / b }, { (ab / bb).toUInt128Wrapped() })
        check("rem", a, b, { a % b }, { (ab % bb).toUInt128Wrapped() })
        check("unaryMinus", a, null, { -a }, { ab.negate().toUInt128Wrapped() })
        check("inc", a, null, { a.inc() }, { (ab + BigInteger.ONE).toUInt128Wrapped() })
        check("dec", a, null, { a.dec() }, { (ab - BigInteger.ONE).toUInt128Wrapped() })
        check("pow", a, null, { a.pow(b.toInt() and 7) }, { ab.pow(b.toInt() and 7).toUInt128Wrapped() })
        check("isqrt", a, null, { a.isqrt() }, { ab.sqrt().toUInt128Wrapped() })
    }

    @Test
    fun comparison() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()

        check("compareTo", a, b, { a.compareTo(b).coerceIn(-1, 1) }, { ab.compareTo(bb) })
        check("equals", a, b, { a == b }, { ab == bb })
        check("hashCode", a, null, { a.hashCode() }, { UInt128(a.upper, a.lower).hashCode() })
    }

    @Test
    fun bits() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val bb = b.toBigInteger()
        // Any Int, as only the seven lowest-order bits are used
        val n = b.toInt()
        val distance = n and (UInt128.SIZE_BITS - 1)

        check("and", a, b, { a and b }, { (ab and bb).toUInt128Wrapped() })
        check("or", a, b, { a or b }, { (ab or bb).toUInt128Wrapped() })
        check("xor", a, b, { a xor b }, { (ab xor bb).toUInt128Wrapped() })
        check("inv", a, null, { a.inv() }, { ab.xor(mask).toUInt128Wrapped() })
        check("shl", a, b, { a shl n }, { ab.shiftLeft(distance).toUInt128Wrapped() })
        check("shr", a, b, { a shr n }, { ab.shiftRight(distance).toUInt128Wrapped() })
        check("rotateLeft", a, b, { a.rotateLeft(n) }, {
            (ab.shiftLeft(distance) or ab.shiftRight(UInt128.SIZE_BITS - distance)).toUInt128Wrapped()
        })
        check("rotateRight", a, b, { a.rotateRight(n) }, {
            (ab.shiftRight(distance) or ab.shiftLeft(UInt128.SIZE_BITS - distance)).toUInt128Wrapped()
        })
        check("countOneBits", a, null, { a.countOneBits() }, { ab.bitCount() })
        check("countLeadingZeroBits", a, null, { a.countLeadingZeroBits() }, { UInt128.SIZE_BITS - ab.bitLength() })
        check("countTrailingZeroBits", a, null, { a.countTrailingZeroBits() }, {
            if (ab.signum() == 0) UInt128.SIZE_BITS else ab.lowestSetBit
        })
    }

    @Test
    fun strings() = fuzz { a, b ->
        val ab = a.toBigInteger()
        val radix = UInt128.MIN_RADIX + b.toInt().mod(UInt128.MAX_RADIX - UInt128.MIN_RADIX + 1)
        val overflow = ab + mask + BigInteger.ONE

        check("toString", a, null, { a.toString() }, { ab.toString() })
        check("toString(radix)", a, b, { a.toString(radix) }, { ab.toString(radix) })
        check("toUInt128(radix)", a, b, { ab.toString(radix).toUInt128(radix) }, { a })
        check("toUInt128(radix) upper case", a, b, { ab.toString(radix).uppercase().toUInt128(radix) }, { a })
        check("toUInt128OrNull(radix) overflow", a, b, { overflow.toString(radix).toUInt128OrNull(radix) }, { null })
    }

    private fun fuzz(block: (UInt128, UInt128) -> Unit) {
        repeat(ITERATIONS) { block(randomUInt128(), randomUInt128()) }
        if (failures.isNotEmpty()) fail(failures.entries.joinToString("\n") { "${it.key}: ${it.value}" })
    }

    private fun randomUInt128(): UInt128 = when (random.nextInt(8)) {
        0 -> UInt128.ZERO
        1 -> UInt128(0u, random.nextULong())
        2 -> UInt128(random.nextULong(), 0u)
        3 -> UInt128(0u, random.nextULong(1000uL))
        4 -> UInt128.MAX_VALUE - random.nextULong(1000uL)
        5 -> UInt128.ONE shl random.nextInt(UInt128.SIZE_BITS)
        6 -> UInt128(random.nextULong(), random.nextULong()) shr random.nextInt(UInt128.SIZE_BITS)
        else -> UInt128(random.nextULong(), random.nextULong())
    }

    /** Records the first mismatch for each operation, treating any exception thrown as part of the result. */
    private fun check(operation: String, a: UInt128, b: UInt128?, actual: () -> Any?, expected: () -> Any?) {
        if (operation in failures) return
        val expectedResult = runCatching(expected).getOrElse { "throws ${it::class.simpleName}" }
        val actualResult = runCatching(actual).getOrElse { "throws ${it::class.simpleName}" }
        if (expectedResult != actualResult) {
            failures[operation] = "a=$a, b=$b, expected=$expectedResult, actual=$actualResult"
        }
    }

    private fun UInt128.toBigInteger(): BigInteger =
        BigInteger(1, ByteArray(UInt128.SIZE_BYTES) { (this shr (UInt128.SIZE_BITS - Byte.SIZE_BITS * (it + 1))).toByte() })

    private fun BigInteger.toUInt128Wrapped(): UInt128 {
        val value = and(mask)
        return UInt128(value.shiftRight(ULong.SIZE_BITS).toLong().toULong(), value.and(wordMask).toLong().toULong())
    }

    private companion object {
        const val ITERATIONS = 20_000
    }
}
