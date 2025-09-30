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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17
    }
    buildToolsVersion = "36.1.0 rc1"
    ndkVersion = "29.0.14033849 rc4"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")

    // Core litert library for runtime management
    implementation("com.google.ai.edge.litert:litert:2.0.2") // This is libs.litert

    // Standard TensorFlow Lite libraries - aligning with app module's version 2.14.0
    implementation("org.tensorflow:tensorflow-lite-api:2.14.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") // Add if you plan to use GPU delegate directly via TFLite APIs

}

kotlin {
    jvmToolchain(17)
}
