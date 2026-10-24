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

import kotlin.math.sqrt

private const val HALF_MASK: ULong = 0xFFFFFFFFu
private const val HALF_BITS: Int = UInt.SIZE_BITS

internal fun uint128Compare(v1: UInt128, v2: UInt128): Int {
    val cmp = v1.upper.compareTo(v2.upper)
    if (cmp == 0) {
        return v1.lower.compareTo(v2.lower)
    }
    return cmp
}

internal fun uint128Plus(v1: UInt128, v2: UInt128): UInt128 {
    val lower = v1.lower + v2.lower
    val carry = if (lower < v1.lower) 1uL else 0uL
    return UInt128(v1.upper + v2.upper + carry, lower)
}

internal fun uint128Minus(v1: UInt128, v2: UInt128): UInt128 {
    val borrow = if (v1.lower < v2.lower) 1uL else 0uL
    return UInt128(v1.upper - v2.upper - borrow, v1.lower - v2.lower)
}

internal fun uint128Times(v1: UInt128, v2: UInt128): UInt128 {
    // The cross terms only contribute to the upper word, so their overflow can be discarded
    val upper = uint64MultiplyHigh(v1.lower, v2.lower) + v1.lower * v2.upper + v1.upper * v2.lower
    return UInt128(upper, v1.lower * v2.lower)
}

/** Returns the upper 64 bits of the 128-bit product of [x] and [y], using a platform intrinsic where available. */
internal expect fun uint64MultiplyHigh(x: ULong, y: ULong): ULong

