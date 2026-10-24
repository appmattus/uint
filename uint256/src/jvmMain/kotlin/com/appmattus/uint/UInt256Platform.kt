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

// These mirror the uint128 module's JVM hooks, which are internal to it.

/**
 * HotSpot compiles [Math.multiplyHigh] to a single high-multiply instruction (smulh on arm64, imul on x86-64). Its
 * signed result is corrected for operands with their top bit set, as Java 18's Math.unsignedMultiplyHigh does.
 */
internal actual fun u256MultiplyHigh(x: ULong, y: ULong): ULong {
    val sx = x.toLong()
    val sy = y.toLong()
    return (Math.multiplyHigh(sx, sy) + ((sx shr 63) and sy) + ((sy shr 63) and sx)).toULong()
}

// Kotlin compiles ULong division to Long.divideUnsigned, which before Java 17 falls back to BigInteger whenever the
// dividend has its top bit set. Being a static final, the JIT folds this check away.
private val hasFastDivideUnsigned: Boolean = Runtime.version().feature() >= 17

internal actual fun u256Divide(dividend: ULong, divisor: ULong): ULong =
    if (hasFastDivideUnsigned) dividend / divisor else u256DivideWithoutBigInteger(dividend, divisor)

internal actual fun u256Remainder(dividend: ULong, divisor: ULong): ULong =
    if (hasFastDivideUnsigned) {
        dividend % divisor
    } else {
        dividend - u256DivideWithoutBigInteger(dividend, divisor) * divisor
    }

/**
 * Unsigned division using only signed division, as Java 17's Long.divideUnsigned does: halve the dividend so it's
 * non-negative, then correct the doubled quotient, which is at most one too small (Hacker's Delight, 2nd ed., 9-3).
 */
internal fun u256DivideWithoutBigInteger(dividend: ULong, divisor: ULong): ULong {
    val d = divisor.toLong()
    if (d < 0L) return if (dividend < divisor) 0uL else 1uL
    val n = dividend.toLong()
    if (n >= 0L) return (n / d).toULong()
    val quotient = ((n ushr 1) / d) shl 1
    val remainder = n - quotient * d
    return (quotient + if (remainder.toULong() >= divisor) 1L else 0L).toULong()
}
