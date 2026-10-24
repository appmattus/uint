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

import com.appmattus.uint.UInt128.Companion.ONE

/**
 * Raises this value to the integer power [n]
 */
public fun UInt128.pow(n: Int): UInt128 {
    var exp = n
    if (exp < 0) {
        throw IllegalArgumentException("exp must be >= 0")
    }

    var result = ONE
    var base: UInt128 = this
    while (exp != 0) {
        if (exp and 1 != 0) {
            result *= base
        }
        base *= base
        exp = exp ushr 1
    }
    return result
}

/**
 * Get the floor value of the exact square root of this
 */
public fun UInt128.isqrt(): UInt128 = UInt128(0uL, uint128Isqrt(this))
