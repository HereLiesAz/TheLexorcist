# Agent Instructions

This file contains instructions for AI agents working on this codebase.

### Your Goal
Do not run a build or test without explicitly being told to do so. Call for a code review every 10 changes. Could be across 10 different files, could be all on one file. But every 10 changes, you get a code review. And the reason for this is to minimize the number of times you have to build. You are only allowed one build, IF ANY AT ALL. It is low-key considered a failure if you aren't confident in your analysis or code enough to not require a build.
Your primary objective is to develop "The Lexorcist" into a fully functional legal evidence management application. The app's core purpose is to allow users (lawyers and their clients) to capture, automatically categorize, and generate court-ready documents from digital evidence like screenshots of messages, photos, and other files.

### The Workflow to Enable

1.  A user captures or imports an image (e.g., a screenshot of a threatening text message).
2.  The app performs OCR to extract the text.
3.  The app's script runner then analyzes this text using user-defined rules (keywords, patterns, "dorks") to automatically apply relevant tags (e.g., "threat," "slander," "hate speech").
4.  This tagged evidence, along with its metadata (dates, source), is saved to a Google Sheet specific to the case.
5.  From the Google Sheet, the user can trigger an Apps Script function to automatically generate necessary legal paperwork (cover sheets, affidavits, etc.) using predefined HTML templates.

---

### **CRITICAL ARCHITECTURAL DIRECTIVE: Local-First**

The application follows a **local-first** data management strategy. All user data, including case information and evidence, is stored in a single spreadsheet file (`lexorcist_data.xlsx`) within the application's folder, and synchronized with cloud services. Upon loading, the app must attempt synchronization to ensure it is using the lastest saved state.

This approach ensures that the app is always functional, even when offline.

The file google-services.template.json located at the root of the /app/ folder should be used to create the google-services.json file you will need to build the project.

---

### UI and UX

