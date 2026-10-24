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

/**
 * Returns a string representation of this [UInt256] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("ExceptionRaisedInUnexpectedLocation")
public fun UInt256.toString(radix: Int): String {
    if (radix < UInt256.MIN_RADIX || radix > UInt256.MAX_RADIX) {
        throw IllegalArgumentException("Illegal radix: $radix")
    }

    if (w3 == 0uL && w2 == 0uL && w1 == 0uL) {
        return w0.toString(radix)
    }

    // Peel off chunks of digits using the largest power of the radix that fits in a ULong, so each step is a short
    // division, until the remaining value fits in a ULong
    val chunkDivisor = chunkDivisors[radix]
    val chunkDigits = chunkDigits[radix]
    val chunks = mutableListOf<String>()
    var w3 = w3
    var w2 = w2
    var w1 = w1
    var w0 = w0
    while (w3 != 0uL || w2 != 0uL || w1 != 0uL) {
        val q3 = u256Divide(w3, chunkDivisor)
        val q2 = u256DivideWide(w3 - q3 * chunkDivisor, w2, chunkDivisor)
        val q1 = u256DivideWide(w2 - q2 * chunkDivisor, w1, chunkDivisor)
        val q0 = u256DivideWide(w1 - q1 * chunkDivisor, w0, chunkDivisor)
        chunks += (w0 - q0 * chunkDivisor).toString(radix).padStart(chunkDigits, '0')
        w3 = q3
        w2 = q2
        w1 = q1
        w0 = q0
    }

    return buildString {
        append(w0.toString(radix))
        for (i in chunks.indices.reversed()) append(chunks[i])
    }
}

/**
 * Parses the string as a [UInt256] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toUInt256(): UInt256 = toUInt256OrNull() ?: throw NumberFormatException("Invalid number format: '$this'")

/**
 * Parses the string as a [UInt256] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public fun String.toUInt256(radix: Int): UInt256 = toUInt256OrNull(radix) ?: throw NumberFormatException("Invalid number format: '$this'")

/**
 * Parses the string as a [UInt256] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public inline fun String.toUInt256OrNull(): UInt256? = toUInt256OrNull(radix = 10)

/**
 * Parses the string as a [UInt256] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@Suppress("ReturnCount", "LongMethod", "ComplexMethod")
public fun String.toUInt256OrNull(radix: Int): UInt256? {
    if (radix < UInt256.MIN_RADIX || radix > UInt256.MAX_RADIX) {
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
    val chunkDigits = chunkDigits[radix]
    var w3 = 0uL
    var w2 = 0uL
    var w1 = 0uL
    var w0 = 0uL
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

        // (w3, w2, w1, w0) * multiplier + chunk, returning null if the result doesn't fit in 256 bits
        var carry = chunk
        var product = w0 * multiplier
        var next = u256MultiplyHigh(w0, multiplier)
        w0 = product + carry
        carry = next + if (w0 < product) 1uL else 0uL

        product = w1 * multiplier
        next = u256MultiplyHigh(w1, multiplier)
        w1 = product + carry
        carry = next + if (w1 < product) 1uL else 0uL

        product = w2 * multiplier
        next = u256MultiplyHigh(w2, multiplier)
        w2 = product + carry
        carry = next + if (w2 < product) 1uL else 0uL

        product = w3 * multiplier
        next = u256MultiplyHigh(w3, multiplier)
        w3 = product + carry
        if (next != 0uL || w3 < product) return null
    }

    return UInt256(w3, w2, w1, w0)
}

/** The most digits of each radix, indexed by radix, whose value and place multiplier always fit in a [ULong]. */
private val chunkDigits = IntArray(UInt256.MAX_RADIX + 1) { radix ->
    var digits = 0
    if (radix >= UInt256.MIN_RADIX) {
        var multiplier = 1uL
        while (multiplier <= ULong.MAX_VALUE / radix.toULong()) {
            multiplier *= radix.toULong()
            digits++
        }
    }
    digits
}

/** Each radix, indexed by radix, raised to the power of its [chunkDigits]. */
@OptIn(ExperimentalUnsignedTypes::class)
private val chunkDivisors = ULongArray(UInt256.MAX_RADIX + 1) { radix ->
    var divisor = 1uL
    if (radix >= UInt256.MIN_RADIX) repeat(chunkDigits[radix]) { divisor *= radix.toULong() }
    divisor
}
