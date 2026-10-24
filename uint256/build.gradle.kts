import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id(libs.plugins.gradleMavenPublishPlugin.get().pluginId)
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
            baseName = "UInt256"
        }
    }

    // Everything except the JVM, which has intrinsics, shares the portable Kotlin implementation
    applyDefaultHierarchyTemplate {
        common {
            group("portable") {
                withJs()
                withWasmJs()
                withWasmWasi()
                withNative()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":uint128"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