/** Returns the upper 64 bits of the 128-bit product of [x] and [y]. */
internal fun uint64MultiplyHighPortable(x: ULong, y: ULong): ULong {
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

/** Returns [dividend] / [divisor], working around slow unsigned division on some platforms. */
internal expect fun uint64Divide(dividend: ULong, divisor: ULong): ULong

/** Returns [dividend] % [divisor], working around slow unsigned division on some platforms. */
internal expect fun uint64Remainder(dividend: ULong, divisor: ULong): ULong

internal fun uint128Div(dividend: UInt128, divisor: UInt128): UInt128 = uint128DivRem(dividend, divisor, remainder = false)

internal fun uint128Rem(dividend: UInt128, divisor: UInt128): UInt128 = uint128DivRem(dividend, divisor, remainder = true)

@Suppress("ReturnCount")
private fun uint128DivRem(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128 {
    if (divisor.upper == 0uL) {
        if (divisor.lower == 0uL) throw ArithmeticException("/ by zero")
        if (dividend.upper == 0uL) {
            val result = if (remainder) {
                uint64Remainder(dividend.lower, divisor.lower)
            } else {
                uint64Divide(dividend.lower, divisor.lower)
            }
            return UInt128(0uL, result)
        }
    }
    if (dividend < divisor) {
        return if (remainder) dividend else UInt128.ZERO
    }

    return uint128DivRemWords(dividend, divisor, remainder)
}

/**
 * Returns either the quotient or the [remainder] of [dividend] divided by [divisor] using only 64-bit arithmetic, so
 * nothing is allocated beyond the result. Follows the word division in Hacker's Delight (2nd ed., 9-4 and 9-5).
 * Only called once the divisor is known to be non-zero, the dividend is at least the divisor, and at least one of them
 * doesn't fit in a [ULong].
 */
private fun uint128DivRemWords(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128 {
    if (divisor.upper == 0uL) {
        // Divide the upper word first, leaving a remainder below the divisor so the second step can't overflow. This
        // repeats uint128DivULong rather than calling it, as the extra call depth stops JDK 11 inlining divideWide.
        val d = divisor.lower
        val upperQuotient = uint64Divide(dividend.upper, d)
        val lowerQuotient = divideWide(dividend.upper - upperQuotient * d, dividend.lower, d)
        // The remainder is below the divisor, so only its lower word survives the wrapping subtraction
        return if (remainder) UInt128(0uL, dividend.lower - lowerQuotient * d) else UInt128(upperQuotient, lowerQuotient)
    }

    // The divisor is at least 2^64, so the quotient fits in a ULong. Estimate it from the top word of the normalised
    // divisor and half the dividend, which keeps the estimate from overflowing; it's then exact or one too small.
    val n = divisor.upper.countLeadingZeroBits()
    val divisorTop = if (n == 0) divisor.upper else (divisor.upper shl n) or (divisor.lower shr (ULong.SIZE_BITS - n))
    val halfUpper = dividend.upper shr 1
    val halfLower = (dividend.lower shr 1) or (dividend.upper shl (ULong.SIZE_BITS - 1))
    var quotient = divideWide(halfUpper, halfLower, divisorTop) shr (ULong.SIZE_BITS - 1 - n)
    if (quotient != 0uL) quotient--

    // remainder = dividend - quotient * divisor
    val productLower = quotient * divisor.lower
    val productUpper = uint64MultiplyHigh(quotient, divisor.lower) + quotient * divisor.upper
    var remainderLower = dividend.lower - productLower
    var remainderUpper = dividend.upper - productUpper - (if (dividend.lower < productLower) 1uL else 0uL)

    if (remainderUpper > divisor.upper || (remainderUpper == divisor.upper && remainderLower >= divisor.lower)) {
        quotient++
        val borrow = if (remainderLower < divisor.lower) 1uL else 0uL
        remainderLower -= divisor.lower
        remainderUpper = remainderUpper - divisor.upper - borrow
    }

    return if (remainder) UInt128(remainderUpper, remainderLower) else UInt128(0uL, quotient)
}

// The scalar operators are inline, so these are published for them to call. Each works on the scalar directly rather
// than widening it to a UInt128 first, saving an allocation and the arithmetic on its zero upper word.

@PublishedApi
internal fun uint128CompareULong(v: UInt128, other: ULong): Int = if (v.upper != 0uL) 1 else v.lower.compareTo(other)

@PublishedApi
internal fun uint128PlusULong(v: UInt128, other: ULong): UInt128 {
    val lower = v.lower + other
    val carry = if (lower < other) 1uL else 0uL
    return UInt128(v.upper + carry, lower)
}

@PublishedApi
internal fun uint128MinusULong(v: UInt128, other: ULong): UInt128 {
    val borrow = if (v.lower < other) 1uL else 0uL
    return UInt128(v.upper - borrow, v.lower - other)
}

@PublishedApi
internal fun uint128TimesULong(v: UInt128, other: ULong): UInt128 =
    UInt128(uint64MultiplyHigh(v.lower, other) + v.upper * other, v.lower * other)

@PublishedApi
internal fun uint128DivULong(dividend: UInt128, divisor: ULong): UInt128 {
    if (divisor == 0uL) throw ArithmeticException("/ by zero")
    if (dividend.upper == 0uL) return UInt128(0uL, uint64Divide(dividend.lower, divisor))
    // Divide the upper word first, leaving a remainder below the divisor so the second step can't overflow
    val upperQuotient = uint64Divide(dividend.upper, divisor)
    return UInt128(upperQuotient, divideWide(dividend.upper - upperQuotient * divisor, dividend.lower, divisor))
}

@PublishedApi
internal fun uint128RemULong(dividend: UInt128, divisor: ULong): ULong {
    if (divisor == 0uL) throw ArithmeticException("/ by zero")
    if (dividend.upper == 0uL) return uint64Remainder(dividend.lower, divisor)
    val upperQuotient = uint64Divide(dividend.upper, divisor)
    val lowerQuotient = divideWide(dividend.upper - upperQuotient * divisor, dividend.lower, divisor)
    // The remainder is below the divisor, so only its lower word survives the wrapping subtraction
    return dividend.lower - lowerQuotient * divisor
}

private const val TWO_POW_64: Double = 18446744073709551616.0

/** Returns the floor of the square root of [v], which always fits in a [ULong]. */
internal fun uint128Isqrt(v: UInt128): ULong {
    if (v.upper == 0uL) return uint64Isqrt(v.lower)

    // A Double estimate is within about 2^11 of the root. One Newton step, (r + v / r) / 2, brings that within one,
    // and never below the root, as the arithmetic mean of r and v / r is at least their geometric mean.
    val estimate = sqrt(v.upper.toDouble() * TWO_POW_64 + v.lower.toDouble()).toULong()
    val upperQuotient = uint64Divide(v.upper, estimate)
    val lowerQuotient = divideWide(v.upper - upperQuotient * estimate, v.lower, estimate)
    val sumLower = lowerQuotient + estimate
    val sumUpper = upperQuotient + if (sumLower < estimate) 1uL else 0uL
    // The step can reach 2^64 near the top of the range, where the root is ULong.MAX_VALUE
    var root = if (sumUpper > 1uL) ULong.MAX_VALUE else (sumLower shr 1) or (sumUpper shl (ULong.SIZE_BITS - 1))

    while (squareExceeds(root, v)) root--
    return root
}

/** Returns the floor of the square root of [v]. */
private fun uint64Isqrt(v: ULong): ULong {
    // A Double estimate is within one of the root, but can be either side of it. Clamping keeps root * root in range.
    var root = sqrt(v.toDouble()).toULong().coerceAtMost(UInt.MAX_VALUE.toULong())
    while (root * root > v) root--
    while (root < UInt.MAX_VALUE.toULong() && (root + 1uL) * (root + 1uL) <= v) root++
    return root
}

/** Returns whether [root] squared is greater than [v]. */
private fun squareExceeds(root: ULong, v: UInt128): Boolean {
    val upper = uint64MultiplyHigh(root, root)
    return upper > v.upper || (upper == v.upper && root * root > v.lower)
}

/**
 * Returns the quotient of the 128-bit value ([high], [low]) divided by [divisor], which must be greater than [high]
 * so the quotient fits in a ULong. Knuth's Algorithm D on 32-bit halves (Hacker's Delight, 2nd ed., figure 9-3).
 */
private fun divideWide(high: ULong, low: ULong, divisor: ULong): ULong {
    // Normalise so the divisor's top bit is set, guarding against `shr 64` being a no-op
    val s = divisor.countLeadingZeroBits()
    val v = divisor shl s
    val v1 = v shr HALF_BITS
    val v0 = v and HALF_MASK
    val u32 = if (s == 0) high else (high shl s) or (low shr (ULong.SIZE_BITS - s))
    val u10 = low shl s
    val u1 = u10 shr HALF_BITS
    val u0 = u10 and HALF_MASK

    // Estimate each 32-bit quotient digit from the top digit of the divisor, then correct it if it's too large
    var q1 = uint64Divide(u32, v1)
    var rHat = u32 - q1 * v1
    while (q1 > HALF_MASK || q1 * v0 > ((rHat shl HALF_BITS) or u1)) {
        q1--
        rHat += v1
        if (rHat > HALF_MASK) break
    }

    val u21 = (u32 shl HALF_BITS) + u1 - q1 * v
    var q0 = uint64Divide(u21, v1)
    rHat = u21 - q0 * v1
    while (q0 > HALF_MASK || q0 * v0 > ((rHat shl HALF_BITS) or u0)) {
        q0--
        rHat += v1
        if (rHat > HALF_MASK) break
    }

    return (q1 shl HALF_BITS) or q0
}

internal fun uint128Shl(v: UInt128, bitCount: Int): UInt128 {
    val n = bitCount and (UInt128.SIZE_BITS - 1)
    return when {
        n == 0 -> v
        n < ULong.SIZE_BITS -> UInt128((v.upper shl n) or (v.lower shr (ULong.SIZE_BITS - n)), v.lower shl n)
        else -> UInt128(v.lower shl (n - ULong.SIZE_BITS), 0uL)
    }
}

internal fun uint128Shr(v: UInt128, bitCount: Int): UInt128 {
    val n = bitCount and (UInt128.SIZE_BITS - 1)
    return when {
        n == 0 -> v
        n < ULong.SIZE_BITS -> UInt128(v.upper shr n, (v.lower shr n) or (v.upper shl (ULong.SIZE_BITS - n)))
        else -> UInt128(0uL, v.upper shr (n - ULong.SIZE_BITS))
    }
}
