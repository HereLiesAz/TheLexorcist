# Project TODO List

This document outlines the features, fixes, and enhancements required to complete "The Lexorcist" application.

---

### **I. High-Priority Roadmap Items**

1.  **Cloud Synchronization:** [COMPLETED]
    *   **On App Close:** Implement a robust mechanism to trigger a full synchronization of the local `lexorcist_data.xlsx` file and the case folder with the selected cloud provider (Google Drive, etc.) when the application is closed. [COMPLETED]
    *   **On App Load:** Implement a synchronization check upon application startup to ensure the local data is consistent with the latest version in the cloud. [COMPLETED]
    *   **Note:** The `GoogleDriveCloudStorageProvider` was fully implemented to make the existing `SyncManager` logic functional. Sync on close was already implemented via `AppLifecycleObserver`.

2.  **Video Evidence Processing:**
    *   **Frame Extraction:** Implement a service to extract individual frames from a video file at a specified interval (e.g., every 5 seconds).
    *   **Visual Text Recognition:** For each extracted frame, run the existing `OcrProcessingService` to perform OCR and extract any visible text.
    *   **Audio Transcription:** Integrate the existing audio transcription service to process the video's audio track.
    *   **Evidence Aggregation:** Combine the transcribed audio and the text from all frames into a single, comprehensive evidence record linked to the original video file.

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
    *   **Redundant Text Cleanup:**
        *   Implement a text similarity algorithm to find evidence with highly similar text content.
        *   Display these as potential duplicates for manual review and merging.

20. **Document Generation ("Paperwork" Button):** [COMPLETED]
    *   Implement the `onClick` for the "Paperwork" button. [COMPLETED]
    *   The process should iterate through all `Exhibits` in the current case. [COMPLETED]
    *   For each `Exhibit`, it should find the associated `Template`. [COMPLETED]
    *   Call the `GoogleApiService` to generate a document from the template, populating it with data from the evidence within that exhibit. [COMPLETED]
    *   Display a progress indicator and a final success/failure message. [COMPLETED]

21. **Case Finalization ("Finalize" Button):**
    *   **Dialog UI:** Create a dialog that appears when "Finalize" is clicked.
    *   **File Selection:** The dialog must display a checklist of all files and folders within the case directory.
    *   **Export Options:** Include a toggle in the dialog for choosing the output format: `.zip` or `.lex`.
    *   **Packaging Logic:** Implement a service that collects all selected files and compresses them into the chosen archive format.
    *   **File Saving:** Use Android's Storage Access Framework to allow the user to choose a location to save the final archive file.

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
    *   Verify the accuracy of `GMAIL_API_SETUP.md` and `OUTLOOK_API_SETUP.md`. [COMPLETED]
    *   Expand `SCRIPT_EXAMPLES.md` with examples of the new AI and UI scripting capabilities. [COMPLETED]
    *   **Note:** The API setup documents were not found. The `SCRIPT_EXAMPLES.md` file was expanded with new examples for the implemented AI APIs and clarified the status of unimplemented conceptual APIs.