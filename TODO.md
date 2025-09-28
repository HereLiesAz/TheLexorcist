# Project TODO List

This document outlines the features, fixes, and enhancements required to complete "The Lexorcist" application.

---

### **I. High-Priority Roadmap Items**

1.  **Cloud Synchronization:**
    *   **On App Close:** Implement a robust mechanism to trigger a full synchronization of the local `lexorcist_data.xlsx` file and the case folder with the selected cloud provider (Google Drive, etc.) when the application is closed.
    *   **On App Load:** Implement a synchronization check upon application startup to ensure the local data is consistent with the latest version in the cloud. [COMPLETED]
    *   **Note:** The `GoogleDriveCloudStorageProvider` was fully implemented to make the existing `SyncManager` logic functional.

2.  **Video Evidence Processing:**
    *   **Frame Extraction:** Implement a service to extract individual frames from a video file at a specified interval (e.g., every 5 seconds).
    *   **Visual Text Recognition:** For each extracted frame, run the existing `OcrProcessingService` to perform OCR and extract any visible text.
    *   **Audio Transcription:** Integrate the existing audio transcription service to process the video's audio track.
    *   **Evidence Aggregation:** Combine the transcribed audio and the text from all frames into a single, comprehensive evidence record linked to the original video file.

3.  **Editable Audio Transcripts:**
    *   **UI Implementation:** Create a new screen or dialog that displays an audio transcript in an editable text field.
    *   **Edit Tracking:** When an edit is made, prompt the user for a reason for the change.
    *   **Data Storage:** Store the original transcript and a log of all edits (including the change, timestamp, and reason) in the evidence record.

4.  **Transparent Progress Reporting:**
    *   **Detailed Log View:** Enhance the existing progress reporting UI to include a scrollable, detailed log view that displays each step of the evidence processing pipeline (e.g., "Uploading file," "Running OCR," "Applying script 'Profanity Tagger'").
    *   **Structured Logging:** Refactor the logging services to provide more structured and user-friendly log messages.

---

### **II. UI/UX Enhancements & Fixes**

5.  **Component Right-Alignment:**
    *   Audit every screen (`CasesScreen`, `EvidenceScreen`, `SettingsScreen`, etc.) and ensure all UI components (Buttons, TextFields, Cards, etc.), but not the text within them, are right-aligned as per the UI/UX rules.

6.  **`AzButton` Conversion:**
    *   Systematically review all screens and replace any standard Material `Button`, `ToggleButton`, `FloatingActionButton`, or similar components with their `AzNavRail` library equivalents (`AzButton`, `AzToggle`, `AzCycler`).

7.  **Case List Highlighting:** [COMPLETED]
    *   In `CasesScreen.kt`, implement a visual indicator (e.g., changing the background color) to highlight the currently selected case in the list.
    *   **Note:** Improved the visual implementation by changing the default background color of unselected case items to `MaterialTheme.colorScheme.surface` for better contrast and clarity.

8.  **Loading Animations:**
    *   Verify that every asynchronous data-loading operation (e.g., fetching cases, loading evidence, synchronizing with the cloud) displays a loading indicator to the user. [COMPLETED]
    *   **Note:** Verified on `CasesScreen`. A global loading state is used, which likely covers other areas, but a full audit may be needed later.

9.  **Script Builder Screen (`ScriptBuilderScreen.kt`):**
    *   Implement the "Share" button's `onClick` action to open a dialog. [COMPLETED - UI Only]
    *   Create the "Share Script" dialog, which should prompt the user to enter their name and email. [COMPLETED - UI Only]
    *   Implement the logic to send the shared script to the creator's email when the "Edit" button is clicked.

10. **Allegations Screen (`AllegationsScreen.kt`):**
    *   Re-implement the layout to match the specified order: Title, selected allegations list, search box, request/sort buttons, and the main list of allegations.
    *   Add a `onLongClick` modifier to the allegation list items to trigger a details dialog.

11. **Timeline Screen (`TimelineScreen.kt`):**
    *   Add logic to display a placeholder `ExtendedEvent` component when the timeline is empty to show users what to expect.

