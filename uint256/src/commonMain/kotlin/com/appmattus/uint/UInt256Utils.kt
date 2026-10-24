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

import kotlin.math.sqrt

private const val HALF_MASK: ULong = 0xFFFFFFFFu
private const val HALF_BITS: Int = UInt.SIZE_BITS
private const val WORD_BITS: Int = ULong.SIZE_BITS
private const val WORD_COUNT: Int = UInt256.SIZE_BITS / WORD_BITS

/** Returns the upper 64 bits of the 128-bit product of [x] and [y], using a platform intrinsic where available. */
internal expect fun u256MultiplyHigh(x: ULong, y: ULong): ULong

/** Returns [dividend] / [divisor], working around slow unsigned division on some platforms. */
internal expect fun u256Divide(dividend: ULong, divisor: ULong): ULong

/** Returns [dividend] % [divisor], working around slow unsigned division on some platforms. */
internal expect fun u256Remainder(dividend: ULong, divisor: ULong): ULong

/** Returns the upper 64 bits of the 128-bit product of [x] and [y]. */
internal fun u256MultiplyHighPortable(x: ULong, y: ULong): ULong {
    val x0 = x and HALF_MASK
    val x1 = x shr HALF_BITS
    val y0 = y and HALF_MASK
    val y1 = y shr HALF_BITS

    val p00 = x0 * y0
    val p01 = x0 * y1
    val p10 = x1 * y0
    val p11 = x1 * y1

    val middle = (p00 shr HALF_BITS) + (p01 and HALF_MASK) + (p10 and HALF_MASK)
    return p11 + (p01 shr HALF_BITS) + (p10 shr HALF_BITS) + (middle shr HALF_BITS)
}

/** Returns word [index] of [v], counting from the least significant, or zero outside the value. */
private fun word(v: UInt256, index: Int): ULong = when (index) {
    0 -> v.w0
    1 -> v.w1
    2 -> v.w2
    3 -> v.w3
    else -> 0uL
}

internal fun uint256Compare(v1: UInt256, v2: UInt256): Int = when {
    v1.w3 != v2.w3 -> v1.w3.compareTo(v2.w3)
    v1.w2 != v2.w2 -> v1.w2.compareTo(v2.w2)
    v1.w1 != v2.w1 -> v1.w1.compareTo(v2.w1)
    else -> v1.w0.compareTo(v2.w0)
}

internal fun uint256Plus(v1: UInt256, v2: UInt256): UInt256 {
    val r0 = v1.w0 + v2.w0
    var carry = if (r0 < v1.w0) 1uL else 0uL
    val s1 = v1.w1 + v2.w1
    val r1 = s1 + carry
    carry = (if (s1 < v1.w1) 1uL else 0uL) + (if (r1 < s1) 1uL else 0uL)
    val s2 = v1.w2 + v2.w2
    val r2 = s2 + carry
    carry = (if (s2 < v1.w2) 1uL else 0uL) + (if (r2 < s2) 1uL else 0uL)
    return UInt256(v1.w3 + v2.w3 + carry, r2, r1, r0)
}

internal fun uint256Minus(v1: UInt256, v2: UInt256): UInt256 {
    val r0 = v1.w0 - v2.w0
    var borrow = if (v1.w0 < v2.w0) 1uL else 0uL
    val d1 = v1.w1 - v2.w1
    val r1 = d1 - borrow
    borrow = (if (v1.w1 < v2.w1) 1uL else 0uL) + (if (d1 < borrow) 1uL else 0uL)
    val d2 = v1.w2 - v2.w2
    val r2 = d2 - borrow
    borrow = (if (v1.w2 < v2.w2) 1uL else 0uL) + (if (d2 < borrow) 1uL else 0uL)
    return UInt256(v1.w3 - v2.w3 - borrow, r2, r1, r0)
}