1. Except for the AzNavRail, all components on every screen (text boxes, not text) need to be right aligned.
2. All buttons should be AzButtons, imported from the com.hereliesaz.AzNavRail library. If you find a button (or a toggle, or a small list of options) or a FAB that is not an AzButton (or AzToggle, or AzCycler), notify the developer immediately. It MUST be changed.
3. When it comes to the DSL-style AzNavRail library, NEVER, NEVER, NEVER, i.e., do NOT fucking EVER guess how it works. Read the goddamned documentation, and FOLLOW THE INSTRUCTIONS, you lazy piece of shit.  https://github.com/HereLiesAz/AzNavRail
3. All changes should be saved immediately and automatically to the case's folder. The folder must be saved and syncronized onto Google Drive often, but synchronization must always be attempted when the app is closed. That means the app will need to be explicit about being closed.
4. When the user selects a photo, audio, or video file as evidence, text from the image/audio should be automatically parsed and formatted with markdown code, and saved.
5. All raw evidence files should be copied into a raw evidence folder.
6. The progress of loading the file, finding text, formatting with markdown, and adding it to the case and indexing the evidence, needs to be transparent to the user. Progress should be shown, the current task summarized over the progress bar, and a detailed live log should be displayed below.
7. If no text is found, then the file is saved to the raw evidence folder, indexed as non-textual evidence, and the user should be informed.
8. For audio files, text is parsed from the transcript. Transcripts must be editable, tagging edits with a timestamp and reason for the edit.
9. For video files, text from video should be parsed both from its audio transcript and what's visible in the video.
10. ALL photos and media files added as evidence must be saved in the case folder, indexed, and linked in the spreadsheet.
11. When the user taps a Case in the case list, it needs to highlight so they can see that a case was selected. Selecting a case should cause it to be loaded--its evidence previously added, the text parsed from that evidence, the allegations previously selected, the templates and scripts last loaded or edited or created for that case, etc. All screens should be in their most recent previous state.
12. All instances where data is being loaded should be indicated by a loading animation.
13. The user should be able to select cloud services in Settings.
14. On the Script Builder screen, when the Share button is pushed, whatever script is in the editor tab, described in the description tab, and has its name in the Script Title box, is the listing to appear on the Extras screen (addon browser). Upon pressing the Share button, a dialog pops up for the user to set their name and email.
15. If the user ever wants to edit the shared script (or template) AFTER it's already shared, an edit button must be provided, which automatically sends that script (or template) to the creator's email (limit once per day). The user emails back their updated version, and that's as much security in ownership as is needed.
16. The Allegations screen layout should be "Allegations", below that should be a list of the selected allegations that are applied to the case, below that is the search box, then the request button is next to the sort-by option on the same row, and lastly, the complete list of available allegations to select from. Long pressing an allegation brings up an Allegation Details dialog.
17. If there's nothing to display on the Timeline screen, then it should still have the screen title, "Timeline", displayed at the top, and there MUST be a placeholder extended event displayed until at least one piece of evidence is added, so the user knows what to expect from that screen.
18. The Script builder should support scripts that interact with the way the app interacts with data AND with Google Apps Scripts.
19. The Review screen must provide a functioning button to initiate an automatic evidence cleanup. This should be as automatic as possible, but must be possible to do manually as well. For example, if someone takes a series of screenshots to capture the entirety of a conversation, you wouldn't submit each screenshot as a separate piece of evidence. These need to be combined as a single piece of evidence. Another issue for auto cleaning is duplicate evidence files and repeated text.
20. Once cleaned up, Evidence must then be collected into exhibits. These are thematic groupings to prove at least one of the required qualifications of an allegation. This does mean we must maintain a database of how to legally support each specific allegation.
21. The Review screen is also where the user generates documents and files. The "Paperwork" button generates all the documents needed for the Case's exhibits using the templates the user loaded or created on the Templates screen. The "Finalize" button is a .zip generator that brings up a dialog for the user to choose what all in the Case folder should be included in the .zip file. At the bottom of the dialog, the user can choose to generate into the .zip or the .lex format (which is really just a .zip file with the extension changed to .lex, but is the file type that The Lexorcist immediately recognizes as a Case file/archive/backup to be imported on the Cases screen).
22. On the script builder screen, there are three tabs. One is for writing or editing a script. One is a description of the script as it currently is, which is programmatically generated AND the user can edit to add to the description. And the last tab is a list of all the scripts that will be applied on the next screen load.
23. On every screen load, the app will run all active scripts on all evidence, but only if the evidence or the script has changed since the last run.
24. On the Exhibits screen, under "Exhibits" at the top, is a tabbed box where most of this screen's functionality will take place. Under that are a series of automatic cleanup and organization tools to handle the assumed fragmented nature of evidence files, like duplicate files, files that need to be combined (like a series of screenshots of a conversation, for example), and evidence assessment.
25. On the Exhibits screen, under the Assign tab, is a scrollable list of pertinent exhibit types are displayed, determined by the allegations the user has selected. Next to that, all of the unassigned evidences are also displayed. The user is to drag and drop the evidences onto the exhibit it applies to. Each Exhibit that contains evidence is to be kept at the top of the list of exhibits.
26. If the user clicks on an Exhibit then its description, a list of pertinent evidence types, the Exhibit's completeness and influence factor, and a list of that Exhibit's contents are displayed. Tapping a piece of evidence here allows the user to remove it from that Exhibit, tag it, or do other unknown things via scripting.


---

### Roadmap & Task Analysis

This section provides a detailed breakdown of the items from `TODO.md`, along with an analysis of the repository for potential missed items or considerations.

#### **I. High-Priority Roadmap Items**

