# uint

[![CI status](https://github.com/appmattus/uint/workflows/Build/badge.svg)](https://github.com/appmattus/uint/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.appmattus.uint/uint128)](https://search.maven.org/search?q=g:com.appmattus.uint)

A Kotlin Multiplatform library providing 128-bit and 256-bit unsigned
integers, `UInt128` and `UInt256`, which behave like Kotlin's own `UInt` and
`ULong`.

The following operations are supported:

## Arithmetic

- `+`, `-`, `*`, `/`, `%`, `floorDiv`, `mod`, `++`, `--`, unary `-`
- `pow`, `isqrt`
- Operands of `UByte`, `UShort`, `UInt`, `ULong`, `UInt128` and `UInt256`
- Results wrap around on overflow, just like `ULong`

## Bitwise operations

- `and`, `or`, `xor`, `inv`, `shl`, `shr`
- `rotateLeft`, `rotateRight`
- `countOneBits`, `countLeadingZeroBits`, `countTrailingZeroBits`
- `takeHighestOneBit`, `takeLowestOneBit`

## Conversions

- To and from `Byte`, `Short`, `Int`, `Long`, `UByte`, `UShort`, `UInt` and
  `ULong`
- Between `UInt128` and `UInt256`
- To and from strings in any radix from 2 to 36

## Comparisons and ranges

- `<`, `<=`, `>`, `>=`, `==`, `minOf`, `maxOf`
- `..`, `until`, `step`

## Getting started

![badge][badge-android]
![badge][badge-ios]
![badge][badge-watchos]
![badge][badge-tvos]
![badge][badge-mac]
![badge][badge-linux]
![badge][badge-windows]
![badge][badge-jvm]
![badge][badge-js]
![badge][badge-wasm]

Include the following dependency in your *build.gradle.kts* file:

```kotlin
commonMain {
    implementation("com.appmattus.uint:uint128:<latest-version>")

    // Or for UInt256, which also includes UInt128
    implementation("com.appmattus.uint:uint256:<latest-version>")
}
```

Create values from their parts, from other integer types or from strings, then
use them as you would any other unsigned integer:

```kotlin
// Create a UInt128 from its upper and lower 64 bits, another integer or a string
val twoTo64 = UInt128(upper = 1uL, lower = 0uL)
val small = 12345u.toUInt128()
val max = "340282366920938463463374607431768211455".toUInt128()

// Arithmetic works with other UInt128s and with smaller unsigned integers
val sum = twoTo64 + small          // 18446744073709563961
val product = twoTo64 * 3uL        // 55340232221128654848
val remainder = max % 1_000_000uL  // 211455

// Results wrap around on overflow, just like ULong
max + 1u == UInt128.ZERO           // true

// Format and parse in any radix from 2 to 36
max.toString(16)                   // "ffffffffffffffffffffffffffffffff"
"ff".toUInt128(16)                 // 255

// Bit operations and maths
(UInt128.ONE shl 100).countLeadingZeroBits()  // 27
10u.toUInt128().pow(30)                       // 1000000000000000000000000000000
1_000_000u.toUInt128().isqrt()                // 1000
```

`UInt256` works the same way, and is made up of an upper and lower `UInt128`:

```kotlin
val twoTo128 = UInt256(upper = UInt128.ONE, lower = UInt128.ZERO)

twoTo128.upper                        // 1
twoTo128 * 3u                         // 1020847100762815390390123822295304634368
UInt256.MAX_VALUE.isqrt()             // 340282366920938463463374607431768211455
UInt128.MAX_VALUE.toUInt256() + 1u    // 340282366920938463463374607431768211456
```

On the JVM the libraries need Java 11 or later.

## Contributing

Please fork this repository and contribute back using [pull requests](https://github.com/appmattus/uint/pulls).

All contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed.

## License

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Copyright 2022-2026 Appmattus Limited

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

[badge-android]: http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat
[badge-ios]: http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat
[badge-js]: http://img.shields.io/badge/platform-js-F8DB5D.svg?style=flat
[badge-jvm]: http://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat
[badge-linux]: http://img.shields.io/badge/platform-linux-2D3F6C.svg?style=flat
[badge-windows]: http://img.shields.io/badge/platform-windows-4D76CD.svg?style=flat
[badge-mac]: http://img.shields.io/badge/platform-macos-111111.svg?style=flat
[badge-watchos]: http://img.shields.io/badge/platform-watchos-C0C0C0.svg?style=flat
[badge-tvos]: http://img.shields.io/badge/platform-tvos-808080.svg?style=flat
[badge-wasm]: https://img.shields.io/badge/platform-wasm-624FE8.svg?style=flat
