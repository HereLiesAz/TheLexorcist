// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false // Add this line
    // id("com.palantir.git-version") version "4.0.0" apply false // Removed as it's not used by app module
}
