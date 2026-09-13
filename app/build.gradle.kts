import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties


val myLocalProperties = Properties() // Create a Properties object
val myLocalPropertiesFile = rootProject.file("local.properties")

if (myLocalPropertiesFile.exists()) {
    myLocalPropertiesFile.inputStream().use { input ->
        myLocalProperties.load(input)
    }
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler) // UPDATED from libs.plugins.kotlin.compose
    id("com.google.gms.google-services") // Added Google Services plugin
    id("kotlin-parcelize") // ADDED
}
// Load version properties
val versionPropsFile = project.rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

// Load local properties
val localProperties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

var currentVersionCode = versionProps.getProperty("versionBuild", "1").toInt()

// versionCode.
//
// An explicitly supplied -PversionBuild always wins. build.yml passes the git
// commit count, which is deterministic for a given commit; nothing about the
// build then depends on mutable state in the working tree.
//
// Failing that, a local release build auto-increments the tracked
// version.properties, which is the behaviour this project has always had.
//
// Two guards on that auto-increment:
//
//   * Never in CI. The task-name test below matches ANY task containing
//     "Release" -- including minifyReleaseWithR8, which the CI workflow runs on
//     every push and pull request to catch R8 failures. Without this guard,
//     every CI run mutates a tracked file, and the same commit produces a
//     different versionCode on every build.
//   * Only for tasks that actually produce a release artifact. Checking,
//     linting or shrinking a release variant is not a release, and should not
//     burn a version number.
val explicitVersionBuild = (project.findProperty("versionBuild") as String?)?.toIntOrNull()

val producesReleaseArtifact = gradle.startParameter.taskNames.any { task ->
    val name = task.substringAfterLast(':')
    name.startsWith("assemble", ignoreCase = true) ||
        name.startsWith("bundle", ignoreCase = true) ||
        name.startsWith("package", ignoreCase = true) ||
        name.startsWith("publish", ignoreCase = true) ||
        name.equals("build", ignoreCase = true)
} && gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

val runningInCi = System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null

when {
    explicitVersionBuild != null -> currentVersionCode = explicitVersionBuild
    producesReleaseArtifact && !runningInCi -> {
        currentVersionCode++
        versionProps.setProperty("versionBuild", currentVersionCode.toString())
        versionPropsFile.outputStream().use {
            versionProps.store(it, "Auto-incremented by release build")
        }
    }
}

val verMajor = versionProps.getProperty("versionMajor", "1")
val verMinor = versionProps.getProperty("versionMinor", "0")
val verPatch = versionProps.getProperty("versionPatch", "0")
val currentVersionName = "$verMajor.$verMinor.$verPatch"

// Add KSP configuration block
ksp {
    arg("dagger.validateTransitiveComponentDependencies", "ENABLED")
    arg("dagger.fullBindingGraphValidation", "ERROR") // You can also try "WARNING"
}

