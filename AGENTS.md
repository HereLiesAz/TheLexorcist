# Agent Instructions

This file contains instructions for AI agents working on this codebase.

### Your Goal

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
2. All buttons need to be transparent with a stroke the color of the theme. Text inside the buttons should be that same color. 
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

---

### Before You Begin...

1.  **Analyze the Full Project:** The core architecture for the above workflow is already in place. Familiarize yourself with `OcrViewModel.kt` (for image processing), `ScriptRunner.kt` (for the tagging engine), `GoogleApiService.kt` (for Sheets/Drive integration), and the `raw` resources folder (for Apps Script and HTML templates).
2.  **Get Code Reviews Often:** If you are struggling, if you have a question, if you'd like to know how you're doing, get a code review. A code review must be run and heeded before any commit. If you disagree with the code review, get another code review. 
3.  **Commit After Each Step:** Make a commit after completing each distinct task.
4.  **Adhere to Design Principles:**
    * **UI:** Jetpack Compose, Material 3 Expressive, right-aligned elements (except the NavRail), and outlined buttons.
    * **Theme:** The color scheme is generated dynamically from a random seed color. This should not be changed.
    * **Documentation:**
    * **Code Style:** This project uses `ktlint` to enforce a consistent code style.
