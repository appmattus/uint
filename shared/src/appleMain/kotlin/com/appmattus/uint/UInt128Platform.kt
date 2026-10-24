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

import com.appmattus.uint.cinterop.uint128_divrem
import com.appmattus.uint.cinterop.uint128_native_available
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
private val nativeAvailable: Boolean = uint128_native_available() != 0

// The cinterop call overhead outweighs the gain from a native multiply, measured as ~15% slower than portable Kotlin
internal actual fun uint64MultiplyHigh(x: ULong, y: ULong): ULong = uint64MultiplyHighPortable(x, y)

@OptIn(ExperimentalForeignApi::class)
internal actual fun uint128PlatformDivRem(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128 {
    if (!nativeAvailable) return uint128DivRemPortable(dividend, divisor, remainder)
    val out = ULongArray(2)
    out.usePinned {
        uint128_divrem(
            dividend.upper, dividend.lower, divisor.upper, divisor.lower, if (remainder) 1 else 0, it.addressOf(0)
        )
    }
    return UInt128(out[0], out[1])
}

internal actual fun uint64Divide(dividend: ULong, divisor: ULong): ULong = dividend / divisor

internal actual fun uint64Remainder(dividend: ULong, divisor: ULong): ULong = dividend % divisor
