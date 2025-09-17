import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize") // Added kotlin-parcelize plugin
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services") // Added Google Services plugin
    id("com.palantir.git-version")
    id("io.realm.kotlin") // Added Realm plugin
    id("io.objectbox") // Added ObjectBox plugin
}

// Add KSP configuration block to exclude the Room KSP processor
ksp {
    arg("dagger.validateTransitiveComponentDependencies", "ENABLED")
    arg("dagger.fullBindingGraphValidation", "ERROR") // You can also try "WARNING"
    arg("ksp.excluded.processors", "androidx.room.compiler.processing.ksp.RoomKspProcessor") // Keep this
}

android {
    namespace = "com.hereliesaz.lexorcist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.lexorcist"
        minSdk = 26
        targetSdk = 36

        versionCode = 3
        versionName = "0.9.0"

        testInstrumentationRunner = "com.hereliesaz.lexorcist.HiltTestRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
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
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.google.android.material)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.credentials) // Added AndroidX Credentials
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.material3) // Explicitly use KTX version and direct coordinate

    // Core testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk.android)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlin.test.junit)

    // AndroidX Test dependencies (androidTest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.play.services.auth)

    implementation(libs.google.play.services.mlkit.text.recognition)
    implementation(libs.play.services.base) // Added


    // Gson
    implementation(libs.google.code.gson)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process) // Added lifecycle-process
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.runtime.livedata)

    // Jetpack Compose

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    // iText and POI
    implementation(libs.itext7.core)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)
    implementation(libs.apache.poi.scratchpad)

    // Google Sign-In (using the new Identity Services API)

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


    // Dropbox SDK
    implementation(libs.dropbox.core.sdk)
    implementation(libs.dropbox.android.sdk)

    // Microsoft Graph SDK for OneDrive
    implementation(libs.microsoft.graph)
    implementation(libs.msal)

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


    // Room IS NOT ALLOWED IN THIS PROJECT!!!!!

    // Realm Database
    implementation(libs.library.base) // Added Realm library

    // Google Cloud Speech-to-Text
    implementation(libs.google.cloud.speech)
    // Explicit gRPC dependencies with consistent versions
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.core)
    // implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.context)

    implementation(libs.aznavrail)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}
