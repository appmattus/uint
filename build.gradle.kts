plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.gradleMavenPublishPlugin) apply false
}

subprojects {
    version = (System.getenv("GITHUB_REF") ?: System.getProperty("GITHUB_REF"))
        ?.replaceFirst("refs/tags/", "") ?: "unspecified"
}
