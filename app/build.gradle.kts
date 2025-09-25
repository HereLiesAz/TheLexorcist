import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties() // Create a Properties object
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    id("com.google.devtools.ksp") // Changed from alias
    alias(libs.plugins.compose.compiler) // UPDATED from libs.plugins.kotlin.compose
    id("com.google.gms.google-services") // Added Google Services plugin
    id("kotlin-parcelize") // ADDED
}

// Add KSP configuration block
ksp {
    arg("dagger.validateTransitiveComponentDependencies", "ENABLED")
    arg("dagger.fullBindingGraphValidation", "ERROR") // You can also try "WARNING"
}

android {
    signingConfigs {
        maybeCreate("release").apply {
            storeFile = file("G://My Drive//az_apk_keystore.jks")
            storePassword = localProperties.getProperty("MY_KEYSTORE_PASSWORD") ?: System.getenv("MY_KEYSTORE_PASSWORD") ?: ""
            keyAlias = "key0"
            keyPassword = localProperties.getProperty("MY_KEY_PASSWORD") ?: System.getenv("MY_KEY_PASSWORD") ?: ""
        }
    }
    namespace = "com.hereliesaz.lexorcist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.lexorcist"
        minSdk = 26
        targetSdk = 36

        versionCode = 4
        versionName = "0.9.2"

        testInstrumentationRunner = "com.hereliesaz.lexorcist.HiltTestRunner"

        // Expose the API key to the app
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("API_KEY")}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release") // Explicitly assign signing config
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true // ADDED
        buildConfig = true
    }
    composeOptions {
    }
    packaging {
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/LICENSE-notice.md")
        resources.excludes.add("META-INF/NOTICE.md")
        // Exclude gRPC and Protobuf meta-inf files that can cause conflicts
        resources.excludes.add("META-INF/services/io.grpc.ManagedChannelProvider")
        resources.excludes.add("META-INF/grpc-all.versions")
        resources.excludes.add("META-INF/io.netty.versions.properties")
        // Netty native image properties
        resources.excludes.add("META-INF/native-image/io.netty/codec-http2/native-image.properties")
        resources.excludes.add("META-INF/native-image/io.netty/codec/native-image.properties")
        resources.excludes.add("META-INF/native-image/io.netty/common/native-image.properties")
        resources.excludes.add("META-INF/native-image/io.netty/handler/native-image.properties")
        resources.excludes.add("META-INF/native-image/io.netty/resolver/native-image.properties")
        resources.excludes.add("META-INF/native-image/io.netty/transport/native-image.properties")
        // Common protobuf schema files that can cause conflicts
        resources.excludes.add("google/protobuf/any.proto")
        resources.excludes.add("google/protobuf/api.proto")
        resources.excludes.add("google/protobuf/descriptor.proto")
        resources.excludes.add("google/protobuf/duration.proto")
        resources.excludes.add("google/protobuf/empty.proto")
        resources.excludes.add("google/protobuf/field_mask.proto")
        resources.excludes.add("google/protobuf/source_context.proto")
        resources.excludes.add("google/protobuf/struct.proto")
        resources.excludes.add("google/protobuf/timestamp.proto")
        resources.excludes.add("google/protobuf/type.proto")
        resources.excludes.add("google/protobuf/wrappers.proto")
        resources.excludes.add("META-INF/services/org.tensorflow.lite.TfLiteFlexDelegate")
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13599879 rc2"
}

dependencies {

    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5") {
            because("Align kotlin versions")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20") {
            because("Align kotlin versions")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20") {
            because("Align kotlin versions")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.20") {
            because("Align kotlin versions")
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.google.android.material)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.credentials) // Added AndroidX Credentials
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.ai) {
        exclude(group = "org.tensorflow")
    }
    implementation(libs.material3)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.room.ktx) // Explicitly use KTX version and direct coordinate

    // Core testing dependencies
    testImplementation(libs.junit) // JUnit 4
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlin.mockito.kotlin)
    testImplementation(libs.mockito.inline) // ADDED for static mocking

    // WorkManager Testing (version 2.10.4 matches your work-runtime-ktx)
    testImplementation(libs.androidx.work.testing)
    
    // TensorFlow Lite / AI Edge Runtime
    implementation(libs.litert) // com.google.ai.edge.litert:litert:2.0.2 (for runtime mgmt)
    implementation("org.tensorflow:tensorflow-lite:${libs.versions.tensorflowLite.get()}")
    implementation("org.tensorflow:tensorflow-lite-api:${libs.versions.tensorflowLite.get()}")
    // REMOVED: implementation("com.google.android.gms:play-services-tflite-java:16.4.0")
    // REMOVED: implementation("com.google.android.gms:play-services-tflite-support:16.4.0")
    // implementation("sh.calvin.reorderable:reorderable:0.9.6")

    testImplementation(libs.androidx.arch.core.testing) // For InstantTaskExecutorRule
    testImplementation(libs.kotlinx.coroutines.test) // For coroutines testing (runTest, TestDispatchers)
    testImplementation(libs.turbine) // For testing Kotlin Flows
    testImplementation(libs.mockwebserver) // For MockWebServer

    // AndroidX Test dependencies (androidTest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.mockk.android) // MockK for AndroidTest, if used there
    androidTestImplementation(libs.play.services.auth)

    implementation(libs.google.play.services.mlkit.text.recognition) {
        exclude(group = "org.tensorflow")
    }
    implementation(libs.play.services.base) // Added

    // Gson
    implementation(libs.google.code.gson)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process) // Added lifecycle-process
    implementation(libs.androidx.work.runtime.ktx) // This is 2.10.4
    implementation(libs.androidx.compose.runtime.livedata)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.ui) // CORRECTED ALIAS
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime.livedata)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    // iText and POI
    implementation(libs.itext7.core)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)
    implementation(libs.apache.poi.scratchpad)

    // Google APIs
    implementation(libs.google.api.client)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.api.services.script)
    implementation(libs.google.api.services.docs)

    implementation(libs.google.http.client.jackson2)
    implementation(libs.google.guava)
    implementation(libs.quickbirdstudios.opencv)
    implementation(libs.play.services.auth)

    // Mozilla Rhino for JavaScript execution
    implementation(libs.mozilla.rhino)
    implementation(libs.materialkolor)

    // Vico Charting Library
    implementation(libs.vico.compose)
    implementation(libs.vico.core)

    // JetLime Timeline Library
    implementation(libs.jetlime)

    // Dropbox SDK
    implementation(libs.dropbox.core.sdk)
    implementation(libs.dropbox.android.sdk)

    // Microsoft Graph SDK for OneDrive
    implementation(libs.microsoft.graph.core)
    implementation(libs.microsoft.graph)

    // Hilt
    implementation(libs.google.dagger.hilt.android)
    ksp(libs.google.dagger.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    testImplementation(libs.google.dagger.hilt.android.testing)
    kspTest(libs.google.dagger.hilt.compiler) // Corrected alias
    androidTestImplementation(libs.google.dagger.hilt.android.testing)
    kspAndroidTest(libs.google.dagger.hilt.compiler) // Corrected alias

    // Jetpack DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    // Room IS NOT ALLOWED IN THIS PROJECT!!!!!

    // Vosk for on-device speech-to-text
    implementation(libs.vosk.android) // UNCOMMENTED

    // Whisper for on-device speech-to-text
    implementation(project(":whisper"))
    // Explicit gRPC dependencies with consistent versions
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.core)
    implementation(libs.grpc.context)

    implementation(libs.aznavrail)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}
