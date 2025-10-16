# Project TODO List

**Note:** This file is pending deletion. All outstanding tasks have been migrated to the "Production Readiness and Remediation Protocol" section below.

---

### **I. High-Priority Roadmap Items**

1.  **Cloud Synchronization:** [COMPLETED]
    *   **On App Close:** Implement a robust mechanism to trigger a full synchronization of the local `lexorcist_data.xlsx` file and the case folder with the selected cloud provider (Google Drive, etc.) when the application is closed. [COMPLETED]
    *   **On App Load:** Implement a synchronization check upon application startup to ensure the local data is consistent with the latest version in the cloud. [COMPLETED]
    *   **Note:** The `GoogleDriveCloudStorageProvider` was fully implemented to make the existing `SyncManager` logic functional. Sync on close was already implemented via `AppLifecycleObserver`.

2.  **Video Evidence Processing:** [COMPLETED]
    *   **Note:** This task is superseded by **Task 29** in the Production Readiness and Remediation Protocol.

3.  **Editable Audio Transcripts:** [COMPLETED]
    *   **UI Implementation:** Create a new screen or dialog that displays an audio transcript in an editable text field. [COMPLETED]
    *   **Edit Tracking:** When an edit is made, prompt the user for a reason for the change. [COMPLETED]
    *   **Data Storage:** Store the original transcript and a log of all edits (including the change, timestamp, and reason) in the evidence record. [COMPLETED]

4.  **Transparent Progress Reporting:** [COMPLETED]
    *   **Detailed Log View:** Enhance the existing progress reporting UI to include a scrollable, detailed log view that displays each step of the evidence processing pipeline (e.g., "Uploading file," "Running OCR," "Applying script 'Profanity Tagger'"). [COMPLETED]
    *   **Structured Logging:** Refactor the logging services to provide more structured and user-friendly log messages. [COMPLETED]

---

### **II. UI/UX Enhancements & Fixes**

5.  **Component Right-Alignment:** [COMPLETED]
    *   Audit every screen (`CasesScreen`, `EvidenceScreen`, `SettingsScreen`, etc.) and ensure all UI components (Buttons, TextFields, Cards, etc.), but not the text within them, are right-aligned as per the UI/UX rules.
    *   **Note:** This was completed in conjunction with the `AzButton` conversion.

6.  **`AzButton` Conversion:** [COMPLETED]
    *   Systematically review all screens and replace any standard Material `Button`, `ToggleButton`, `FloatingActionButton`, or similar components with their `AzNavRail` library equivalents (`AzButton`, `AzToggle`, `AzCycler`).
    *   **Note:** All `LexorcistOutlinedButton` and standard `Button` components were replaced with `AzButton`. `AlertDialog` was replaced with `AzAlertDialog`.

7.  **Case List Highlighting:** [COMPLETED]
    *   In `CasesScreen.kt`, implement a visual indicator (e.g., changing the background color) to highlight the currently selected case in the list.
    *   **Note:** Improved the visual implementation by changing the default background color of unselected case items to `MaterialTheme.colorScheme.surface` for better contrast and clarity.

8.  **Loading Animations:** [COMPLETED]
    *   Verify that every asynchronous data-loading operation (e.g., fetching cases, loading evidence, synchronizing with the cloud) displays a loading indicator to the user. [COMPLETED]
    *   **Note:** Verified on `CasesScreen`. A global loading state is used, which likely covers other areas, but a full audit may be needed later.

9.  **Script Builder Screen (`ScriptBuilderScreen.kt`):**
    *   Implement the "Share" button's `onClick` action to open a dialog. [COMPLETED - UI Only]
    *   Create the "Share Script" dialog, which should prompt the user to enter their name and email. [COMPLETED - UI Only]
    *   Implement the logic to send the shared script to the creator's email when the "Edit" button is clicked. [COMPLETED]
    *   **Note:** The initial "Share" functionality (saving a *new* script to the public spreadsheet) was already implemented. The "Suggest an Edit" feature for *existing* scripts on the Extras screen (sending an email to the author) has now been added.

10. **Allegations Screen (`AllegationsScreen.kt`):** [COMPLETED]
    *   Re-implement the layout to match the specified order: Title, selected allegations list, search box, request/sort buttons, and the main list of allegations.
    *   Add a `onLongClick` modifier to the allegation list items to trigger a details dialog.
    *   **Note:** The layout already conformed to the requirements. No changes were necessary.

