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

private const val LIMB_MASK: ULong = 0xFFFFFFFFu
private const val LIMB_BITS: Int = UInt.SIZE_BITS
private const val LIMB_COUNT: Int = UInt128.SIZE_BITS / LIMB_BITS

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

/** Multiplies two values, using the platform's native 128-bit integer support where available. */
internal expect fun uint128Times(v1: UInt128, v2: UInt128): UInt128

/**
 * Returns either the quotient or the [remainder] of [dividend] divided by [divisor], using the platform's native
 * 128-bit integer support where available. Only called once the divisor is known to be non-zero, the dividend is at
 * least the divisor, and at least one of them doesn't fit in a [ULong].
 */
internal expect fun uint128PlatformDivRem(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128

internal fun uint128TimesPortable(v1: UInt128, v2: UInt128): UInt128 {
    // The cross terms only contribute to the upper word, so their overflow can be discarded
    val upper = multiplyHigh(v1.lower, v2.lower) + v1.lower * v2.upper + v1.upper * v2.lower
    return UInt128(upper, v1.lower * v2.lower)
}

/** Returns the upper 64 bits of the 128-bit product of [x] and [y]. */
private fun multiplyHigh(x: ULong, y: ULong): ULong {
    val x0 = x and LIMB_MASK
    val x1 = x shr LIMB_BITS
    val y0 = y and LIMB_MASK
    val y1 = y shr LIMB_BITS

    val p00 = x0 * y0
    val p01 = x0 * y1
    val p10 = x1 * y0
    val p11 = x1 * y1

    val middle = (p00 shr LIMB_BITS) + (p01 and LIMB_MASK) + (p10 and LIMB_MASK)
    return p11 + (p01 shr LIMB_BITS) + (p10 shr LIMB_BITS) + (middle shr LIMB_BITS)
}

internal fun uint128Div(dividend: UInt128, divisor: UInt128): UInt128 = uint128DivRem(dividend, divisor, remainder = false)

internal fun uint128Rem(dividend: UInt128, divisor: UInt128): UInt128 = uint128DivRem(dividend, divisor, remainder = true)

@Suppress("ReturnCount")
private fun uint128DivRem(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128 {
    if (divisor.upper == 0uL) {
        if (divisor.lower == 0uL) throw ArithmeticException("/ by zero")
        if (dividend.upper == 0uL) {
            val result = if (remainder) dividend.lower % divisor.lower else dividend.lower / divisor.lower
            return UInt128(0uL, result)
        }
    }
    if (dividend < divisor) {
        return if (remainder) dividend else UInt128.ZERO
    }

    return uint128PlatformDivRem(dividend, divisor, remainder)
}

/**
 * Returns either the quotient or the [remainder] of [dividend] divided by [divisor] using Knuth's Algorithm D
 * (The Art of Computer Programming, Vol. 2, 4.3.1) on 32-bit limbs.
 */
@Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
internal fun uint128DivRemPortable(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128 {
    val u = dividend.toLimbs()
    val v = divisor.toLimbs()
    val m = LIMB_COUNT - dividend.countLeadingZeroBits() / LIMB_BITS
    val n = LIMB_COUNT - divisor.countLeadingZeroBits() / LIMB_BITS
    val q = IntArray(LIMB_COUNT)

    if (n == 1) {
        // Short division by a single limb
        val d = v[0].toULimb()
        var r = 0uL
        for (j in m - 1 downTo 0) {
            val current = (r shl LIMB_BITS) or u[j].toULimb()
            q[j] = (current / d).toInt()
            r = current % d
        }
        return if (remainder) UInt128(0uL, r) else q.toUInt128()
    }

    // Normalise so the most significant limb of the divisor has its top bit set
    val s = v[n - 1].countLeadingZeroBits()
    val vn = IntArray(n)
    for (i in n - 1 downTo 1) {
        vn[i] = (v[i] shl s) or v[i - 1].carryRight(s)
    }
    vn[0] = v[0] shl s

    val un = IntArray(m + 1)
    un[m] = u[m - 1].carryRight(s)
    for (i in m - 1 downTo 1) {
        un[i] = (u[i] shl s) or u[i - 1].carryRight(s)
    }
    un[0] = u[0] shl s

    val base = 1uL shl LIMB_BITS
    val vTop = vn[n - 1].toULimb()
    val vNext = vn[n - 2].toULimb()

    for (j in m - n downTo 0) {
        // Estimate the quotient digit and correct it if it's too large
        val numerator = (un[j + n].toULimb() shl LIMB_BITS) or un[j + n - 1].toULimb()
        var qHat = numerator / vTop
        var rHat = numerator - qHat * vTop
        while (qHat >= base || qHat * vNext > ((rHat shl LIMB_BITS) or un[j + n - 2].toULimb())) {
            qHat--
            rHat += vTop
            if (rHat >= base) break
        }

        // Multiply and subtract
        var borrow = 0L
        var t: Long
        for (i in 0 until n) {
            val p = qHat * vn[i].toULimb()
            t = un[i + j].toULimb().toLong() - borrow - (p and LIMB_MASK).toLong()
            un[i + j] = t.toInt()
            borrow = (p shr LIMB_BITS).toLong() - (t shr LIMB_BITS)
        }
        t = un[j + n].toULimb().toLong() - borrow
        un[j + n] = t.toInt()

        q[j] = qHat.toInt()
        if (t < 0) {
            // The estimate was one too large, so add the divisor back
            q[j]--
            var carry = 0L
            for (i in 0 until n) {
                t = un[i + j].toULimb().toLong() + vn[i].toULimb().toLong() + carry
                un[i + j] = t.toInt()
                carry = t ushr LIMB_BITS
            }
            un[j + n] += carry.toInt()
        }
    }

    if (!remainder) return q.toUInt128()

    // Denormalise the remainder
    val r = IntArray(LIMB_COUNT)
    for (i in 0 until n) {
        r[i] = (un[i] ushr s) or un[i + 1].carryLeft(s)
    }
    return r.toUInt128()
}

private fun Int.toULimb(): ULong = toUInt().toULong()

/** The bits shifted out of the top of this limb by `shl s`, guarding against `ushr 32` being a no-op. */
private fun Int.carryRight(s: Int): Int = if (s == 0) 0 else this ushr (LIMB_BITS - s)

/** The bits shifted out of the bottom of this limb by `ushr s`, guarding against `shl 32` being a no-op. */
private fun Int.carryLeft(s: Int): Int = if (s == 0) 0 else this shl (LIMB_BITS - s)

/** Little-endian 32-bit limbs. */
private fun UInt128.toLimbs(): IntArray = intArrayOf(low.toInt(), midLow.toInt(), midHigh.toInt(), high.toInt())

private fun IntArray.toUInt128(): UInt128 = UInt128(this[3].toUInt(), this[2].toUInt(), this[1].toUInt(), this[0].toUInt())

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
