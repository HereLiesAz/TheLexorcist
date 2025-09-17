// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false // Updated Hilt plugin to 2.57.1
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
    id("com.palantir.git-version") version "4.0.0" apply false
    id("io.realm.kotlin") version "3.0.0" apply false // Added Realm plugin
    id("io.objectbox") version "4.3.1" apply false // Added ObjectBox plugin
}