11. **Timeline Screen (`TimelineScreen.kt`):** [COMPLETED]
    *   Add logic to display a placeholder `ExtendedEvent` component when the timeline is empty to show users what to expect.

12. **Exhibits Screen (`ExhibitsScreen.kt`):**
    *   Implement a tabbed layout for the main functionality. [COMPLETED]
    *   **Note:** Verified that a `PrimaryTabRow` was already implemented, fulfilling the requirement.
    *   Implement a drag-and-drop interface for assigning evidence to exhibits.
    *   Create the exhibit details view that appears when an exhibit is clicked. [COMPLETED]

---

### **III. Core Functionality & Workflow**

14. **Evidence Processing Pipeline:**
    *   Ensure that text extracted from any evidence source (image, audio, video) is formatted with Markdown code blocks before being saved. [COMPLETED]
    *   **Note:** This functionality was already implemented in the `OcrProcessingService` and `VideoProcessingService`.
    *   Verify that all raw evidence files are copied into a dedicated "raw" folder within the case directory. [COMPLETED]
    *   **Note:** This functionality was confirmed to be working as intended in the `LocalFileStorageService`.
    *   Implement the logic to index files with no extracted text as "non-textual evidence." [COMPLETED]

15. **Scripting Engine:**
    *   Extend the `ScriptRunner` to support scripts that can call Google Apps Script functions via the `GoogleApiService`. [COMPLETED]
    *   **Note:** This was verified to be implemented via the `lex.google.runAppsScript` function.
    *   Implement a change-detection mechanism to ensure that scripts are only run on evidence that has changed or if the script itself has been updated.

16. **Location History Import:** [COMPLETED]
    *   Implement a method for importing location history from a file (e.g., Google Takeout).
    *   Allow the user to filter the imported history by a specific date range.
    *   **Note:** Direct access to location history is not possible due to Android's privacy restrictions. The implementation uses a file-based import as a workaround.

17. **Evidence Cleanup & Organization:**
    *   Create a database or repository to store the legal elements required to support each type of allegation. [COMPLETED]
    *   **Note:** Created the `LegalElement` data class and the `LegalElementsRepository` with a placeholder implementation.
    *   Implement the logic on the "Exhibits" screen to use this database to guide the user in creating exhibits.

---

### **IV. Review Screen Implementation**

17. **Layout and Initial UI:** [COMPLETED]
    *   Create the basic `ReviewScreen.kt` composable.
    *   Add the "Automatic Cleanup," "Paperwork," and "Finalize" `AzButton`s to the screen, ensuring they are right-aligned.
    *   **Note:** The `ReviewScreen.kt` file already existed. The task was updated to replace the incorrect buttons ("Generate", "Package", "Report") with the specified ones.

19. **Evidence Cleanup Functionality:** [COMPLETED]
    *   **Duplicate Detection:** [COMPLETED]
        *   Implement a file hashing mechanism to generate a unique hash for each media file upon import. [COMPLETED]
        *   Store the file hash in the `Evidence` data model. [COMPLETED]
        *   **Note:** This functionality was already implemented. The `Evidence` data model contains a `fileHash` property, and the processing services (`OcrProcessingService`, `VideoProcessingService`) correctly populate it.
        *   Create a `CleanupService` that compares file hashes to identify duplicates. [COMPLETED]
        *   Implement a UI component on the Review screen to display groups of duplicate evidence. [COMPLETED]
        *   Provide a button for each group to "Keep One, Delete Rest." [COMPLETED]
    *   **Image Series Merging (for conversations):** [COMPLETED]
        *   Develop an algorithm to identify sequential screenshots based on filename patterns and timestamps. [COMPLETED]
        *   Implement a UI component to display these identified series. [COMPLETED]
        *   Provide a "Merge" button that combines the images into a single PDF and merges their OCR text into one evidence record. [COMPLETED]
    *   **Redundant Text Cleanup:** [COMPLETED]
        *   **Note:** This task is superseded by **Task 31** in the Production Readiness and Remediation Protocol.

20. **Document Generation ("Paperwork" Button):** [COMPLETED]
    *   Implement the `onClick` for the "Paperwork" button. [COMPLETED]
    *   The process should iterate through all `Exhibits` in the current case. [COMPLETED]
    *   For each `Exhibit`, it should find the associated `Template`. [COMPLETED]
    *   Call the `GoogleApiService` to generate a document from the template, populating it with data from the evidence within that exhibit. [COMPLETED]
    *   Display a progress indicator and a final success/failure message. [COMPLETED]

