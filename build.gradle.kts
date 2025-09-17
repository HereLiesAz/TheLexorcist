// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.5.8" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.palantir.git-version") version "4.0.0" apply false
    id("io.realm.kotlin") version "1.11.0" apply false
    id("io.objectbox") version "4.3.1" apply false
}
