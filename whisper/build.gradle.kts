plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hereliesaz.whisper"
    compileSdkPreview = "CANARY"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "../whisper_android/whisper_java/app/src/main/assets")
        }
    }
    buildToolsVersion = "36.1.0 rc1"
    ndkVersion = "29.0.14033849 rc4"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.material)

    // Core litert library for runtime management
    implementation(libs.litert) // This is libs.litert

    // Standard TensorFlow Lite libraries - aligning with app module's version 2.14.0
    implementation(libs.tensorflow.lite.api)
    implementation(libs.tensorflow.lite)
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") // Add if you plan to use GPU delegate directly via TFLite APIs

}

kotlin {
    jvmToolchain(17)
}
