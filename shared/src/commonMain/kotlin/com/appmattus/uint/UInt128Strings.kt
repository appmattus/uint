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
        val q3 = r / chunkDivisor
        r = ((r % chunkDivisor) shl UInt.SIZE_BITS) or (hi and UInt.MAX_VALUE.toULong())
        val q2 = r / chunkDivisor
        r = ((r % chunkDivisor) shl UInt.SIZE_BITS) or (lo shr UInt.SIZE_BITS)
        val q1 = r / chunkDivisor
        r = ((r % chunkDivisor) shl UInt.SIZE_BITS) or (lo and UInt.MAX_VALUE.toULong())
        val q0 = r / chunkDivisor
        chunks += (r % chunkDivisor).toString(radix).padStart(chunkDigits, '0')
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
@Suppress("NestedBlockDepth", "ReturnCount")
public fun String.toUInt128OrNull(radix: Int): UInt128? {
    if (radix < UInt128.MIN_RADIX || radix > UInt128.MAX_RADIX) {
        throw IllegalArgumentException("Illegal radix: $radix")
    }

    if (this.isEmpty()) return null

    val limit: UInt128 = UInt128.MAX_VALUE
    val start: Int

    val firstChar = this[0]
    if (firstChar < '0') {
        if (this.length == 1 || firstChar != '+') return null
        start = 1
    } else {
        start = 0
    }

    val limitForMaxRadix = limit / UInt128.MAX_RADIX.toUInt128() //  9452287970026068429538183539771339207  //  limit / 36

    var limitBeforeMul = limitForMaxRadix
    val uradix = radix.toUInt128()
    var result = UInt128.ZERO
    for (i in start until this.length) {
        val digit = this[i].digitToIntOrNull(radix) ?: return null
        if (result > limitBeforeMul) {
            if (limitBeforeMul == limitForMaxRadix) {
                limitBeforeMul = limit / uradix

                if (result > limitBeforeMul) {
                    return null
                }
            } else {
                return null
            }
        }

        result *= uradix

        val beforeAdding = result
        result += digit.toUInt128()
        if (result < beforeAdding) return null // overflow has happened
    }

    return result
}