1.  **Cloud Synchronization:**
    *   **Goal:** Ensure the local `lexorcist_data.xlsx` file and associated case folders are synchronized with a cloud provider on app load and app close.
    *   **Analysis:** The application already has a `SyncManager` and a `StorageService` interface with a `synchronize` method. An `AppLifecycleObserver` is also in place.
    *   **Steps:**
        1.  **On App Load:** Inject the `StorageService` into `LexorcistApplication` and invoke the `storageService.synchronize()` method within the `onCreate` lifecycle event. This ensures the app starts with the latest data.
        2.  **On App Close:** The existing `AppLifecycleObserver` already calls `storageService.synchronize()` in its `onStop` method. This correctly handles the app-close scenario. No changes are needed here.
        3.  **Provider Selection:** The `synchronize` method correctly uses the `SettingsManager` to determine which cloud provider to use, making the implementation flexible.

2.  **Video Evidence Processing:**
    *   **Goal:** Extract text from both the video frames (OCR) and the audio track (transcription) and combine them into a single evidence record.
    *   **Analysis:** The project has an `OcrProcessingService` and multiple `TranscriptionService` implementations. A `VideoProcessingService` exists but is not fully implemented.
    *   **Steps:**
        1.  Implement the frame extraction logic in `VideoProcessingService` using Android's `MediaMetadataRetriever`.
        2.  For each extracted frame, pass the bitmap to the existing `OcrProcessingService`.
        3.  Integrate one of the existing transcription services (e.g., `VoskTranscriptionService`) to process the video's audio.
        4.  Aggregate the text from all sources into a single `Evidence` object, linking it to the original video file.
        5.  Update the `Evidence` data model if necessary to properly store aggregated video data.

3.  **Editable Audio Transcripts:**
    *   **Goal:** Allow users to edit audio transcripts and log the changes.
    *   **Analysis:** The `Evidence` data model needs to be updated. A new UI screen or dialog is required. The `LocalFileStorageService` will need to be updated to handle saving the edits.
    *   **Steps:**
        1.  Create a new screen or dialog for transcript editing.
        2.  Add a `transcriptEdits` list to the `Evidence` data class.
        3.  Implement the `onSave` logic to prompt for a reason and store the original and edited transcript along with the reason and timestamp.
        4.  Update `LocalFileStorageService` to read and write these edits from a new "TranscriptEdits" sheet in the `lexorcist_data.xlsx` file.

4.  **Transparent Progress Reporting:**
    *   **Goal:** Provide detailed, step-by-step progress to the user during evidence processing.
    *   **Analysis:** A `LogService` and a `ProcessingState` model exist but are not fully utilized. The UI needs to be enhanced to display these logs.
    *   **Steps:**
        1.  Refactor the evidence processing services (`OcrProcessingService`, `VideoProcessingService`) to emit structured log entries via the `LogService` at each major step.
        2.  Enhance the relevant UI screens to include a `LazyColumn` that displays the `logMessages` from the `CaseViewModel`.

#### **II. UI/UX Enhancements & Fixes**

This section involves a systematic audit and update of all UI screens located in `app/src/main/java/com/hereliesaz/lexorcist/ui/`.

*   **Items 5 & 6 (Right-Alignment & `AzButton` Conversion):** A full audit of each screen file is required to replace standard Material components with their `AzNavRail` library equivalents and ensure right-alignment for all components except the `AzNavRail` itself.
*   **Items 7-12 (Screen-Specific UI Logic):** These items require targeted changes in their respective composable files (`CasesScreen.kt`, `ScriptBuilderScreen.kt`, etc.) to implement specific UI behaviors like list highlighting, placeholder content, and dialogs.

#### **III. Core Functionality & Workflow**

1.  **Automatic Saving:**
    *   **Goal:** Persist all data changes to the local spreadsheet immediately.
    *   **Analysis:** The `LocalFileStorageService` is the single source of truth for file I/O. The main issue is that some ViewModels use `SharedPreferences` instead of the `CaseRepository` for persistence.
    *   **Steps:**
        1.  Refactor `CaseViewModel`'s `onPlaintiffsChanged`, `onDefendantsChanged`, and `onCourtSelected` methods to update the `Case` object and call `caseRepository.updateCase()`.
        2.  Remove the now-redundant `saveCaseInfoToSharedPrefs` method.
        3.  Audit other ViewModels to ensure all data-mutating actions call the appropriate repository method.

