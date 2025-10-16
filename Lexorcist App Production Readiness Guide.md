

# **The Lexorcist: Production Readiness and Remediation Protocol**

## **Executive Summary**

This report provides a comprehensive, step-by-step protocol for transforming "The Lexorcist" Android application from its current developmental state into a production-ready product. The project's vision—to empower legal professionals by automating evidence processing—is robust and well-defined.1 However, the current codebase is encumbered by significant foundational issues, including build system instability, security vulnerabilities, project contamination, and incomplete critical features.

This protocol is designed to be executed by an autonomous agent. It is structured in three sequential phases:

1. **Foundational Project Sanitization and Build System Stabilization:** Eradicate non-project files, rectify critical security flaws, resolve build version inconsistencies, and harmonize the entire dependency graph to establish a stable, reproducible build environment.  
2. **Implementation of Incomplete Core Features:** Systematically implement the high-priority features outlined in project documentation, including video evidence processing, the advanced on-device AI and dynamic UI scripting capabilities, redundant evidence cleanup, and case finalization.  
3. **Pre-Production Hardening and Refinement:** Implement essential production-level safeguards, including code obfuscation, API key protection, performance tracing, and final documentation updates to ensure the application is secure, maintainable, and robust.

Adherence to this protocol will systematically address all identified deficiencies and deliver a final product that is stable, secure, and aligned with the project's ambitious goals.

## **1\. Foundational Project Sanitization and Build System Stabilization**

The current project archive is not a clean repository but a direct workspace copy, containing extraneous files, security risks, and unstable build configurations. This phase is a mandatory prerequisite for all subsequent development; failure to complete these steps will render any further work unreliable and insecure.

### **1.1 Workspace and Repository Decontamination**

The project directory contains a significant number of files that are not part of the core application, including an entire unrelated sample project. This contamination introduces immediate security risks and build ambiguities that must be eliminated.1 The presence of crash logs, local backup scripts, and IDE-specific files indicates a lack of source control hygiene, which this step will rectify.

The most critical issue is the inclusion of the ms-identity-android-kotlin-master directory. This unrelated project contains its own debug.keystore, which represents a severe security vulnerability. The private key for this keystore is now compromised, and its presence creates ambiguity for the build system.

**Action Protocol:**

1. Execute the deletion of the following directories from the project root:  
   * ms-identity-android-kotlin-master  
   * whisper\_android (Dependencies for this module should be managed via Gradle).  
2. Execute the deletion of all files matching the following patterns from the project root:  
   * hs\_err\_pid\*.log  
   * hs\_err\_pid\*.mdmp  
   * project\_backup\_full\_\*.txt  
   * backup.ps1  
3. Verify that the .gitignore file correctly lists directories and patterns such as /.idea/, /\*/build/, \*.log, and local.properties to prevent future contamination of the version control system.1

### **1.2 Build Configuration and Security Rectification**

The project's build configuration contains version mismatches and a critical security flaw related to the release signing key, making the build process unstable and non-portable.

The user has specified an Android Gradle Plugin (AGP) version of 8.14.0. However, the project's build files (build.gradle.kts and gradle/libs.versions.toml) are configured for version 8.13.0.1 External analysis of official Google and Gradle repositories confirms that while Gradle itself has a version 8.14, there is no corresponding stable release for AGP 8.14.0.2 Attempting to build with a non-existent or pre-release AGP version is a direct violation of production-readiness principles. Therefore, deviation from the user's specified version is absolutely necessary to achieve a stable build.

