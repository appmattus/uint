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

import java.io.File

/**
 * JNI bridge to the platform's native 128-bit integer support, bundled for macOS only.
 *
 * Set the system property `com.appmattus.uint.native` to `false` to force the portable Kotlin implementation.
 */
internal object NativeUInt128 {

    private const val LIBRARY = "/com/appmattus/uint/native/libuint128.dylib"

    @JvmField
    val isAvailable: Boolean = load()

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun load(): Boolean {
        if (System.getProperty("com.appmattus.uint.native")?.toBoolean() == false) return false
        if (!System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)) return false
        return try {
            val resource = NativeUInt128::class.java.getResourceAsStream(LIBRARY) ?: return false
            val file = File.createTempFile("libuint128", ".dylib").apply { deleteOnExit() }
            resource.use { input -> file.outputStream().use { input.copyTo(it) } }
            System.load(file.absolutePath)
            true
        } catch (e: Throwable) {
            false
        }
    }

    /** Returns the most significant 64 bits of the 128-bit product of a and b. */
    @JvmStatic
    external fun mulHigh(aHi: Long, aLo: Long, bHi: Long, bLo: Long): Long

    /** Writes { upper, lower } of either the quotient or the remainder of a / b into out. */
    @JvmStatic
    external fun divRem(aHi: Long, aLo: Long, bHi: Long, bLo: Long, remainder: Boolean, out: LongArray)
}
