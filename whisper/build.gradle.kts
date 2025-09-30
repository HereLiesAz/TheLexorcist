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
    implementation(libs.litert) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
    }

    // Standard TensorFlow Lite libraries - Using version 2.17.0 from version catalog
    implementation(libs.tensorflow.lite.api)
    implementation(libs.tensorflow.lite)
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0") // Use consistent version if GPU delegate is needed

}

kotlin {
    jvmToolchain(17)
}
