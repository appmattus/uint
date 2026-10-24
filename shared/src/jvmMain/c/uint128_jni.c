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

#include <jni.h>
#include <stdint.h>

#define UINT128(hi, lo) (((unsigned __int128) (uint64_t) (hi) << 64) | (uint64_t) (lo))

JNIEXPORT jlong JNICALL Java_com_appmattus_uint_NativeUInt128_mulHigh(
    JNIEnv *env, jclass cls, jlong a_hi, jlong a_lo, jlong b_hi, jlong b_lo) {
    return (jlong) (uint64_t) ((UINT128(a_hi, a_lo) * UINT128(b_hi, b_lo)) >> 64);
}

JNIEXPORT void JNICALL Java_com_appmattus_uint_NativeUInt128_divMod(
    JNIEnv *env, jclass cls, jlong a_hi, jlong a_lo, jlong b_hi, jlong b_lo, jlongArray out) {
    unsigned __int128 a = UINT128(a_hi, a_lo);
    unsigned __int128 b = UINT128(b_hi, b_lo);
    unsigned __int128 q = a / b;
    unsigned __int128 r = a - q * b;
    jlong result[4] = {
        (jlong) (uint64_t) (q >> 64), (jlong) (uint64_t) q, (jlong) (uint64_t) (r >> 64), (jlong) (uint64_t) r
    };
    (*env)->SetLongArrayRegion(env, out, 0, 4, result);
}
