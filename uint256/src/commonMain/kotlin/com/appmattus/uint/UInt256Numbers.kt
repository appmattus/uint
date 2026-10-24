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
 * Counts the number of set bits in the binary representation of this [UInt256] number.
 */
public fun UInt256.countOneBits(): Int = w3.countOneBits() + w2.countOneBits() + w1.countOneBits() + w0.countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [UInt256] number.
 */
public fun UInt256.countLeadingZeroBits(): Int = when {
    w3 != 0uL -> w3.countLeadingZeroBits()
    w2 != 0uL -> ULong.SIZE_BITS + w2.countLeadingZeroBits()
    w1 != 0uL -> 2 * ULong.SIZE_BITS + w1.countLeadingZeroBits()
    else -> 3 * ULong.SIZE_BITS + w0.countLeadingZeroBits()
}

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [UInt256] number.
 */
public fun UInt256.countTrailingZeroBits(): Int = when {
    w0 != 0uL -> w0.countTrailingZeroBits()
    w1 != 0uL -> ULong.SIZE_BITS + w1.countTrailingZeroBits()
    w2 != 0uL -> 2 * ULong.SIZE_BITS + w2.countTrailingZeroBits()
    else -> 3 * ULong.SIZE_BITS + w3.countTrailingZeroBits()
}

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [UInt256] number,
 * or zero, if this number is zero.
 */
public fun UInt256.takeHighestOneBit(): UInt256 = when {
    w3 != 0uL -> UInt256(w3.takeHighestOneBit(), 0uL, 0uL, 0uL)
    w2 != 0uL -> UInt256(0uL, w2.takeHighestOneBit(), 0uL, 0uL)
    w1 != 0uL -> UInt256(0uL, 0uL, w1.takeHighestOneBit(), 0uL)
    else -> UInt256(0uL, 0uL, 0uL, w0.takeHighestOneBit())
}

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [UInt256] number,
 * or zero, if this number is zero.
 */
public fun UInt256.takeLowestOneBit(): UInt256 = when {
    w0 != 0uL -> UInt256(0uL, 0uL, 0uL, w0.takeLowestOneBit())
    w1 != 0uL -> UInt256(0uL, 0uL, w1.takeLowestOneBit(), 0uL)
    w2 != 0uL -> UInt256(0uL, w2.takeLowestOneBit(), 0uL, 0uL)
    else -> UInt256(w3.takeLowestOneBit(), 0uL, 0uL, 0uL)
}

/**
 * Rotates the binary representation of this [UInt256] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [UInt256.SIZE_BITS] (256) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 256)`
 */
public fun UInt256.rotateLeft(bitCount: Int): UInt256 = (this shl bitCount) or (this shr -bitCount)

/**
 * Rotates the binary representation of this [UInt256] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [UInt256.SIZE_BITS] (256) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 256)`
 */
public fun UInt256.rotateRight(bitCount: Int): UInt256 = (this shr bitCount) or (this shl -bitCount)
