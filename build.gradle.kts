plugins {
    //trick: for the same plugin versions in all sub-modules
    kotlin("multiplatform").version("2.4.21").apply(false)
    id("com.vanniktech.maven.publish").version("0.37.0").apply(false)
}

subprojects {
    version = (System.getenv("GITHUB_REF") ?: System.getProperty("GITHUB_REF"))
        ?.replaceFirst("refs/tags/", "") ?: "unspecified"
}
