
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Removed kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" // Added KSP plugin
    id("com.google.gms.google-services") // Add this line at the end of the plugins block

}

android {
    namespace = "com.hereliesaz.lexorcist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.lexorcist"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Explicitly set sourceInformation to true (or false if you don't need it for debug).
        // For debugging, it's generally desirable to have source information.
        //sourceInformation = true
        // You might also explicitly set the Kotlin compiler extension version here
        // if you encounter other compatibility issues, but for this error,
        // focusing on sourceInformation is enough for now.
        // For example: kotlinCompilerExtensionVersion = "1.5.11" // (matching your Compose BOM version)
    }
    packaging { // Changed from packagingOptions
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.runtime:runtime:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Room components
    val room_version = "2.8.0-rc01"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // Changed from kapt to ksp

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.0")

    // Jetpack Compose
    val compose_bom_version = "2025.08.00"
    implementation(platform("androidx.compose:compose-bom:$compose_bom_version"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.0")

    // iText and POI
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.0.0")
    implementation("com.google.api-client:google-api-client-android:2.8.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20250819-2.0.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")
    implementation("com.google.apis:google-api-services-script:v1-rev20250623-2.0.0")
    implementation("com.google.http-client:google-http-client-jackson2:2.0.0")
    implementation("com.github.HereLiesAz:AzNavRail:1.9")
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-analytics")

}

// Removed kapt block entirely
/*
kapt {
    arguments {
        arg("kapt.jvm.args", "-J--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED " +
                            "-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
    }
}
*/
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