internal fun uint256Times(v1: UInt256, v2: UInt256): UInt256 {
    val a0 = v1.w0
    val a1 = v1.w1
    val a2 = v1.w2
    val a3 = v1.w3
    val b0 = v2.w0
    val b1 = v2.w1
    val b2 = v2.w2
    val b3 = v2.w3

    // Schoolbook multiplication on 64-bit words, keeping only the low 256 bits. Products that only reach the top
    // word need just their low half, and carries out of the top word are discarded.
    var r0 = 0uL
    var r1 = 0uL
    var r2 = 0uL
    var r3 = 0uL
    var carry: ULong
    var lo: ULong
    var sum: ULong
    var c1: ULong
    carry = 0uL
    lo = a0 * b0
    sum = lo + r0
    c1 = if (sum < lo) 1uL else 0uL
    r0 = sum + carry
    carry = u256MultiplyHigh(a0, b0) + c1 + (if (r0 < sum) 1uL else 0uL)
    lo = a0 * b1
    sum = lo + r1
    c1 = if (sum < lo) 1uL else 0uL
    r1 = sum + carry
    carry = u256MultiplyHigh(a0, b1) + c1 + (if (r1 < sum) 1uL else 0uL)
    lo = a0 * b2
    sum = lo + r2
    c1 = if (sum < lo) 1uL else 0uL
    r2 = sum + carry
    carry = u256MultiplyHigh(a0, b2) + c1 + (if (r2 < sum) 1uL else 0uL)
    r3 += a0 * b3 + carry
    carry = 0uL
    lo = a1 * b0
    sum = lo + r1
    c1 = if (sum < lo) 1uL else 0uL
    r1 = sum + carry
    carry = u256MultiplyHigh(a1, b0) + c1 + (if (r1 < sum) 1uL else 0uL)
    lo = a1 * b1
    sum = lo + r2
    c1 = if (sum < lo) 1uL else 0uL
    r2 = sum + carry
    carry = u256MultiplyHigh(a1, b1) + c1 + (if (r2 < sum) 1uL else 0uL)
    r3 += a1 * b2 + carry
    carry = 0uL
    lo = a2 * b0
    sum = lo + r2
    c1 = if (sum < lo) 1uL else 0uL
    r2 = sum + carry
    carry = u256MultiplyHigh(a2, b0) + c1 + (if (r2 < sum) 1uL else 0uL)
    r3 += a2 * b1 + carry
    carry = 0uL
    r3 += a3 * b0 + carry

    return UInt256(r3, r2, r1, r0)
}

internal fun uint256Div(dividend: UInt256, divisor: UInt256): UInt256 = uint256DivRem(dividend, divisor, remainder = false)

internal fun uint256Rem(dividend: UInt256, divisor: UInt256): UInt256 = uint256DivRem(dividend, divisor, remainder = true)

@Suppress("ReturnCount")
private fun uint256DivRem(dividend: UInt256, divisor: UInt256, remainder: Boolean): UInt256 {
    if (divisor.w3 == 0uL && divisor.w2 == 0uL && divisor.w1 == 0uL) {
        return if (remainder) {
            UInt256(0uL, 0uL, 0uL, uint256RemULong(dividend, divisor.w0))
        } else {
            uint256DivULong(dividend, divisor.w0)
        }
    }
    if (dividend < divisor) {
        return if (remainder) dividend else UInt256.ZERO
    }
    return uint256DivRemWords(dividend, divisor, remainder)
}

/**
 * Returns either the quotient or the [remainder] of [dividend] divided by a [divisor] of at least two words, using
 * Knuth's Algorithm D (The Art of Computer Programming, Vol. 2, 4.3.1) on 64-bit words.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
private fun uint256DivRemWords(dividend: UInt256, divisor: UInt256, remainder: Boolean): UInt256 {
    val m = WORD_COUNT - dividend.countLeadingZeroBits() / WORD_BITS
    val n = WORD_COUNT - divisor.countLeadingZeroBits() / WORD_BITS

    // Normalise so the most significant word of the divisor has its top bit set
    val s = word(divisor, n - 1).countLeadingZeroBits()
    val vn = ULongArray(n) { i -> (word(divisor, i) shl s) or carryRight(word(divisor, i - 1), s) }
    val un = ULongArray(m + 1) { i -> (word(dividend, i) shl s) or carryRight(word(dividend, i - 1), s) }
    val q = ULongArray(WORD_COUNT)

    val vTop = vn[n - 1]
    val vNext = vn[n - 2]

    for (j in m - n downTo 0) {
        // Estimate the quotient word from the top two words of the remainder, then correct it if it's too large
        var qHat: ULong
        var rHat: ULong
        var rHatOverflow: Boolean
        if (un[j + n] >= vTop) {
            qHat = ULong.MAX_VALUE
            rHat = un[j + n - 1] + un[j + n]
            rHatOverflow = rHat < un[j + n - 1]
        } else {
            qHat = u256DivideWide(un[j + n], un[j + n - 1], vTop)
            rHat = un[j + n - 1] - qHat * vTop
            rHatOverflow = false
        }
        while (!rHatOverflow) {
            val productHigh = u256MultiplyHigh(qHat, vNext)
            val productLow = qHat * vNext
            if (productHigh < rHat || (productHigh == rHat && productLow <= un[j + n - 2])) break
            qHat--
            val previous = rHat
            rHat += vTop
            rHatOverflow = rHat < previous
        }

        // Multiply and subtract
        var mulCarry = 0uL
        var borrow = 0uL
        for (i in 0 until n) {
            val productLow = qHat * vn[i]
            val subtrahend = productLow + mulCarry
            mulCarry = u256MultiplyHigh(qHat, vn[i]) + if (subtrahend < productLow) 1uL else 0uL
            val x = un[i + j]
            val y = x - subtrahend
            val z = y - borrow
            borrow = (if (x < subtrahend) 1uL else 0uL) + (if (y < borrow) 1uL else 0uL)
            un[i + j] = z
        }
        val x = un[j + n]
        val y = x - mulCarry
        un[j + n] = y - borrow
        val negative = x < mulCarry || y < borrow

        q[j] = qHat
        if (negative) {
            // The estimate was one too large, so add the divisor back
            q[j]--
            var carry = 0uL
            for (i in 0 until n) {
                val sum = un[i + j] + vn[i]
                val total = sum + carry
                carry = (if (sum < vn[i]) 1uL else 0uL) + (if (total < sum) 1uL else 0uL)
                un[i + j] = total
            }
            un[j + n] += carry
        }
    }

    if (!remainder) return UInt256(q[3], q[2], q[1], q[0])

    // Denormalise the remainder
    val r = ULongArray(WORD_COUNT)
    for (i in 0 until n) {
        r[i] = (un[i] shr s) or carryLeft(un[i + 1], s)
    }
    return UInt256(r[3], r[2], r[1], r[0])
}

/** The bits shifted out of the top of [v] by `shl s`, guarding against `shr 64` being a no-op. */
private fun carryRight(v: ULong, s: Int): ULong = if (s == 0) 0uL else v shr (WORD_BITS - s)

