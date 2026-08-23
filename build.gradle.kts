// Top-level build file. Configuration common to every module lives here.
//
// Toolchain: JDK 21 everywhere (see `subprojects` block below and
// gradle.properties). Kotlin is held at the version KSP supports -- Hilt's
// annotation processor is the binding constraint, and KSP has no 2.4.x
// release yet, so the Kotlin plugin cannot move past 2.3.x until it does.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.palantir.git-version") version "5.0.0" apply false
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}
