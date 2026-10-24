plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.kotlin.multiplatform.library").version("9.4.1").apply(false)
    kotlin("multiplatform").version("2.4.21").apply(false)
}
