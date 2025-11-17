# The Lexorcist: Master TODO & Remediation Protocol

This document is the single source of truth for all outstanding development tasks. It supersedes and replaces the previous `TODO.md` and `docs/task_flow.md`. All tasks are organized into three sequential phases to transform the application from its current state to a stable, feature-complete, and secure production-ready product.

---

## **Phase 1: Foundational Project Sanitization and Build System Stabilization**

This phase is a mandatory prerequisite for all subsequent development. Its goal is to create a clean, stable, secure, and reproducible build environment.

### **1.1 Workspace and Repository Decontamination**
* [ ] **Delete Extraneous Directories:** Execute the deletion of the following directories from the project root:
    * `ms-identity-android-kotlin-master`
    * `whisper_android` (This module's dependencies should be managed via Gradle)
* [ ] **Delete Extraneous Files:** Execute the deletion of all files matching the following patterns from the project root:
    * `hs_err_pid*.log`
    * `hs_err_pid*.mdmp`
    * `project_backup_full_*.txt`
    * `backup.ps1`
    * `ms-identity-android-kotlin.zip`
* [ ] **Verify `.gitignore`:** Audit the `.gitignore` file to ensure it correctly lists directories and patterns such as `/.idea/`, `/*/build/`, `*.log`, and `local.properties` to prevent future contamination.

### **1.2 Build Configuration and Security Rectification**
* [ ] **Stabilize AGP and Gradle Versions:**
    1.  Programmatically query Google's Maven repository for the *latest stable* version of the Android Gradle Plugin (`com.android.application`).
    2.  Consult official Android documentation to determine the required Gradle version for that stable AGP.
    3.  Update `androidApplication` in `gradle/libs.versions.toml` to the latest stable AGP version.
    4.  Update the `com.android.application` plugin version in the root `build.gradle.kts` to match.
    5.  Update the `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` to the required Gradle version.
* [ ] **Secure `signingConfigs`:**
    1.  Modify `app/build.gradle.kts` to remove the hardcoded `storeFile`, `storePassword`, and `keyPassword` from the `signingConfigs.release` block.
    2.  Re-implement these properties to be read dynamically from the `local.properties` file.

### **1.3 Dependency Graph Harmonization**
* [ ] **Implement Compose BOM:** In `app/build.gradle.kts`, add the Jetpack Compose Bill of Materials: `implementation(platform(libs.androidx.compose.bom))`.
* [ ] **Refactor Compose Dependencies:** Remove all explicit version strings from individual `androidx.compose.*` dependencies in `app/build.gradle.kts` to allow the BOM to manage them.
* [ ] **Centralize Versions:** Audit `app/build.gradle.kts` and `gradle/libs.versions.toml` to ensure every dependency's version is defined *once* in the TOML file and referenced by its alias.
* [ ] **Fix Test Configuration:** Locate the dependency for `androidx.compose.ui:ui-test-manifest` and modify its configuration to `debugImplementation`.
* [ ] **Verify Resolution:** Execute `./gradlew :app:dependencies --refresh-dependencies` and parse the output to verify that all major version conflicts have been resolved.

---

## **Phase 2: Implementation of Incomplete Core Features**

With a stable foundation, development will proceed to complete the application's core feature set as defined across all project documentation.

### **2.1 Video Evidence Processing Pipeline**
* [ ] **Create `VideoProcessingService.kt`:** Implement a new service for handling video files.
* [ ] **Implement Frame Extraction:** Use `MediaMetadataRetriever` to loop through the video at a fixed interval (e.g., every 5 seconds) and extract frames as `Bitmap` objects.
* [ ] **Process Frames:** For each extracted `Bitmap`, invoke the existing `OcrProcessingService` to perform text recognition.
* [ ] **Transcribe Audio:** Invoke the `WhisperService` to perform audio transcription on the video file's audio track.
* [ ] **Aggregate Evidence:** Combine the transcribed audio and all collected OCR text into a single, formatted string. Ensure this text is wrapped in Markdown code blocks per `docs/workflow.md`.
* [ ] **Save Evidence:** Create a new `Evidence` record linking the aggregated text to the original video file, which should be copied to the "raw" folder.

### **2.2 Editable Audio Transcripts**
* [ ] **Create UI:** Implement a new screen or dialog that displays an audio transcript in an editable `TextField`.
* [ ] **Track Edits:** When an edit is made, prompt the user for a reason for the change (e.g., "Corrected spelling," "Removed filler word").
* [ ] **Store Edit History:** Modify the `Evidence` data model or a related table to store the original transcript and a log of all edits (e.g., a JSON list of objects containing the diff, timestamp, and reason).

### **2.3 Advanced Scripting Engine Activation**
* [ ] **On-Device Semantic AI (`lex.ai.local`):**
    1.  Add the `com.google.mediapipe:tasks-text` dependency.
    2.  Verify the `universal_sentence_encoder.tflite` model is in `app/src/main/assets`.
    3.  Create `SemanticService.kt` with a singleton instance of `TextEmbedder`.
    4.  Implement `calculateSimilarity(text1: String, text2: String): Float` using `TextEmbedder.cosineSimilarity()`.
    5.  Expose the `SemanticService` to the `ScriptRunner` JavaScript context under `lex.ai.local`.
* [ ] **Dynamic UI Generation (`lex.ui`):**
    1.  Define `UiComponentModel` data classes (e.g., `id`, `type`, `properties`) to represent a simple UI schema.
    2.  Add a `MutableStateFlow<List<UiComponentModel>>` to the relevant ViewModel.
    3.  Create a `DynamicUiRenderer(components: List<UiComponentModel>)` Composable that uses a `when` block to render the correct component (e.g., `AzButton`, `Text`) based on its `type`.
    4.  In `ScriptRunner.kt`, expose a function `lex.ui.addOrUpdate(json: String)` that parses the JSON and updates the ViewModel's `StateFlow`.
* [ ] **Google Apps Script Integration:** Verify and, if incomplete, implement the `ScriptRunner`'s ability to call Google Apps Script functions via the `GoogleApiService`, as described in `TODO.md` and `docs/workflow.md`.
* [ ] **Change-Detection Mechanism:** Implement a change-detection system (e.g., using hashes of script content and evidence content) to ensure scripts are only re-run on evidence that has changed or if the script itself has been updated.

### **2.4 Evidence Cleanup & Organization (`ReviewScreen.kt`)**
* [ ] **Button Layout:** Verify the `ReviewScreen` contains the `AzButton` components labeled "Automatic Cleanup", "Paperwork", and "Finalize". (Note: `docs/UI_UX.md` and `docs/task_flow.md` have conflicting button names. This plan will use the names from `docs/screens.md` and `docs/workflow.md`).
* [ ] **Duplicate Detection (File Hash):**
    1.  Verify that `OcrProcessingService` and `VideoProcessingService` generate and store a `fileHash` for each `Evidence` item.
    2.  Create/Update `CleanupService.kt` with a function to find duplicate `Evidence` items by comparing `fileHash`.
    3.  Implement a UI component on the Review screen (under "Automatic Cleanup") to display groups of duplicates and provide a "Keep One, Delete Rest" button.
* [ ] **Image Series Merging:**
    1.  Implement an algorithm in `CleanupService` to identify sequential screenshots (based on filename patterns and timestamps).
    2.  Implement a UI component to display these series.
    3.  Implement the "Merge" button logic to combine the images into a single PDF and merge their OCR text into one `Evidence` record.
* [ ] **Redundant Text Cleanup (Semantic):**
    1.  Implement a function in `CleanupService` that uses `SemanticService.calculateSimilarity()` to find `Evidence` items with a similarity score > 0.95.
    2.  Add a `LazyColumn` section to the "Automatic Cleanup" UI to display these groups with a "Review & Merge" button.

### **2.5 Legal Elements Database**
* [ ] **Implement Repository:** Fully implement the `LegalElementsRepository` (beyond the current placeholder noted in `TODO.md`).
* [ ] **Load Data:** The repository should load and store the legal elements required to support each allegation, likely by parsing `allegations.csv` and `exhibits.csv`.
* [ ] **Guide Exhibit Creation:** Use this data in the `ExhibitsScreen` to guide the user in creating effective exhibits that map directly to the elements of the selected allegations.

### **2.6 Document Generation ("Paperwork" Button)**
* [ ] **Implement `onClick`:** Wire up the "Paperwork" button on the `ReviewScreen`.
* [ ] **Generation Logic:** The function should:
    1.  Iterate through all `Exhibits` in the current case.
    2.  For each `Exhibit`, find its associated `Template`.
    3.  Call the `GoogleApiService` to generate a document from the template, populating it with data from the evidence within that exhibit.
* [ ] **Show Progress:** Display a global loading indicator and a final success/failure message.

### **2.7 Case Finalization ("Finalize" Button)**
* [ ] **Implement `onClick`:** Wire up the "Finalize" button to show a `FinalizeCaseDialog`.
* [ ] **Dialog UI:** The dialog must contain a `LazyColumn` with checkboxes for all files/folders in the case directory, and a toggle (e.g., `AzToggle`) for `.zip` or `.lex` format.
* [ ] **Create `PackagingService.kt`:** Create a new service with a function `createArchive(files: List<File>, destinationUri: Uri, format: String)`.
* [ ] **Archive Logic:** Use `ZipOutputStream` to create the archive. The `.lex` format is a standard `.zip` with a custom extension.
* [ ] **Launch File Picker:** Upon dialog confirmation, launch the `ACTION_CREATE_DOCUMENT` intent to allow the user to select a save location and name. Pass the resulting `Uri` to the `PackagingService`.

---

## **Phase 3: Pre-Production Hardening and Refinement**

This final phase addresses non-functional requirements essential for a secure, performant, and maintainable production application.

### **3.1 Security and Obfuscation**
* [ ] **Enable R8:** In `app/build.gradle.kts`, in the `buildTypes.release` block, change `isMinifyEnabled` to `true`.
* [ ] **Configure ProGuard:** Add and test `proguard-rules.pro` to keep rules for all data models, Hilt components, Coroutine classes, and any other libraries that use reflection.
* [ ] **Secure API Key:**
    1.  Remove the `buildConfigField` for `API_KEY` from `app/build.gradle.kts`.
    2.  Implement NDK-based API key storage using a `native-lib.cpp` file and JNI to retrieve the key at runtime.
    3.  Ensure the key is passed from `local.properties` to the CMake build process and is not checked into version control.

### **3.2 Performance Tracing**
* [ ] **Add Dependency:** Add the `androidx.tracing:tracing-ktx` dependency.
* [ ] **Add Traces:** Add `trace("Section Name") { ... }` blocks to performance-sensitive code paths, especially `VideoProcessingService`, `CleanupService`, and `ScriptRunner`.

### **3.3 Documentation and Code Hygiene**
* [ ] **Review API Docs:** Review and update `docs/auth.md` to ensure the steps for Google and Outlook/Azure API configuration are accurate.
* [ ] **Update Script Examples:** Update `SCRIPT_EXAMPLES.md` to remove notes about conceptual APIs and add working, tested examples for the new `lex.ai.local` and `lex.ui` functions.
* [ ] **Linting:** Run `./gradlew lintDebug`, analyze the report, and fix all high-priority warnings and errors.
* [ ] **Code Formatting:** Configure and run a code formatter like `ktlint` or `spotlessApply` to ensure a consistent code style across the entire project.
* [ ] **Final Cleanup:** Consolidate any remaining low-priority tasks into this file's "Future Work" section (or delete them if obsolete) and then **delete** the old `docs/task_flow.md` file.

---

## **Future Work (Post-Production)**
* (Empty - To be populated after Phase 3 review)
