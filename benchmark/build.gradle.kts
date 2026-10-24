import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "com.appmattus.uint.benchmark.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
        }
    }
}

// Runs the JVM benchmark; pass -Pnative=false to force the portable implementation
val jvmBenchmark by tasks.registering(JavaExec::class) {
    val compilation = kotlin.jvm().compilations.getByName("main")
    classpath(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
    mainClass.set("com.appmattus.uint.benchmark.MainKt")
    systemProperty("com.appmattus.uint.native", providers.gradleProperty("native").getOrElse("true"))
}
