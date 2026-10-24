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
 * A range of values of type [UInt256].
 */
public class UInt256Range(start: UInt256, endInclusive: UInt256) : UInt256Progression(start, endInclusive, 1), ClosedRange<UInt256> {
    override val start: UInt256 get() = first
    override val endInclusive: UInt256 get() = last

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun contains(value: UInt256): Boolean = first <= value && value <= last

    /**
     * Checks if the range is empty.

     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is UInt256Range && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * first.hashCode() + last.hashCode())

    override fun toString(): String = "$first..$last"

    public companion object {
        /** An empty range of values of type [UInt256]. */
        public val EMPTY: UInt256Range = UInt256Range(UInt256.MAX_VALUE, UInt256.MIN_VALUE)
    }
}

/**
 * A progression of values of type [UInt256].
 */
public open class UInt256Progression internal constructor(
    start: UInt256,
    endInclusive: UInt256,
    step: Long
) : Iterable<UInt256> {
    init {
        if (step == 0L) throw IllegalArgumentException("Step must be non-zero.")
        if (step == Long.MIN_VALUE) throw IllegalArgumentException("Step must be greater than Long.MIN_VALUE to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: UInt256 = start

    /**
     * The last element in the progression.
     */
    public val last: UInt256 = getUInt256ProgressionLastElement(start, endInclusive, step)

    /**
     * The step of the progression.
     */
    @Suppress("CanBePrimaryConstructorProperty")
    public val step: Long = step

    final override fun iterator(): Iterator<UInt256> = UInt256ProgressionIterator(first, last, step)

    /**
     * Checks if the progression is empty.

     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is UInt256Progression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int = if (isEmpty()) {
        -1
    } else {
        (31 * (31 * first.hashCode() + last.hashCode()) + (step xor (step ushr 32)).toInt())
    }

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    public companion object {
        /**
         * Creates UInt256Progression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `Long.MIN_VALUE` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: UInt256, rangeEnd: UInt256, step: Long): UInt256Progression =
            UInt256Progression(rangeStart, rangeEnd, step)
    }
}

/**
 * An iterator over a progression of values of type [UInt256].
 * @property step the number by which the value is incremented on each step.
 */
@SinceKotlin("1.3")
@Suppress("DEPRECATION_ERROR")
private class UInt256ProgressionIterator(first: UInt256, last: UInt256, step: Long) : UInt256Iterator() {
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private val step = step.toUInt256() // use 2-complement math for negative steps
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun nextUInt256(): UInt256 {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw NoSuchElementException()
            hasNext = false
        } else {
            next += step
        }
        return value
    }
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to `this` value, then the returned range is empty.
 */
public infix fun UInt256.until(to: UInt256): UInt256Range {
    if (to <= UInt256.MIN_VALUE) return UInt256Range.EMPTY
    return this..(to - 1u)
}

/**
 * Returns a progression that goes over the same range with the given step.
 */
public infix fun UInt256Progression.step(step: Long): UInt256Progression {
    if (step <= 0) throw IllegalArgumentException("Step must be positive, was: $step.")
    return UInt256Progression.fromClosedRange(first, last, if (this.step > 0) step else -step)
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
 * [step].
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition:
 *
 * - either `step > 0` and `start <= end`,
 * - or `step < 0` and `start >= end`.
 *
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param step increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun getUInt256ProgressionLastElement(start: UInt256, end: UInt256, step: Long): UInt256 = when {
    step > 0 -> if (start >= end) end else end - uint256DifferenceModulo(end, start, step.toUInt256())
    step < 0 -> if (start <= end) end else end + uint256DifferenceModulo(start, end, (-step).toUInt256())
    else -> throw IllegalArgumentException("Step is zero.")
}

private fun uint256DifferenceModulo(a: UInt256, b: UInt256, c: UInt256): UInt256 {
    val ac = a % c
    val bc = b % c
    return if (ac >= bc) ac - bc else ac - bc + c
}
