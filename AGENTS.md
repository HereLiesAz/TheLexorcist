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
---

### Roadmap & Task Analysis

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
    *   Verify that every asynchronous data-loading operation (e.g., fetching cases, loading evidence, synchronizing with the cloud) displays a loading indicator to the user.

9.  **Script Builder Screen (`ScriptBuilderScreen.kt`):**
    *   Implement the "Share" button's `onClick` action to open a dialog.
    *   Create the "Share Script" dialog, which should prompt the user to enter their name and email.
    *   Implement the logic to send the shared script to the creator's email when the "Edit" button is clicked.

10. **Allegations Screen (`AllegationsScreen.kt`):**
    *   Re-implement the layout to match the specified order: Title, selected allegations list, search box, request/sort buttons, and the main list of allegations.
    *   Add a `onLongClick` modifier to the allegation list items to trigger a details dialog.

11. **Timeline Screen (`TimelineScreen.kt`):** [COMPLETED]
    *   Add logic to display a placeholder `ExtendedEvent` component when the timeline is empty to show users what to expect.

12. **Exhibits Screen (`ExhibitsScreen.kt`):**
    *   Implement a tabbed layout for the main functionality.
    *   Implement a drag-and-drop interface for assigning evidence to exhibits.
    *   Create the exhibit details view that appears when an exhibit is clicked.

13. **Templates Screen Court List:** [COMPLETED]
    *   **Problem:** The list of courts in `app/src/main/assets/jurisdictions.json` was incomplete.
    *   **Solution:** The `jurisdictions.json` file was updated to include a comprehensive list of U.S. federal, state, and territorial courts, ensuring existing court IDs were preserved to maintain data integrity.
    *   **Desired Result:** The "Court" dropdown on the Templates screen now displays a full list of jurisdictions for selection.

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

23. **Create New Documentation:** [COMPLETED]
    *   Create a `CONTRIBUTING.md` file with guidelines for code contributions, pull requests, and code style.
    *   Create a `CODE_OF_CONDUCT.md` file.

24. **Review Existing Documentation:**
    *   Verify the accuracy of `GMAIL_API_SETUP.md` and `OUTLOOK_API_SETUP.md`.
    *   Expand `SCRIPT_EXAMPLES.md` with examples of the new AI and UI scripting capabilities.
