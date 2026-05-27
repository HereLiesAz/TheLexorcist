// Top-level build file where you can add configuration options common to all subprojects/modules.

// Note: Per protocol, checked for latest stable AGP version.
// Project is updated to the latest stable version (9.2.1) with the corresponding Gradle version (9.5).
// Hilt 2.59.2 requires AGP 9.0.0 or higher.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    id("com.palantir.git-version") version "5.0.0" apply false
    alias(libs.plugins.compose.compiler) apply false
}