/** The bits shifted out of the bottom of [v] by `shr s`, guarding against `shl 64` being a no-op. */
private fun carryLeft(v: ULong, s: Int): ULong = if (s == 0) 0uL else v shl (WORD_BITS - s)

/**
 * Returns the quotient of the 128-bit value ([high], [low]) divided by [divisor], which must be greater than [high]
 * so the quotient fits in a ULong. Knuth's Algorithm D on 32-bit halves (Hacker's Delight, 2nd ed., figure 9-3).
 */
internal fun u256DivideWide(high: ULong, low: ULong, divisor: ULong): ULong {
    // Normalise so the divisor's top bit is set, guarding against `shr 64` being a no-op
    val s = divisor.countLeadingZeroBits()
    val v = divisor shl s
    val v1 = v shr HALF_BITS
    val v0 = v and HALF_MASK
    val u32 = if (s == 0) high else (high shl s) or (low shr (WORD_BITS - s))
    val u10 = low shl s
    val u1 = u10 shr HALF_BITS
    val u0 = u10 and HALF_MASK

    // Estimate each 32-bit quotient digit from the top digit of the divisor, then correct it if it's too large
    var q1 = u256Divide(u32, v1)
    var rHat = u32 - q1 * v1
    while (q1 > HALF_MASK || q1 * v0 > ((rHat shl HALF_BITS) or u1)) {
        q1--
        rHat += v1
        if (rHat > HALF_MASK) break
    }

    val u21 = (u32 shl HALF_BITS) + u1 - q1 * v
    var q0 = u256Divide(u21, v1)
    rHat = u21 - q0 * v1
    while (q0 > HALF_MASK || q0 * v0 > ((rHat shl HALF_BITS) or u0)) {
        q0--
        rHat += v1
        if (rHat > HALF_MASK) break
    }

    return (q1 shl HALF_BITS) or q0
}

internal fun uint256Shl(v: UInt256, bitCount: Int): UInt256 {
    val n = bitCount and (UInt256.SIZE_BITS - 1)
    if (n == 0) return v
    val words = n / WORD_BITS
    val bits = n % WORD_BITS
    fun shifted(index: Int): ULong {
        val source = index - words
        return (word(v, source) shl bits) or if (bits == 0) 0uL else word(v, source - 1) shr (WORD_BITS - bits)
    }
    return UInt256(shifted(3), shifted(2), shifted(1), shifted(0))
}

internal fun uint256Shr(v: UInt256, bitCount: Int): UInt256 {
    val n = bitCount and (UInt256.SIZE_BITS - 1)
    if (n == 0) return v
    val words = n / WORD_BITS
    val bits = n % WORD_BITS
    fun shifted(index: Int): ULong {
        val source = index + words
        return (word(v, source) shr bits) or if (bits == 0) 0uL else word(v, source + 1) shl (WORD_BITS - bits)
    }
    return UInt256(shifted(3), shifted(2), shifted(1), shifted(0))
}

// The scalar operators are inline, so these are published for them to call. Each works on the scalar directly rather
// than widening it to a UInt256 first, saving an allocation and the arithmetic on its zero upper words.

