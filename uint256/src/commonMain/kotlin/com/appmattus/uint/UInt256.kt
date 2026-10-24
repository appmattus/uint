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

/** Represents a 256-bit unsigned integer. */
@Suppress("TooManyFunctions")
public class UInt256 @PublishedApi internal constructor(
    /** The most significant 64-bit word. */
    @PublishedApi internal val w3: ULong,
    @PublishedApi internal val w2: ULong,
    @PublishedApi internal val w1: ULong,
    /** The least significant 64-bit word. */
    @PublishedApi internal val w0: ULong
) : Comparable<UInt256> {

    /** Creates a [UInt256] from its most significant ([upper]) and least significant ([lower]) 128 bits. */
    public constructor(upper: UInt128, lower: UInt128) : this(upper.upper, upper.lower, lower.upper, lower.lower)

    /** The most significant 128 bits. */
    public val upper: UInt128 get() = UInt128(w3, w2)

    /** The least significant 128 bits. */
    public val lower: UInt128 get() = UInt128(w1, w0)

    public companion object {

        /** A constant holding the minimum value an instance of [UInt256] can have. */
        public val MIN_VALUE: UInt256 = UInt256(0uL, 0uL, 0uL, 0uL)

        /** A constant holding the maximum value an instance of [UInt256] can have. */
        public val MAX_VALUE: UInt256 = UInt256(ULong.MAX_VALUE, ULong.MAX_VALUE, ULong.MAX_VALUE, ULong.MAX_VALUE)

        /** The number of bytes used to represent an instance of [UInt256] in a binary form. */
        public const val SIZE_BYTES: Int = ULong.SIZE_BYTES * 4

        /** The number of bits used to represent an instance of [UInt256] in a binary form. */
        public const val SIZE_BITS: Int = ULong.SIZE_BITS * 4

        /** The maximum radix available for conversion to and from strings. */
        public const val MAX_RADIX: Int = 36

        /** The minimum radix available for conversion to and from strings. */
        public const val MIN_RADIX: Int = 2

        /** The [UInt256] constant zero. */
        public val ZERO: UInt256 = UInt256(0uL, 0uL, 0uL, 0uL)

        /** The [UInt256] constant one. */
        public val ONE: UInt256 = UInt256(0uL, 0uL, 0uL, 1uL)
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: UByte): Int = compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: UShort): Int = compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: UInt): Int = compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: ULong): Int = uint256CompareULong(this, other)

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public inline operator fun compareTo(other: UInt128): Int = compareTo(other.toUInt256())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    public override fun compareTo(other: UInt256): Int = uint256Compare(this, other)

    /** Adds the [other] value to this value. */
    public inline operator fun plus(other: UByte): UInt256 = plus(other.toULong())

    /** Adds the [other] value to this value. */
    public inline operator fun plus(other: UShort): UInt256 = plus(other.toULong())

    /** Adds the [other] value to this value. */
    public inline operator fun plus(other: UInt): UInt256 = plus(other.toULong())

    /** Adds the [other] value to this value. */
    public inline operator fun plus(other: ULong): UInt256 = uint256PlusULong(this, other)

    /** Adds the [other] value to this value. */
    public inline operator fun plus(other: UInt128): UInt256 = plus(other.toUInt256())

    /** Adds the [other] value to this value. */
    public operator fun plus(other: UInt256): UInt256 = uint256Plus(this, other)

    /** Subtracts the [other] value from this value. */
    public inline operator fun minus(other: UByte): UInt256 = minus(other.toULong())

    /** Subtracts the [other] value from this value. */
    public inline operator fun minus(other: UShort): UInt256 = minus(other.toULong())

    /** Subtracts the [other] value from this value. */
    public inline operator fun minus(other: UInt): UInt256 = minus(other.toULong())

    /** Subtracts the [other] value from this value. */
    public inline operator fun minus(other: ULong): UInt256 = uint256MinusULong(this, other)

    /** Subtracts the [other] value from this value. */
    public inline operator fun minus(other: UInt128): UInt256 = minus(other.toUInt256())

    /** Subtracts the [other] value from this value. */
    public operator fun minus(other: UInt256): UInt256 = uint256Minus(this, other)

    /** Multiplies this value by the [other] value. */
    public inline operator fun times(other: UByte): UInt256 = times(other.toULong())

    /** Multiplies this value by the [other] value. */
    public inline operator fun times(other: UShort): UInt256 = times(other.toULong())

    /** Multiplies this value by the [other] value. */
    public inline operator fun times(other: UInt): UInt256 = times(other.toULong())

    /** Multiplies this value by the [other] value. */
    public inline operator fun times(other: ULong): UInt256 = uint256TimesULong(this, other)

    /** Multiplies this value by the [other] value. */
    public inline operator fun times(other: UInt128): UInt256 = times(other.toUInt256())

    /** Multiplies this value by the [other] value. */
    public operator fun times(other: UInt256): UInt256 = uint256Times(this, other)

    /** Divides this value by the [other] value, truncating the result to an integer that is closer to zero. */
    public inline operator fun div(other: UByte): UInt256 = div(other.toULong())

    /** Divides this value by the [other] value, truncating the result to an integer that is closer to zero. */
    public inline operator fun div(other: UShort): UInt256 = div(other.toULong())

    /** Divides this value by the [other] value, truncating the result to an integer that is closer to zero. */
    public inline operator fun div(other: UInt): UInt256 = div(other.toULong())

    /** Divides this value by the [other] value, truncating the result to an integer that is closer to zero. */
    public inline operator fun div(other: ULong): UInt256 = uint256DivULong(this, other)

    /** Divides this value by the [other] value, truncating the result to an integer that is closer to zero. */
    public inline operator fun div(other: UInt128): UInt256 = div(other.toUInt256())

    /** Divides this value by the [other] value, truncating the result to an integer that is closer to zero. */
    public operator fun div(other: UInt256): UInt256 = uint256Div(this, other)

    /**
     * Calculates the remainder of truncating division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     */
    public inline operator fun rem(other: UByte): UInt256 = rem(other.toULong())

    /**
     * Calculates the remainder of truncating division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     */
    public inline operator fun rem(other: UShort): UInt256 = rem(other.toULong())

    /**
     * Calculates the remainder of truncating division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     */
    public inline operator fun rem(other: UInt): UInt256 = rem(other.toULong())

    /**
     * Calculates the remainder of truncating division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     */
    public inline operator fun rem(other: ULong): UInt256 = UInt256(0uL, 0uL, 0uL, uint256RemULong(this, other))

    /**
     * Calculates the remainder of truncating division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     */
    public inline operator fun rem(other: UInt128): UInt256 = rem(other.toUInt256())

    /**
     * Calculates the remainder of truncating division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     */
    public operator fun rem(other: UInt256): UInt256 = uint256Rem(this, other)

    /**
     * Divides this value by the [other] value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    public inline fun floorDiv(other: UByte): UInt256 = div(other)

    /**
     * Divides this value by the [other] value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    public inline fun floorDiv(other: UShort): UInt256 = div(other)

    /**
     * Divides this value by the [other] value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    public inline fun floorDiv(other: UInt): UInt256 = div(other)

    /**
     * Divides this value by the [other] value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    public inline fun floorDiv(other: ULong): UInt256 = div(other)

    /**
     * Divides this value by the [other] value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    public inline fun floorDiv(other: UInt128): UInt256 = div(other)

    /**
     * Divides this value by the [other] value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    public inline fun floorDiv(other: UInt256): UInt256 = div(other)

    /**
     * Calculates the remainder of flooring division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    public inline fun mod(other: UByte): UByte = uint256RemULong(this, other.toULong()).toUByte()

    /**
     * Calculates the remainder of flooring division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    public inline fun mod(other: UShort): UShort = uint256RemULong(this, other.toULong()).toUShort()

    /**
     * Calculates the remainder of flooring division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    public inline fun mod(other: UInt): UInt = uint256RemULong(this, other.toULong()).toUInt()

    /**
     * Calculates the remainder of flooring division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    public inline fun mod(other: ULong): ULong = uint256RemULong(this, other)

    /**
     * Calculates the remainder of flooring division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    public inline fun mod(other: UInt128): UInt128 = rem(other).toUInt128()

    /**
     * Calculates the remainder of flooring division of this value by the [other] value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    public inline fun mod(other: UInt256): UInt256 = rem(other)

    /** Returns this value incremented by one. */
    public operator fun inc(): UInt256 = this + ONE

    /** Returns this value decremented by one. */
    public operator fun dec(): UInt256 = this - ONE

    /** Creates a range from this value to the specified [other] value. */
    public inline operator fun rangeTo(other: UInt256): UInt256Range = UInt256Range(this, other)

    /**
     * Shifts this value left by the [bitCount] number of bits.
     *
     * Note that only the eight lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..255`.
     */
    public infix fun shl(bitCount: Int): UInt256 = uint256Shl(this, bitCount)

    /**
     * Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros.
     *
     * Note that only the eight lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..255`.
     */
    public infix fun shr(bitCount: Int): UInt256 = uint256Shr(this, bitCount)

    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: UInt256): UInt256 =
        UInt256(w3 and other.w3, w2 and other.w2, w1 and other.w1, w0 and other.w0)

    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: UInt256): UInt256 =
        UInt256(w3 or other.w3, w2 or other.w2, w1 or other.w1, w0 or other.w0)

    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: UInt256): UInt256 =
        UInt256(w3 xor other.w3, w2 xor other.w2, w1 xor other.w1, w0 xor other.w0)

    /** Inverts the bits in this value. */
    public fun inv(): UInt256 = UInt256(w3.inv(), w2.inv(), w1.inv(), w0.inv())

    /** Returns this value. */
    public operator fun unaryPlus(): UInt256 = this

    /** Returns the negative of this value. */
    public operator fun unaryMinus(): UInt256 = ZERO - this

    /**
     * Converts this [UInt256] value to [Byte].
     *
     * If this value is less than or equal to [Byte.MAX_VALUE], the resulting [Byte] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [Byte] value is represented by the least significant 8 bits of this [UInt256] value.
     * Note that the resulting [Byte] value may be negative.
     */
    public inline fun toByte(): Byte = w0.toByte()

    /**
     * Converts this [UInt256] value to [Short].
     *
     * If this value is less than or equal to [Short.MAX_VALUE], the resulting [Short] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [Short] value is represented by the least significant 16 bits of this [UInt256] value.
     * Note that the resulting [Short] value may be negative.
     */
    public inline fun toShort(): Short = w0.toShort()

    /**
     * Converts this [UInt256] value to [Int].
     *
     * If this value is less than or equal to [Int.MAX_VALUE], the resulting [Int] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [Int] value is represented by the least significant 32 bits of this [UInt256] value.
     * Note that the resulting [Int] value may be negative.
     */
    public inline fun toInt(): Int = w0.toInt()

    /**
     * Converts this [UInt256] value to [Long].
     *
     * If this value is less than or equal to [Long.MAX_VALUE], the resulting [Long] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [Long] value is represented by the least significant 64 bits of this [UInt256] value.
     * Note that the resulting [Long] value may be negative.
     */
    public inline fun toLong(): Long = w0.toLong()

    /**
     * Converts this [UInt256] value to [UByte].
     *
     * If this value is less than or equal to [UByte.MAX_VALUE], the resulting [UByte] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [UByte] value is represented by the least significant 8 bits of this [UInt256] value.
     */
    public inline fun toUByte(): UByte = w0.toUByte()

    /**
     * Converts this [UInt256] value to [UShort].
     *
     * If this value is less than or equal to [UShort.MAX_VALUE], the resulting [UShort] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [UShort] value is represented by the least significant 16 bits of this [UInt256] value.
     */
    public inline fun toUShort(): UShort = w0.toUShort()

    /**
     * Converts this [UInt256] value to [UInt].
     *
     * If this value is less than or equal to [UInt.MAX_VALUE], the resulting [UInt] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [UInt] value is represented by the least significant 32 bits of this [UInt256] value.
     */
    public inline fun toUInt(): UInt = w0.toUInt()

    /**
     * Converts this [UInt256] value to [ULong].
     *
     * If this value is less than or equal to [ULong.MAX_VALUE], the resulting [ULong] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [ULong] value is represented by the least significant 64 bits of this [UInt256] value.
     */
    public inline fun toULong(): ULong = w0

    /**
     * Converts this [UInt256] value to [UInt128].
     *
     * If this value is less than or equal to [UInt128.MAX_VALUE], the resulting [UInt128] value represents
     * the same numerical value as this [UInt256].
     *
     * The resulting [UInt128] value is represented by the least significant 128 bits of this [UInt256] value.
     */
    public inline fun toUInt128(): UInt128 = UInt128(w1, w0)

    /** Returns this value. */
    public inline fun toUInt256(): UInt256 = this

    override fun equals(other: Any?): Boolean {
        // Note that this needs to be consistent with compareTo
        if (this === other) {
            return true
        }
        if (other is UInt256) {
            return w3 == other.w3 && w2 == other.w2 && w1 == other.w1 && w0 == other.w0
        }
        return false
    }

    override fun hashCode(): Int = 31 * (31 * (31 * w3.hashCode() + w2.hashCode()) + w1.hashCode()) + w0.hashCode()

    override fun toString(): String = toString(10)
}