Furthermore, the app/build.gradle.kts file contains a hardcoded absolute path to the release signing keystore (G://My Drive//az\_apk\_keystore.jks).1 This practice renders the release build process non-portable and exposes sensitive details about the developer's file system structure.

**Action Protocol:**

1. Modify gradle/libs.versions.toml to set androidApplication \= "8.14.0".  
2. Modify the root build.gradle.kts file to set the com.android.application plugin version to 8.14.0.  
3. Execute a Gradle sync operation (e.g., ./gradlew :app:dependencies). This operation is expected to fail due to the unavailability of the specified AGP version.  
4. Log the failure and its cause (unresolved plugin).  
5. Programmatically query Google's Maven repository for the latest stable version of the Android Gradle Plugin (com.android.application).  
6. Update the androidApplication version in gradle/libs.versions.toml and the plugin version in the root build.gradle.kts to this latest stable version.  
7. Consult official Android documentation to determine the required Gradle version for the selected stable AGP version.3 Update the distributionUrl in gradle/wrapper/gradle-wrapper.properties accordingly.  
8. In app/build.gradle.kts, modify the signingConfigs.release block. Remove the hardcoded storeFile, storePassword, and keyPassword values.  
9. Re-implement these properties to be read dynamically from the local.properties file, which is correctly excluded by .gitignore. The implementation should mirror the existing pattern used for reading API\_KEY.

### **1.3 Dependency Graph Harmonization**

The project's dependency report (dependencies.txt) reveals numerous version conflicts among transitive dependencies, indicating a lack of centralized version management.1 This leads to unpredictable builds, increased compilation times, and a high risk of runtime errors. A production-ready application requires a deterministic and conflict-free dependency graph.

The primary solution is to enforce a single source of truth for all dependency versions using the gradle/libs.versions.toml version catalog and to leverage Bill of Materials (BOM) files for large library families like Jetpack Compose. A specific, known issue with Compose testing setups is the incorrect configuration of ui-test-manifest, which must be declared as a debugImplementation dependency to function correctly.5

**Action Protocol:**

1. In app/build.gradle.kts, add the Jetpack Compose Bill of Materials to enforce version consistency across all Compose libraries: implementation(platform(libs.androidx.compose.bom)).  
2. Remove all explicit version strings from individual androidx.compose.\* dependencies in the same file, allowing the BOM to manage them.  
3. Systematically audit app/build.gradle.kts and gradle/libs.versions.toml. For every dependency, ensure its version is defined once in the TOML file and referenced by its alias in the build script.  
4. Locate the dependency declaration for androidx.compose.ui:ui-test-manifest. Modify its configuration from implementation or androidTestImplementation to debugImplementation.  
5. Execute ./gradlew :app:dependencies \--refresh-dependencies.  
6. Parse the command's output to verify that all major version conflicts (indicated by \-\>) have been resolved. The following table outlines the strategy for resolving the most critical conflicts identified.

**Table 1: Dependency Harmonization Plan**

| Library Group & Name | Conflicting Versions Found | Resolved Version Strategy | Action Taken | Justification |
| :---- | :---- | :---- | :---- | :---- |
| org.jetbrains.kotlin:kotlin-stdlib | 1.6.10 \-\> 2.1.21 | User Specified | Unify in TOML to 2.2.20 | Aligns with project requirements and ensures consistent Kotlin runtime across all modules and libraries. |
| androidx.compose.\* | runtime:1.9.0 \-\> 1.9.1 | Latest Stable | Enforce Compose BOM | Uses Google's official Bill of Materials to guarantee compatibility and simplify management of all Compose artifacts. |
| com.squareup.kotlinpoet | 1.11.0 \-\> 1.14.2 | Latest Stable | Unify in TOML | Resolves transitive conflict from annotation processors (hilt-compiler). |
| androidx.core:core | 1.2.0 \-\> 1.8.0 | Latest Stable | Unify in TOML | A fundamental AndroidX library; a single version prevents API mismatches and runtime errors. |
| androidx.compose.ui:ui-test-manifest | N/A (Configuration Error) | N/A | Change to debugImplementation | Corrects a common setup error required for Compose instrumented tests to find the test activity manifest.5 |

## **2\. Implementation of Incomplete Core Features**

With a stable build foundation, development can proceed to complete the application's core functionality as described in the project's TODO.md and SCRIPT\_EXAMPLES.md files.1

### **2.1 Video Evidence Processing Pipeline**

The TODO.md file identifies video processing as a high-priority, unimplemented feature. The required workflow involves frame extraction, OCR on each frame, audio transcription, and aggregation of all resulting text into a single evidence record. While external libraries like FFMPEG exist, the native Android MediaMetadataRetriever class provides all necessary functionality without increasing the application's binary size or adding complex external dependencies.8

**Action Protocol:**

1. Create a new Kotlin file for VideoProcessingService.kt.  
2. Implement a public function within the service, such as processVideo(uri: Uri): String, which will orchestrate the processing.  
3. Inside this function, instantiate android.media.MediaMetadataRetriever.  
4. Use setDataSource(context, uri) to initialize the retriever with the video file.  
5. Retrieve the video duration from the retriever's metadata.  
6. Implement a loop that iterates from 0 to the video duration with a fixed interval (e.g., 5000 milliseconds). In each iteration, call getFrameAtTime() to extract a Bitmap of the video frame.  
7. For each extracted Bitmap, invoke the existing OcrProcessingService to perform text recognition. Collect the resulting text strings.  
8. Invoke the WhisperService (from the :whisper module) to perform audio transcription on the same video URI.  
9. Aggregate the transcribed audio and all collected OCR text into a single, formatted string.  
10. Return the aggregated string. The calling service will be responsible for creating the final Evidence entity.

### **2.2 Advanced Scripting Engine Activation**

The SCRIPT\_EXAMPLES.md file outlines a powerful vision for the scripting engine, including on-device AI for semantic analysis and the ability for scripts to dynamically modify the UI.1 These features are marked as conceptual and require implementation.

#### **2.2.1 On-Device Semantic Similarity (lex.ai.local)**

The requirement is to calculate the semantic similarity between two pieces of text directly on the device. The Google MediaPipe TextEmbedder task is ideally suited for this, as it is designed for Android, includes a built-in cosine similarity function, and can be used with efficient on-device models.10 This provides a robust and well-supported solution compared to implementing the text-to-vector pipeline manually with other models like MiniLM.11

**Action Protocol:**

1. Add the com.google.mediapipe:tasks-text dependency to app/build.gradle.kts.  
2. Download a compatible text embedding model (e.g., universal\_sentence\_encoder) and place it in the app/src/main/assets directory.  
3. Create a new SemanticService.kt file.  
4. Within this service, create a singleton instance of TextEmbedder. Initialize it using TextEmbedder.createFromFileAndOptions(), providing the path to the model file in the assets.  
5. Implement a public function calculateSimilarity(text1: String, text2: String): Float.  
6. Inside this function, call textEmbedder.embed() on both input strings to get their EmbeddingResult.  
7. Use the static method TextEmbedder.cosineSimilarity() to compute the similarity between the two embedding results.  
8. Return the resulting similarity score.  
9. In ScriptRunner.kt, expose an instance of SemanticService to the JavaScript context. Define a bridge function so that calling lex.ai.local.calculateSimilarity(text1, text2) in JavaScript executes the corresponding Kotlin method.

#### **2.2.2 Dynamic UI Generation (lex.ui)**

Dynamically adding UI elements from a script in Jetpack Compose should not be done via direct manipulation or reflection. The idiomatic, declarative approach is to have the script modify a state model, which the UI then observes and renders accordingly.13 The script will generate a JSON schema describing the desired UI, which a dedicated Composable will then parse and render.

**Action Protocol:**

1. Define a set of Kotlin data classes that represent a simple UI schema (e.g., UiComponentModel(id: String, type: String, properties: Map\<String, String\>)). The type can be an enum for "button", "text", etc.  
2. In the relevant ViewModel (e.g., EvidenceViewModel), create a MutableStateFlow\<List\<UiComponentModel\>\> to hold the list of dynamically generated UI components.  
3. Create a new Composable function, DynamicUiRenderer(components: List\<UiComponentModel\>).  
4. Inside this Composable, iterate through the components list. Use a when (component.type) block to render the appropriate Composable (e.g., AzButton for "button", Text for "text"), configuring it with data from the properties map.  
5. In ScriptRunner.kt, expose a function to the JavaScript context under lex.ui.addOrUpdate(). This function will accept a JSON string from the script.  
6. The bridge function will parse this JSON into a UiComponentModel and update the ViewModel's StateFlow, triggering a recomposition that renders the new UI element.

### **2.3 Redundant Text Cleanup Implementation**

The "Redundant Text Cleanup" feature, listed as incomplete in TODO.md, is a direct application of the semantic similarity engine developed in the previous step.1 This task involves finding and grouping evidence items with highly similar text content for user review.

**Action Protocol:**

1. Create a CleanupService.kt file if one does not already exist.  
2. Inject the SemanticService into the CleanupService.  
3. Implement a new function findSimilarTextEvidence(evidenceList: List\<Evidence\>): List\<List\<Evidence\>\>.  
4. Inside this function, perform a pairwise comparison of all evidence items using semanticService.calculateSimilarity(). This can be optimized to avoid redundant checks (e.g., comparing A-B is the same as B-A).  
5. Group all evidence items whose similarity score exceeds a defined threshold (e.g., 0.95).  
6. In the ReviewViewModel, call this service method to get the groups of similar evidence.  
7. In ReviewScreen.kt, add a new LazyColumn section titled "Potential Text Duplicates."  
8. For each group returned by the service, render a card that displays a preview of the items and provides a "Review & Merge" button. The merge logic will be handled by a subsequent task.

### **2.4 Case Finalization and Export**

The "Finalize" button functionality, as detailed in TODO.md, is entirely unimplemented.1 This feature allows the user to package selected case files into a compressed archive (.zip or .lex).

**Action Protocol:**

1. In ReviewScreen.kt, implement the onClick listener for the "Finalize" AzButton. This should trigger the display of a new FinalizeCaseDialog Composable.  
2. The FinalizeCaseDialog will contain:  
   * A LazyColumn that lists all files and folders within the current case's directory. Each item will have a Checkbox.  
   * A Switch or Row of AzToggle buttons to select the output format: .zip or .lex.  
   * Confirm and Cancel buttons.  
3. Upon confirmation, launch the Android Storage Access Framework's ACTION\_CREATE\_DOCUMENT intent. This allows the user to select a name and location for the output file.  
4. Create a PackagingService.kt. Implement a function createArchive(files: List\<File\>, destinationUri: Uri, format: String).  
5. Inside this service, use context.contentResolver.openOutputStream(destinationUri) to get an OutputStream. Wrap it in a ZipOutputStream.  
6. Iterate through the list of files selected by the user. For each file, create a ZipEntry and write the file's contents to the ZipOutputStream.  
7. Close the stream upon completion. The .lex format will be handled by simply using .lex as the file extension for a standard zip archive.

## **3\. Pre-Production Hardening and Refinement**

The final phase addresses non-functional requirements essential for a secure, performant, and maintainable production application.

### **3.1 Security, Obfuscation, and Performance**

The current release configuration has significant security weaknesses that must be addressed before public distribution. Minification and obfuscation are disabled, and an API key is insecurely stored in the BuildConfig.1

**Action Protocol:**

1. In app/build.gradle.kts, locate the buildTypes.release block. Change isMinifyEnabled from false to true.  
2. In the proguard-rules.pro file, add the following essential rules to prevent critical code from being removed or obfuscated by R8:  
   * Keep rules for all data model classes: \-keep class com.hereliesaz.lexorcist.models.\*\* { \*; }  
   * Keep rules for all classes annotated with @Serializable if using kotlinx.serialization.  
   * Add the standard ProGuard rules for Hilt, Coroutines, and any other libraries that use reflection.  
3. Remove the buildConfigField("String", "API\_KEY",...) line from app/build.gradle.kts.  
4. Implement NDK-based API key storage:  
   * Create a native-lib.cpp file in app/src/main/cpp.  
   * Define a JNI function that returns the API key as an obfuscated string.  
   * Load this native library in the application and retrieve the key at runtime.  
   * Ensure the API key itself is stored in local.properties and passed to the CMake build process, not checked into version control.  
5. To enable performance analysis, add the androidx.tracing:tracing-ktx dependency.  
6. In performance-sensitive code paths identified in Section 2 (e.g., VideoProcessingService, CleanupService), add trace("Section Name") {... } blocks to create custom trace events visible in Android Studio's System Trace profiler.

### **3.2 Documentation and Code Hygiene**

The project's documentation is incomplete and partially outdated. Accurate documentation is crucial for future maintenance and contribution.

**Action Protocol:**

1. Review the docs/auth.md file. Verify that the step-by-step instructions for configuring Google and Outlook/Azure APIs are accurate and complete.1 Add any missing steps or clarify ambiguous language.  
2. Update SCRIPT\_EXAMPLES.md. Remove the note stating that lex.ai.local and lex.ui are conceptual. Replace it with fully functional, tested script examples that demonstrate the new capabilities.  
3. Review TODO.md. Consolidate any remaining, incomplete, lower-priority tasks into a new "Future Work" section in the main README.md file. Delete the TODO.md file.  
4. Execute a full project lint and format check to ensure code consistency and identify potential issues:  
   * Run ./gradlew lintDebug and analyze the generated report for any warnings or errors.  
   * If a code formatting tool like ktlint or spotless is configured, run its apply task (e.g., ./gradlew spotlessApply). If not, configure ktlint with the default ruleset and apply it across the codebase.

## **Conclusion and Recommendations**

The "The Lexorcist" project possesses a strong conceptual foundation and a clear product vision. However, its path to production was blocked by fundamental issues in build stability, security, and feature completeness. The execution of this three-phase protocol will systematically remediate these deficiencies.

Upon completion, the project will be transformed into a production-ready state, characterized by:

* **A Stable and Reproducible Build:** The harmonized dependency graph and corrected build configurations will ensure that the application can be reliably built and deployed in any standard development or CI/CD environment.  
* **Enhanced Security:** The removal of hardcoded credentials, implementation of code obfuscation, and secure storage of API keys will significantly harden the application against reverse-engineering and data extraction.  
* **Feature Completeness:** The implementation of video processing, the advanced scripting engine, and case finalization will fulfill the core promises of the application's documented feature set, delivering substantial value to the end-user.  
* **Improved Maintainability:** The sanitized repository, centralized dependency management, and updated documentation will create a clean and understandable codebase, facilitating future development and maintenance.

It is recommended that this protocol be executed sequentially, as each phase builds upon the stability and correctness established by the previous one. Final verification should include a full regression test of all primary user workflows before deploying the application to the Google Play Store.

#### **Works cited**

1. project\_backup\_full\_2025-10-15\_20-34-33.txt  
2. Gradle 8.14 Release Notes, accessed October 15, 2025, [https://docs.gradle.org/8.14/release-notes.html](https://docs.gradle.org/8.14/release-notes.html)  
3. Android Gradle plugin 8.13 release notes | Android Studio | Android ..., accessed October 15, 2025, [https://developer.android.com/build/releases/gradle-plugin](https://developer.android.com/build/releases/gradle-plugin)  
4. Gradle \- com.android.tools.build \- Maven Repository, accessed October 15, 2025, [https://mvnrepository.com/artifact/com.android.tools.build/gradle](https://mvnrepository.com/artifact/com.android.tools.build/gradle)  
5. The ui-test-manifest library should be included using the debugImplementation configuration \- Google Samples, accessed October 15, 2025, [https://googlesamples.github.io/android-custom-lint-rules/checks/TestManifestGradleConfiguration.md.html](https://googlesamples.github.io/android-custom-lint-rules/checks/TestManifestGradleConfiguration.md.html)  
6. Compose project template doesn't include debug dependency on androidx.compose.ui:ui-test-manifest \[202839436\] \- Issue Tracker, accessed October 15, 2025, [https://issuetracker.google.com/issues/202839436](https://issuetracker.google.com/issues/202839436)  
7. Add androidx.compose.ui:ui-test-manifest:$composeVersion as part of the shot core module dependencies · Issue \#272 \- GitHub, accessed October 15, 2025, [https://github.com/pedrovgs/Shot/issues/272](https://github.com/pedrovgs/Shot/issues/272)  
8. kibotu/android-ffmpeg-transcoder: On device extracting images from videos \- GitHub, accessed October 15, 2025, [https://github.com/kibotu/android-ffmpeg-transcoder](https://github.com/kibotu/android-ffmpeg-transcoder)  
9. Dynamically Retrieving Frames in Android | by Oğuzhan Aslan \- Medium, accessed October 15, 2025, [https://oguzhanaslann.medium.com/dynamically-retrieving-frames-in-android-a659ab406786](https://oguzhanaslann.medium.com/dynamically-retrieving-frames-in-android-a659ab406786)  
10. Text embedding guide | Google AI Edge \- Gemini API, accessed October 15, 2025, [https://ai.google.dev/edge/mediapipe/solutions/text/text\_embedder](https://ai.google.dev/edge/mediapipe/solutions/text/text_embedder)  
11. hissain/AndroidSemanticSearch: A minimalistic Android app showcasing semantic search using ObjectBox and Lucene KNN, leveraging the MiniLM-L6-V2 embedding model and bert\_vocab.txt for efficient retrieval. \- GitHub, accessed October 15, 2025, [https://github.com/hissain/AndroidSemanticSearch](https://github.com/hissain/AndroidSemanticSearch)  
12. Sentence-Embeddings-Android: An Android library to use access all-MiniLM-L6-V2 sentence embeddings (HF sentence-transformers) : r/androiddev \- Reddit, accessed October 15, 2025, [https://www.reddit.com/r/androiddev/comments/1dpunde/sentenceembeddingsandroid\_an\_android\_library\_to/](https://www.reddit.com/r/androiddev/comments/1dpunde/sentenceembeddingsandroid_an_android_library_to/)  
13. Thinking in Compose | Jetpack Compose \- Android Developers, accessed October 15, 2025, [https://developer.android.com/develop/ui/compose/mental-model](https://developer.android.com/develop/ui/compose/mental-model)  
14. Dynamically adding UI components at runtime. : r/JetpackCompose \- Reddit, accessed October 15, 2025, [https://www.reddit.com/r/JetpackCompose/comments/10m10vl/dynamically\_adding\_ui\_components\_at\_runtime/](https://www.reddit.com/r/JetpackCompose/comments/10m10vl/dynamically_adding_ui_components_at_runtime/)  
15. Jetpack Compose: Build UI dynamically based on runtime data \- Stack Overflow, accessed October 15, 2025, [https://stackoverflow.com/questions/79589816/jetpack-compose-build-ui-dynamically-based-on-runtime-data](https://stackoverflow.com/questions/79589816/jetpack-compose-build-ui-dynamically-based-on-runtime-data)