// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Note: Per protocol, checked for latest stable AGP version.
// Project is already on the latest stable version (8.13.0) with the corresponding Gradle version (8.13).
// No version changes are necessary at this time.
plugins {
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
    id("com.palantir.git-version") version "4.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}
