import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Apple targets can only be compiled on a macOS host. Declaring them
// unconditionally makes the whole build unresolvable on Linux (and therefore
// on CI), so they are added only where they can actually be built. Everything
// in commonMain is still written to be Apple-clean; a developer on a Mac gets
// the LexorcistShared framework with no further configuration.
val isMacHost = org.gradle.internal.os.OperatingSystem.current().isMacOsX

// Compose Multiplatform derives the generated `Res` class package from the
// Gradle root project name, which here is "The Lexorcist". The space produces a
// class named `the lexorcist/shared/generated/resources/Res$string`, and D8
// rejects it: "Space characters in SimpleName ... are not allowed prior to DEX
// version 040". Compilation succeeds and only the dex step fails, so this shows
// up in a full assemble rather than at build time. Naming the package
// explicitly avoids depending on the project name at all.
compose.resources {
    packageOfResClass = "com.hereliesaz.lexorcist.shared.resources"
    publicResClass = false
}

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    androidLibrary {
        namespace = "com.hereliesaz.lexorcist.shared"
        compileSdk = 37
        minSdk = 26

        withHostTestBuilder {}
    }

    jvm()

    if (isMacHost) {
        listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "LexorcistShared"
                isStatic = true
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // `api` rather than `implementation`: the domain models and the
            // composables here expose Instant, Flow and Compose types in their
            // public signatures, so consumers need them on the compile
            // classpath to call this module at all.
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)

            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)

            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