/**
 * Converts this [UByte] value to [UInt256].
 *
 * The resulting [UInt256] value represents the same numerical value as this [UByte].
 *
 * The least significant 8 bits of the resulting [UInt256] value are the same as the bits of this [UByte] value,
 * whereas the most significant 248 bits are filled with zeros.
 */
public inline fun UByte.toUInt256(): UInt256 = UInt256(0uL, 0uL, 0uL, toULong())

/**
 * Converts this [UShort] value to [UInt256].
 *
 * The resulting [UInt256] value represents the same numerical value as this [UShort].
 *
 * The least significant 16 bits of the resulting [UInt256] value are the same as the bits of this [UShort] value,
 * whereas the most significant 240 bits are filled with zeros.
 */
public inline fun UShort.toUInt256(): UInt256 = UInt256(0uL, 0uL, 0uL, toULong())

/**
 * Converts this [UInt] value to [UInt256].
 *
 * The resulting [UInt256] value represents the same numerical value as this [UInt].
 *
 * The least significant 32 bits of the resulting [UInt256] value are the same as the bits of this [UInt] value,
 * whereas the most significant 224 bits are filled with zeros.
 */
public inline fun UInt.toUInt256(): UInt256 = UInt256(0uL, 0uL, 0uL, toULong())

/**
 * Converts this [ULong] value to [UInt256].
 *
 * The resulting [UInt256] value represents the same numerical value as this [ULong].
 *
 * The least significant 64 bits of the resulting [UInt256] value are the same as the bits of this [ULong] value,
 * whereas the most significant 192 bits are filled with zeros.
 */