12. **Exhibits Screen (`ExhibitsScreen.kt`):**
    *   Implement a tabbed layout for the main functionality.
    *   Implement a drag-and-drop interface for assigning evidence to exhibits.
    *   Create the exhibit details view that appears when an exhibit is clicked.

---

### **III. Core Functionality & Workflow**

14. **Evidence Processing Pipeline:**
    *   Ensure that text extracted from any evidence source (image, audio, video) is formatted with Markdown code blocks before being saved.
    *   Verify that all raw evidence files are copied into a dedicated "raw" folder within the case directory.
    *   Implement the logic to index files with no extracted text as "non-textual evidence."

15. **Scripting Engine:**
    *   Extend the `ScriptRunner` to support scripts that can call Google Apps Script functions via the `GoogleApiService`.
    *   Implement a change-detection mechanism to ensure that scripts are only run on evidence that has changed or if the script itself has been updated.

16. **Evidence Cleanup & Organization:**
    *   Create a database or repository to store the legal elements required to support each type of allegation.
    *   Implement the logic on the "Exhibits" screen to use this database to guide the user in creating exhibits.

---

### **IV. Review Screen Implementation**

17. **Layout and Initial UI:**
    *   Create the basic `ReviewScreen.kt` composable.
    *   Add the "Automatic Cleanup," "Paperwork," and "Finalize" `AzButton`s to the screen, ensuring they are right-aligned.

18. **Evidence Cleanup Functionality:**
    *   **Duplicate Detection:**
        *   Implement a file hashing mechanism to generate a unique hash for each media file upon import.
        *   Store the file hash in the `Evidence` data model.
        *   Create a `CleanupService` that compares file hashes to identify duplicates.
        *   Implement a UI component on the Review screen to display groups of duplicate evidence.
        *   Provide a button for each group to "Keep One, Delete Rest."
    *   **Image Series Merging (for conversations):**
        *   Develop an algorithm to identify sequential screenshots based on filename patterns and timestamps.
        *   Implement a UI component to display these identified series.
        *   Provide a "Merge" button that combines the images into a single PDF and merges their OCR text into one evidence record.
    *   **Redundant Text Cleanup:**
        *   Implement a text similarity algorithm to find evidence with highly similar text content.
        *   Display these as potential duplicates for manual review and merging.

19. **Document Generation ("Paperwork" Button):**
    *   Implement the `onClick` for the "Paperwork" button.
    *   The process should iterate through all `Exhibits` in the current case.
    *   For each `Exhibit`, it should find the associated `Template`.
    *   Call the `GoogleApiService` to generate a document from the template, populating it with data from the evidence within that exhibit.
    *   Display a progress indicator and a final success/failure message.

20. **Case Finalization ("Finalize" Button):**
    *   **Dialog UI:** Create a dialog that appears when "Finalize" is clicked.
    *   **File Selection:** The dialog must display a checklist of all files and folders within the case directory.
    *   **Export Options:** Include a toggle in the dialog for choosing the output format: `.zip` or `.lex`.
    *   **Packaging Logic:** Implement a service that collects all selected files and compresses them into the chosen archive format.
    *   **File Saving:** Use Android's Storage Access Framework to allow the user to choose a location to save the final archive file.

---

### **V. Documentation**

21. **Update `README.md`:**
    *   Add a "Getting Started" section with instructions for setting up the development environment.
    *   Update the "Key Features" list to reflect the current state of the application.

22. **Update `AGENTS.md`:**
    *   Replace the "Roadmap" section with this new, more granular TODO list.
    *   Review and update the UI/UX rules to be more specific where necessary.

23. **Create New Documentation:**
    *   Create a `CONTRIBUTING.md` file with guidelines for code contributions, pull requests, and code style.
    *   Create a `CODE_OF_CONDUCT.md` file.

24. **Review Existing Documentation:**
    *   Verify the accuracy of `GMAIL_API_SETUP.md` and `OUTLOOK_API_SETUP.md`.
    *   Expand `SCRIPT_EXAMPLES.md` with examples of the new AI and UI scripting capabilities.