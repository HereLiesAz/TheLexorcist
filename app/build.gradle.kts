import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize") // Added kotlin-parcelize plugin
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("com.diffplug.spotless")
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

        testInstrumentationRunner = "com.hereliesaz.lexorcist.HiltTestRunner"
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
    }
    packaging {
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
    implementation("androidx.compose.foundation:foundation:1.9.0")

    // Core testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.10")

    // AndroidX Test dependencies (androidTest)
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")


    // Gson
    implementation("com.google.code.gson:gson:2.13.1")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.0")

    // Jetpack Compose
    val composeBom = "2025.08.01"
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

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
    
    // Vico Charting Library
    implementation("com.patrykandpatrick.vico:compose:2.1.3")
    implementation("com.patrykandpatrick.vico:core:2.1.3")

    // JetLime Timeline Library
    implementation("io.github.pushpalroy:jetlime:4.0.0")
    implementation("io.github.pushpalroy:jetlime-android:4.0.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")
    testImplementation("com.google.dagger:hilt-android-testing:2.57.1")
    kspTest("com.google.dagger:hilt-android-compiler:2.57.1")


    // Room IS NOT ALLOWED IN THIS PROJECT!!!!!

    // Google Cloud Speech-to-Text
    implementation("com.google.cloud:google-cloud-speech:4.68.0")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}
