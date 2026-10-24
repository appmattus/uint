import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "com.appmattus.uint.benchmark.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":uint128"))
        }
    }
}

// Runs the JVM benchmark, forking a JVM per benchmark; pass -PjavaVersion=11 (or 17, 21, ...) to run it on a specific JDK
tasks.register<JavaExec>("jvmBenchmark") {
    val compilation = kotlin.jvm().compilations.getByName("main")
    classpath(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
    mainClass.set("com.appmattus.uint.benchmark.MainKt")
    providers.gradleProperty("javaVersion").orNull?.let { version ->
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(version)) })
    }
}