android {
    signingConfigs {
        maybeCreate("release").apply {
            storeFile = myLocalProperties.getProperty("MY_KEYSTORE_FILE")?.let { file(it) }
            storePassword = myLocalProperties.getProperty("MY_KEYSTORE_PASSWORD") ?: System.getenv("MY_KEYSTORE_PASSWORD") ?: ""
            keyAlias = "key0"
            keyPassword = myLocalProperties.getProperty("MY_KEY_PASSWORD") ?: System.getenv("MY_KEY_PASSWORD") ?: ""
        }
    }
    namespace = "com.hereliesaz.lexorcist"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hereliesaz.lexorcist"
        minSdk = 26
        targetSdk = 37

        versionCode = 4
        versionName = "0.9.2"

        testInstrumentationRunner = "com.hereliesaz.lexorcist.HiltTestRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
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
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

/*
 * The Apache HTTP transport is unused: GoogleApiService and GmailService both
 * construct NetHttpTransport. It arrives anyway through google-api-client and
 * google-http-client, dragging in Apache HttpClient 4.5.x and HttpCore 4.4.x.
 * 4.5.14 is the final release of that line, so the advisories open against it
 * cannot be closed by a version bump -- only by not shipping it.
 *
 * R8 is told not to warn about the resulting absent references; nothing in the
 * app reaches them, and the R8 step in CI would fail on the warnings otherwise.
 */
configurations.configureEach {
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
}

dependencies {

    constraints {
        implementation(libs.kotlinx.collections.immutable) {
            because("Align kotlin versions")
        }
        implementation(libs.kotlin.stdlib) {
            because("Align kotlin versions")
        }
        implementation(libs.kotlin.stdlib.jdk8) {
            because("Align kotlin versions")
        }
        implementation(libs.kotlin.stdlib.jdk7) {
            because("Align kotlin versions")
        }

        // --- Security: force-upgrade vulnerable transitive dependencies to patched ---
        // releases (Dependabot alerts). These are constraints, so they are no-ops for any
        // coordinate not actually present in the resolved graph. Dependabot remains the
        // source of truth for newer CVEs published after these pins.
        implementation("org.bouncycastle:bcprov-jdk18on:1.86") { because("CVE: timing channel / resource consumption") }
        implementation("org.bouncycastle:bcpkix-jdk18on:1.86") { because("CVE: BouncyCastle (MSAL/Graph, iText)") }
        implementation("org.bouncycastle:bcutil-jdk18on:1.86") { because("CVE: BouncyCastle (MSAL/Graph)") }
        implementation("org.bouncycastle:bcpg-jdk18on:1.86") { because("CVE: BouncyCastle uncontrolled resource consumption") }
        implementation("org.bitbucket.b_c:jose4j:0.9.7") { because("CVE: jose4j DoS via compressed JWE (MSAL)") }
        implementation("org.jdom:jdom2:2.0.6.1") { because("CVE-2021-33813: JDOM XXE (Apache POI)") }
        implementation("org.apache.commons:commons-lang3:3.20.0") { because("CVE: commons-lang3 uncontrolled recursion (POI)") }
        implementation("org.apache.commons:commons-compress:1.27.1") { because("CVE: commons-compress Pack200 OOM (POI)") }
        implementation("com.fasterxml.jackson.core:jackson-core:2.18.8") { because("CVE: jackson-core async parser DoS") }
        implementation("com.fasterxml.jackson.core:jackson-databind:2.18.8") { because("CVE: align jackson-databind with patched core") }
        implementation("io.netty:netty-codec-http2:4.2.18.Final") { because("CVE: Netty HTTP/2 DoS family") }
        implementation("io.netty:netty-codec-http:4.2.18.Final") { because("CVE: Netty HTTP request smuggling / decompression") }
        implementation("io.netty:netty-codec:4.2.18.Final") { because("CVE: Netty codec resource exhaustion / zip bomb") }
        implementation("io.netty:netty-handler:4.2.18.Final") { because("CVE: Netty SslHandler / SNI allocation") }
        implementation("com.google.protobuf:protobuf-javalite:3.25.5") {
            because("CVE-2024-7254: unbounded recursion parsing untrusted protobuf (MediaPipe tasks-core pins 3.19.1). Held on the 3.x line: MediaPipe's generated code predates the 4.x runtime's gencode version check.")
        }
        implementation("org.apache.httpcomponents.core5:httpcore5:5.4.3") {
            because("CVE: HTTP/1 header parsing memory exhaustion (arrives via MSAL)")
        }
        implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3") {
            because("Align with the patched httpcore5")
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
    implementation(libs.play.services.location) // Explicitly use KTX version and direct coordinate

    // Core testing dependencies
    testImplementation(libs.junit) // JUnit 4
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlin.mockito.kotlin)
    testImplementation(libs.mockito.inline) // ADDED for static mocking

    // WorkManager Testing (version 2.10.4 matches your work-runtime-ktx)
    testImplementation(libs.androidx.work.testing)
    
    // Raw TensorFlow Lite / LiteRT interpreters were only ever used by
    // LegalBertService, which loaded an asset (legal_bert.tflite) that does not
    // exist in this repository, tokenised with a vocab.txt whose contents were
    // the literal placeholder line "... (full vocabulary content) ...", and was
    // injected by nothing outside its own test. All of it is gone; on-device
    // embeddings come from MediaPipe's TextEmbedder in SemanticService, which
    // is real and wired up.

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

    // Mockito for AndroidTest
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.kotlin.mockito.kotlin)
    androidTestImplementation(libs.mockito.inline)

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

    implementation(libs.mediapipe.tasks.text)
    implementation(libs.androidx.tracing.ktx)

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
    implementation(libs.google.api.services.gmail) // ADDED
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

    // JetLime Timeline Library
    implementation(libs.jetlime)

    // Dropbox SDK
    implementation(libs.dropbox.core.sdk)
    implementation(libs.dropbox.android.sdk)

    // Google Tink — authenticated encryption for OAuth credentials at rest.
    implementation(libs.google.tink.android)

    // Microsoft Graph SDK for OneDrive & MSAL for Authentication
    // implementation(libs.microsoft.graph.core) // REMOVED
    implementation(libs.microsoft.graph)
    implementation(libs.msal)    // CORRECTED from msal4j
    // Jakarta Mail for IMAP
    implementation(libs.jakarta.mail)

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
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    // Room IS NOT ALLOWED IN THIS PROJECT!!!!!

    // Vosk for on-device speech-to-text
    implementation(libs.vosk.android) // UNCOMMENTED

    // Explicit gRPC dependencies with consistent versions
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.core)
    implementation(libs.grpc.context)

    // AzNavRail 11.x is a Compose Multiplatform rewrite. Its JitPack umbrella
    // module (com.github.hereliesaz:aznavrail) declares every platform
    // publication -- android, desktop, wasm-js and the legacy AAR -- as a plain
    // dependency rather than as variants of one module, so an Android consumer
    // resolving it gets a wasm library it cannot fetch and three copies of
    // every class, and the build dies at checkDuplicateClasses. Compilation
    // succeeds either way, which is why this only appears in a full assemble.
    //
    // Depending on the Android publication directly sidesteps the broken
    // metadata. Worth fixing upstream in the library's publication.
    implementation(libs.aznavrail)

    // Kotlin/Compose Multiplatform core: domain models, LexResult, the evidence
    // pipeline contracts and the cross-platform UI.
    implementation(project(":shared"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}
