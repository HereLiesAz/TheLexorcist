pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
    }
}

rootProject.name = "The Lexorcist"

// :shared holds the Kotlin/Compose Multiplatform core -- domain models, the
// evidence pipeline contracts and the cross-platform UI. :app is the Android
// entry point and hosts the platform integrations (Google APIs, POI, ML Kit,
// Rhino, Vosk, WorkManager) that cannot be expressed in common code.
include(":shared")
include(":app")
