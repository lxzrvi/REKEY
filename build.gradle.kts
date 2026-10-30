buildscript {
    dependencies {
        // AGP supplies built-in Kotlin; this only upgrades its compiler/KGP classpath.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