2.  **Evidence & Scripting Engine:**
    *   **Evidence Pipeline:** This involves ensuring all text extraction services wrap their output in Markdown and that raw files are copied to a "raw" folder. This logic should be centralized in the evidence import process.
    *   **Scripting Engine:** The `ScriptRunner` needs to be extended to support calls to `GoogleApiService`, and a change-detection mechanism (likely using file/content hashes) must be implemented to avoid redundant script executions.

#### **IV. Review Screen & Documentation**

These sections are well-defined in `TODO.md`. They involve creating a new `ReviewScreen.kt` with complex cleanup and export functionality, and updating all documentation files (`README.md`, `AGENTS.md`, etc.).

***

### **Repository Analysis: Missed Items & Considerations**

1.  **Absence of a Testing Strategy:** The `TODO.md` file and the codebase have no mention of unit or instrumentation tests. For a project of this complexity, especially one that handles user data and file I/O, a robust testing strategy is critical to ensure reliability and prevent regressions.
2.  **ViewModel Complexity:** `CaseViewModel.kt` is extremely large and manages state for many different features (Cases, Evidence, Allegations, UI state, etc.). This makes it difficult to maintain and debug. **I strongly recommend refactoring it into smaller, more focused ViewModels.**
3.  **Error Handling and User Feedback:** While there are some mechanisms for error handling (`_errorMessage` StateFlow), they are not applied consistently. A global strategy for reporting errors, success messages, and recoverable authentication issues to the user would significantly improve the user experience.
4.  **Periodic Synchronization:** `AGENTS.md` states that data should be synchronized "often," but the current implementation only syncs on app start and stop. A periodic background sync using `WorkManager` should be considered to prevent data loss in case of a crash.

***

### **Proposed Plan**

1.  **Phase 1: High-Priority Tasks & Core Functionality**
    *   Implement the **Cloud Synchronization** on app load as described above.
    *   Implement the **Automatic Saving** logic in `CaseViewModel`.
    *   Implement the **Video Evidence Processing** service.
    *   Implement **Editable Audio Transcripts**, including the UI and data model changes.

2.  **Phase 2: UI/UX Overhaul**
    *   Systematically go through all screens to address **Right-Alignment** and **`AzButton` Conversion**.
    *   Implement the screen-specific UI logic for **Case List Highlighting**, **Loading Animations**, and enhancements to the **Allegations, Timeline, and Script Builder screens**.

3.  **Phase 3: Advanced Features & Review Screen**
    *   Implement the **Review Screen**, starting with the layout and cleanup functionality (duplicate detection, image series merging).
    *   Implement the **Document Generation** and **Case Finalization** features.

4.  **Phase 4: Documentation & Testing**
    *   Update all project documentation as specified in `TODO.md`.
    *   **(New)** Develop and implement a testing strategy, adding unit tests for ViewModels and services.

---

### Before You Begin...

1.  **Analyze the Full Project:** The core architecture for the above workflow is already in place. Familiarize yourself with `OcrViewModel.kt` (for image processing), `ScriptRunner.kt` (for the tagging engine), `GoogleApiService.kt` (for Sheets/Drive integration), `SettingsViewModel.kt` (for user settings), and the `raw` resources folder (for Apps Script and HTML templates).
2.  **Get Code Reviews Often:** If you are struggling, if you have a question, if you'd like to know how you're doing, get a code review. A code review must be run and heeded before any commit. If you disagree with the code review, get another code review.
3.  **Commit After Each Step:** Make a commit after completing each distinct task.
4.  **Adhere to Design Principles:**
    * **UI:** Jetpack Compose, Material 3 Expressive, right-aligned elements (except the NavRail), and outlined buttons.
    * **Theme:** The color scheme is generated dynamically from a random seed color. This should not be changed.
    * **Documentation:**
    * **Code Style:** This project uses `ktlint` to enforce a consistent code style.
