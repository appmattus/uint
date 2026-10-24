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

/**
 * Returns a string representation of this [UInt128] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@Suppress("ExceptionRaisedInUnexpectedLocation")
public fun UInt128.toString(radix: Int): String {
    if (radix < UInt128.MIN_RADIX || radix > UInt128.MAX_RADIX) {
        throw IllegalArgumentException("Illegal radix: $radix")
    }

    if (upper == 0uL) {
        return lower.toString(radix)
    }

    // Peel off chunks of digits using the largest power of the radix that fits in 32 bits, so each step is a cheap
    // short division, until the remaining value fits in a ULong
    val uradix = radix.toULong()
    var chunkDivisor = uradix
    var chunkDigits = 1
    while (chunkDivisor * uradix <= UInt.MAX_VALUE) {
        chunkDivisor *= uradix
        chunkDigits++
    }

    val chunks = mutableListOf<String>()
    var hi = upper
    var lo = lower
    while (hi != 0uL) {
        var r = hi shr UInt.SIZE_BITS
        val q3 = uint64Divide(r, chunkDivisor)
        r = ((r - q3 * chunkDivisor) shl UInt.SIZE_BITS) or (hi and UInt.MAX_VALUE.toULong())
        val q2 = uint64Divide(r, chunkDivisor)
        r = ((r - q2 * chunkDivisor) shl UInt.SIZE_BITS) or (lo shr UInt.SIZE_BITS)
        val q1 = uint64Divide(r, chunkDivisor)
        r = ((r - q1 * chunkDivisor) shl UInt.SIZE_BITS) or (lo and UInt.MAX_VALUE.toULong())
        val q0 = uint64Divide(r, chunkDivisor)
        chunks += (r - q0 * chunkDivisor).toString(radix).padStart(chunkDigits, '0')
        hi = (q3 shl UInt.SIZE_BITS) or q2
        lo = (q1 shl UInt.SIZE_BITS) or q0
    }

    return buildString {
        append(lo.toString(radix))
        for (i in chunks.indices.reversed()) append(chunks[i])
    }
}

/**
 * Parses the string as a [UInt128] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toUInt128(): UInt128 = toUInt128OrNull() ?: throw NumberFormatException("Invalid number format: '$this'")

/**
 * Parses the string as a [UInt128] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public fun String.toUInt128(radix: Int): UInt128 = toUInt128OrNull(radix) ?: throw NumberFormatException("Invalid number format: '$this'")

/**
 * Parses the string as a [UInt128] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public inline fun String.toUInt128OrNull(): UInt128? = toUInt128OrNull(radix = 10)

/**
 * Parses the string as a [UInt128] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@Suppress("ReturnCount")
public fun String.toUInt128OrNull(radix: Int): UInt128? {
    if (radix < UInt128.MIN_RADIX || radix > UInt128.MAX_RADIX) {
        throw IllegalArgumentException("Illegal radix: $radix")
    }

    if (this.isEmpty()) return null

    val start: Int

    val firstChar = this[0]
    if (firstChar < '0') {
        if (this.length == 1 || firstChar != '+') return null
        start = 1
    } else {
        start = 0
    }

    // Accumulate as many digits as fit in a ULong, then fold each chunk into the result with a single multiply-add
    val uradix = radix.toULong()
    val chunkDigits = parseChunkDigits[radix]
    var upper = 0uL
    var lower = 0uL
    var i = start
    while (i < this.length) {
        val end = minOf(i + chunkDigits, this.length)
        var chunk = 0uL
        var multiplier = 1uL
        while (i < end) {
            val digit = this[i++].digitToIntOrNull(radix) ?: return null
            chunk = chunk * uradix + digit.toULong()
            multiplier *= uradix
        }

        // (upper, lower) * multiplier + chunk, returning null if the result doesn't fit in 128 bits
        if (uint64MultiplyHigh(upper, multiplier) != 0uL) return null
        val upperProduct = upper * multiplier
        val lowerProduct = lower * multiplier
        upper = upperProduct + uint64MultiplyHigh(lower, multiplier)
        if (upper < upperProduct) return null
        lower = lowerProduct + chunk
        if (lower < lowerProduct) {
            upper++
            if (upper == 0uL) return null
        }
    }

    return UInt128(upper, lower)
}

/** The most digits of each radix, indexed by radix, whose value and place multiplier always fit in a [ULong]. */
private val parseChunkDigits = IntArray(UInt128.MAX_RADIX + 1) { radix ->
    var digits = 0
    if (radix >= UInt128.MIN_RADIX) {
        var multiplier = 1uL
        while (multiplier <= ULong.MAX_VALUE / radix.toULong()) {
            multiplier *= radix.toULong()
            digits++
        }
    }
    digits
}