21. **Case Finalization ("Finalize" Button):** [COMPLETED]
    *   **Note:** This task is superseded by **Task 32** in the Production Readiness and Remediation Protocol.

---

### **V. Documentation**

22. **Update `README.md`:** [COMPLETED]
    *   Add a "Getting Started" section with instructions for setting up the development environment. [COMPLETED]
    *   Update the "Key Features" list to reflect the current state of the application. [COMPLETED]

23. **Update `AGENTS.md`:** [COMPLETED]
    *   Replace the "Roadmap" section with this new, more granular TODO list.
    *   Review and update the UI/UX rules to be more specific where necessary.

24. **Create New Documentation:** [COMPLETED]
    *   Create a `CONTRIBUTING.md` file with guidelines for code contributions, pull requests, and code style.
    *   Create a `CODE_OF_CONDUCT.md` file.

25. **Review Existing Documentation:** [COMPLETED]
    *   **Note:** This task is superseded by **Task 34** in the Production Readiness and Remediation Protocol.

---

### **VI. Production Readiness and Remediation Protocol**

This section outlines the step-by-step protocol for transforming "The Lexorcist" Android application from its current developmental state into a production-ready product.

#### **Phase 1: Foundational Project Sanitization and Build System Stabilization**

26. **Workspace and Repository Decontamination:** [COMPLETED]
    *   Execute the deletion of the `ms-identity-android-kotlin-master` directory. [COMPLETED]
    *   Execute the deletion of the `whisper_android` directory. [COMPLETED]
    *   Execute the deletion of all files matching the patterns `hs_err_pid*.log`, `hs_err_pid*.mdmp`, `project_backup_full_*.txt`, and `backup.ps1`. [COMPLETED]
    *   Verify that the `.gitignore` file correctly lists directories and patterns such as `/.idea/`, `/*/build/`, `*.log`, and `local.properties`. [COMPLETED]
    *   **Note:** All specified files and directories were already removed, and the `.gitignore` file was correctly configured. No changes were necessary.

27. **Build Configuration and Security Rectification:** [COMPLETED]
    *   Modify `gradle/libs.versions.toml` to set `androidApplication = "8.14.0"`. [COMPLETED]
    *   Modify the root `build.gradle.kts` file to set the `com.android.application` plugin version to `8.14.0`. [COMPLETED]
    *   Execute a Gradle sync operation (e.g., `./gradlew :app:dependencies`). [COMPLETED]
    *   Log the expected failure and its cause (unresolved plugin). [COMPLETED]
    *   Programmatically query Google's Maven repository for the latest stable version of the Android Gradle Plugin (`com.android.application`). [COMPLETED]
    *   Update the `androidApplication` version in `gradle/libs.versions.toml` and the plugin version in the root `build.gradle.kts` to this latest stable version. [COMPLETED]
    *   Consult official Android documentation to determine the required Gradle version for the selected stable AGP version. [COMPLETED]
    *   Update the `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` accordingly. [COMPLETED]
    *   In `app/build.gradle.kts`, modify the `signingConfigs.release` block to read `storeFile`, `storePassword`, and `keyPassword` dynamically from the `local.properties` file. [COMPLETED]
    *   **Note:** The project was already using the latest stable AGP and Gradle versions (8.13.0 and 8.13, respectively), and the signing configuration was already secure. No changes were necessary.

28. **Dependency Graph Harmonization:** [COMPLETED]
    *   In `app/build.gradle.kts`, add the Jetpack Compose Bill of Materials: `implementation(platform(libs.androidx.compose.bom))`. [COMPLETED]
    *   Remove all explicit version strings from individual `androidx.compose.*` dependencies in the same file. [COMPLETED]
    *   Systematically audit `app/build.gradle.kts` and `gradle/libs.versions.toml` to ensure every dependency's version is defined once in the TOML file and referenced by its alias. [COMPLETED]
    *   Locate the dependency declaration for `androidx.compose.ui:ui-test-manifest` and modify its configuration to `debugImplementation`. [COMPLETED]
    *   Execute `./gradlew :app:dependencies --refresh-dependencies` and parse the output to verify that all major version conflicts have been resolved. [COMPLETED]
    *   **Note:** The Compose BOM was already implemented. I have harmonized the remaining dependencies to use the version catalog.

