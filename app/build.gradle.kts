import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream // Added for executing Git commands

// Helper functions to get version from Git
fun getGitVersionName(): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "describe", "--tags", "--dirty", "--always")
            standardOutput = byteOut
            errorOutput = byteOut // Capture errors too, in case 'git describe' fails
            isIgnoreExitValue = true // Allow us to handle non-zero exit codes if needed
        }
        val output = byteOut.toString().trim()
        if (output.startsWith("fatal:") || output.isEmpty()) { // Check if git describe failed (e.g. no tags)
            "0.0.0-nogit" // Fallback if git describe fails or no tags
        } else {
            output
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "0.0.0-error" // Fallback in case of other errors
    }
}

fun getGitVersionCode(): Int {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = byteOut
        }
        byteOut.toString().trim().toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        1 // Fallback version code
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Removed kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" // Added KSP plugin
    // id("com.palantir.git-version") // Temporarily removed
}

android {
    namespace = "com.hereliesaz.lexorcist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.lexorcist"
        minSdk = 26
        targetSdk = 36
        versionCode = getGitVersionCode() // Updated
        versionName = getGitVersionName() // Updated

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
    implementation("androidx.navigation:navigation-runtime-ktx:2.9.3")
    implementation("androidx.compose.ui:ui:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.5")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Room components
    val room_version = "2.8.0-rc02"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // Changed from kapt to ksp

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.0")

    // Jetpack Compose
    val composeBom = "2025.08.01"
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.9.3")
    // iText and POI
    implementation("com.itextpdf:itext7-core:9.2.0")
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.apache.poi:poi-scratchpad:5.4.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.8.1")
    implementation("com.google.api-client:google-api-client-android:2.8.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20250819-2.0.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")
    implementation("com.google.apis:google-api-services-script:v1-rev20250623-2.0.0")
    implementation("com.google.apis:google-api-services-docs:v1-rev20250325-2.0.0")

    implementation("com.google.http-client:google-http-client-jackson2:2.0.0")
    implementation("com.github.HereLiesAz:AzNavRail:1.9")
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.guava:guava:33.4.8-android")
    implementation("com.quickbirdstudios:opencv:4.5.3")

    // Mozilla Rhino for JavaScript execution
    implementation("org.mozilla:rhino:1.8.0")
    implementation("com.materialkolor:material-kolor:3.0.1")
    implementation("com.patrykandpatrick.vico:compose:2.1.3")
    implementation("io.github.pushpalroy:jetlime:4.0.0")
    implementation("io.github.pushpalroy:jetlime-android:4.0.0")
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