@PublishedApi
internal fun uint256CompareULong(v: UInt256, other: ULong): Int =
    if (v.w3 != 0uL || v.w2 != 0uL || v.w1 != 0uL) 1 else v.w0.compareTo(other)

@PublishedApi
internal fun uint256PlusULong(v: UInt256, other: ULong): UInt256 {
    val r0 = v.w0 + other
    if (r0 >= other) return UInt256(v.w3, v.w2, v.w1, r0)
    val r1 = v.w1 + 1uL
    if (r1 != 0uL) return UInt256(v.w3, v.w2, r1, r0)
    val r2 = v.w2 + 1uL
    return UInt256(if (r2 == 0uL) v.w3 + 1uL else v.w3, r2, r1, r0)
}

@PublishedApi
internal fun uint256MinusULong(v: UInt256, other: ULong): UInt256 {
    val r0 = v.w0 - other
    if (v.w0 >= other) return UInt256(v.w3, v.w2, v.w1, r0)
    val r1 = v.w1 - 1uL
    if (v.w1 != 0uL) return UInt256(v.w3, v.w2, r1, r0)
    val r2 = v.w2 - 1uL
    return UInt256(if (v.w2 == 0uL) v.w3 - 1uL else v.w3, r2, r1, r0)
}

@PublishedApi
internal fun uint256TimesULong(v: UInt256, other: ULong): UInt256 {
    val r0 = v.w0 * other
    var carry = u256MultiplyHigh(v.w0, other)
    val p1 = v.w1 * other
    val r1 = p1 + carry
    carry = u256MultiplyHigh(v.w1, other) + if (r1 < p1) 1uL else 0uL
    val p2 = v.w2 * other
    val r2 = p2 + carry
    carry = u256MultiplyHigh(v.w2, other) + if (r2 < p2) 1uL else 0uL
    return UInt256(v.w3 * other + carry, r2, r1, r0)
}

@PublishedApi
internal fun uint256DivULong(dividend: UInt256, divisor: ULong): UInt256 {
    if (divisor == 0uL) throw ArithmeticException("/ by zero")
    // Short division, one word at a time, each leaving a remainder below the divisor so the next step can't overflow
    val q3 = u256Divide(dividend.w3, divisor)
    val q2 = u256DivideWide(dividend.w3 - q3 * divisor, dividend.w2, divisor)
    val q1 = u256DivideWide(dividend.w2 - q2 * divisor, dividend.w1, divisor)
    val q0 = u256DivideWide(dividend.w1 - q1 * divisor, dividend.w0, divisor)
    return UInt256(q3, q2, q1, q0)
}

@PublishedApi
internal fun uint256RemULong(dividend: UInt256, divisor: ULong): ULong {
    if (divisor == 0uL) throw ArithmeticException("/ by zero")
    var r = u256Remainder(dividend.w3, divisor)
    r = dividend.w2 - u256DivideWide(r, dividend.w2, divisor) * divisor
    r = dividend.w1 - u256DivideWide(r, dividend.w1, divisor) * divisor
    // Each remainder is below the divisor, so only its lower word survives the wrapping subtraction
    return dividend.w0 - u256DivideWide(r, dividend.w0, divisor) * divisor
}

private const val TWO_POW_64: Double = 18446744073709551616.0

/** Returns the floor of the square root of [v]. */
internal fun uint256Isqrt(v: UInt256): UInt256 {
    if (v.w3 == 0uL && v.w2 == 0uL) return v.lower.isqrt().toUInt256()

    // A Double estimate is within a relative 2^-53 of the root. Each Newton step, (r + v / r) / 2, squares the
    // relative error, so two bring it within one. Neither lands below the root, as the arithmetic mean of r and v / r
    // is at least their geometric mean.
    val estimate = sqrt(((v.w3.toDouble() * TWO_POW_64 + v.w2.toDouble()) * TWO_POW_64 + v.w1.toDouble()) * TWO_POW_64)
    val estimateUpper = (estimate / TWO_POW_64).toULong()
    val estimateLower = (estimate - estimateUpper.toDouble() * TWO_POW_64).coerceAtLeast(0.0).toULong()
    var root = UInt256(0uL, 0uL, estimateUpper, estimateLower)
    repeat(2) {
        root = (root + v / root) shr 1
    }
    // The root of the largest value is 2^128 - 1, which the steps can overshoot
    if (root > MAX_ROOT) root = MAX_ROOT

    while (root * root > v) root--
    return root
}

private val MAX_ROOT = UInt256(0uL, 0uL, ULong.MAX_VALUE, ULong.MAX_VALUE)