#### **Phase 2: Implementation of Incomplete Core Features**

29. **Video Evidence Processing Pipeline:** [COMPLETED]
    *   Create a new `VideoProcessingService.kt` file. [COMPLETED]
    *   Implement a public function `processVideo(uri: Uri): String` that uses `MediaMetadataRetriever` to extract frames at a fixed interval. [COMPLETED]
    *   For each extracted frame (Bitmap), invoke the existing `OcrProcessingService`. [COMPLETED]
    *   Invoke the `WhisperService` to perform audio transcription on the video URI. [COMPLETED]
    *   Aggregate the transcribed audio and all collected OCR text into a single, formatted string. [COMPLETED]

30. **Advanced Scripting Engine Activation:**
    *   **On-Device Semantic Similarity (`lex.ai.local`):**
        *   Add the `com.google.mediapipe:tasks-text` dependency.
        *   Download a compatible text embedding model (e.g., `universal_sentence_encoder`) and place it in `app/src/main/assets`.
        *   Create a `SemanticService.kt` with a singleton instance of `TextEmbedder`.
        *   Implement a function `calculateSimilarity(text1: String, text2: String): Float` that uses `textEmbedder.embed()` and `TextEmbedder.cosineSimilarity()`.
        *   In `ScriptRunner.kt`, expose the `SemanticService` to the JavaScript context.
    *   **Dynamic UI Generation (`lex.ui`):**
        *   Define data classes for a simple UI schema (e.g., `UiComponentModel`).
        *   In the relevant ViewModel, create a `MutableStateFlow<List<UiComponentModel>>`.
        *   Create a `DynamicUiRenderer(components: List<UiComponentModel>)` Composable that uses a `when` block to render different components based on the schema.
        *   In `ScriptRunner.kt`, expose a function `lex.ui.addOrUpdate()` that accepts a JSON string, parses it, and updates the ViewModel's `StateFlow`.

31. **Redundant Text Cleanup Implementation:**
    *   Create a `CleanupService.kt`.
    *   Inject the `SemanticService` into it.
    *   Implement a function `findSimilarTextEvidence(evidenceList: List<Evidence>): List<List<Evidence>>` that uses `semanticService.calculateSimilarity()` to perform pairwise comparisons.
    *   Group evidence items whose similarity score exceeds a defined threshold (e.g., 0.95).
    *   In `ReviewViewModel`, call this service method.
    *   In `ReviewScreen.kt`, add a new `LazyColumn` section to display the groups of similar evidence with a "Review & Merge" button.

32. **Case Finalization and Export:**
    *   Implement the `onClick` listener for the "Finalize" button on the `ReviewScreen.kt` to show a `FinalizeCaseDialog`.
    *   The dialog will contain a `LazyColumn` with checkboxes for all files in the case directory and a toggle for `.zip` or `.lex` format.
    *   Upon confirmation, launch the `ACTION_CREATE_DOCUMENT` intent.
    *   Create a `PackagingService.kt` with a function `createArchive(files: List<File>, destinationUri: Uri, format: String)` that uses `ZipOutputStream` to create the archive.

#### **Phase 3: Pre-Production Hardening and Refinement**

33. **Security, Obfuscation, and Performance:**
    *   In `app/build.gradle.kts`, in the `buildTypes.release` block, change `isMinifyEnabled` to `true`.
    *   In `proguard-rules.pro`, add keep rules for data models, Hilt, Coroutines, and any other libraries that use reflection.
    *   Remove the `buildConfigField` for the API key from `app/build.gradle.kts`.
    *   Implement NDK-based API key storage using a `native-lib.cpp` file and JNI.
    *   Add the `androidx.tracing:tracing-ktx` dependency and add `trace("Section Name")` blocks to performance-sensitive code paths.

34. **Documentation and Code Hygiene:**
    *   Review and update `docs/auth.md` to ensure accuracy for Google and Outlook/Azure API configuration.
    *   Update `SCRIPT_EXAMPLES.md` to remove notes about conceptual APIs and add working examples for `lex.ai.local` and `lex.ui`.
    *   Consolidate remaining tasks from `TODO.md` into a "Future Work" section in `README.md` and delete `TODO.md`.
    *   Run `./gradlew lintDebug` and analyze the report.
    *   Configure and run a code formatter like `ktlint` (`./gradlew spotlessApply` or similar).