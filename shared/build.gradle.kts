import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js {
        browser()
        nodejs()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Tier 1
    // Apple macOS hosts only:
    macosArm64()
    iosSimulatorArm64()
    iosX64()

    // Tier 2
    linuxX64()
    linuxArm64()
    // Apple macOS hosts only:
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()
    iosArm64()

    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    // Apple macOS hosts only:
    watchosDeviceArm64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }

    // Platforms without native 128-bit integer support share the portable Kotlin implementation
    applyDefaultHierarchyTemplate {
        common {
            group("portable") {
                withJs()
                withWasmJs()
                withWasmWasi()
                withLinux()
                withMingw()
                withAndroidNative()
            }
        }
    }

    // Apple targets use clang's unsigned __int128, the same LLVM i128 type that backs Swift's UInt128
    targets.withType<KotlinNativeTarget>().matching { it.konanTarget.family.isAppleFamily }.configureEach {
        compilations.getByName("main").cinterops.create("uint128")
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

