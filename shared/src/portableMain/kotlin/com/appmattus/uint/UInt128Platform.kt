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

internal actual fun uint128Times(v1: UInt128, v2: UInt128): UInt128 = uint128TimesPortable(v1, v2)

internal actual fun uint128PlatformDivRem(dividend: UInt128, divisor: UInt128, remainder: Boolean): UInt128 =
    uint128DivRemPortable(dividend, divisor, remainder)