public inline fun ULong.toUInt256(): UInt256 = UInt256(0uL, 0uL, 0uL, this)

/**
 * Converts this [UInt128] value to [UInt256].
 *
 * The resulting [UInt256] value represents the same numerical value as this [UInt128].
 *
 * The least significant 128 bits of the resulting [UInt256] value are the same as the bits of this [UInt128] value,
 * whereas the most significant 128 bits are filled with zeros.
 */
public inline fun UInt128.toUInt256(): UInt256 = UInt256(0uL, 0uL, upper, lower)

/**
 * Converts this [Byte] value to [UInt256].
 *
 * If this value is positive, the resulting [UInt256] value represents the same numerical value as this [Byte].
 *
 * The least significant 8 bits of the resulting [UInt256] value are the same as the bits of this [Byte] value,
 * whereas the most significant 248 bits are filled with the sign bit of this value.
 */
public inline fun Byte.toUInt256(): UInt256 {
    val extension = if (this < 0) ULong.MAX_VALUE else 0uL
    return UInt256(extension, extension, extension, toULong())
}

/**
 * Converts this [Short] value to [UInt256].
 *
 * If this value is positive, the resulting [UInt256] value represents the same numerical value as this [Short].
 *
 * The least significant 16 bits of the resulting [UInt256] value are the same as the bits of this [Short] value,
 * whereas the most significant 240 bits are filled with the sign bit of this value.
 */
public inline fun Short.toUInt256(): UInt256 {
    val extension = if (this < 0) ULong.MAX_VALUE else 0uL
    return UInt256(extension, extension, extension, toULong())
}

/**
 * Converts this [Int] value to [UInt256].
 *
 * If this value is positive, the resulting [UInt256] value represents the same numerical value as this [Int].
 *
 * The least significant 32 bits of the resulting [UInt256] value are the same as the bits of this [Int] value,
 * whereas the most significant 224 bits are filled with the sign bit of this value.
 */
public inline fun Int.toUInt256(): UInt256 {
    val extension = if (this < 0) ULong.MAX_VALUE else 0uL
    return UInt256(extension, extension, extension, toULong())
}

/**
 * Converts this [Long] value to [UInt256].
 *
 * If this value is positive, the resulting [UInt256] value represents the same numerical value as this [Long].
 *
 * The least significant 64 bits of the resulting [UInt256] value are the same as the bits of this [Long] value,
 * whereas the most significant 192 bits are filled with the sign bit of this value.
 */
public inline fun Long.toUInt256(): UInt256 {
    val extension = if (this < 0) ULong.MAX_VALUE else 0uL
    return UInt256(extension, extension, extension, toULong())
}
