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
2. All buttons should be AzButtons, imported from the com.hereliesaz.AzNavRail library. If you find a button (or a toggle, or a small list of options) that is not an AzButton (or AzToggle, or AzCycler), notify the developer immediately. It MUST be changed.  
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
22. This app must implement 


---

### Roadmap

This section outlines the remaining tasks to be completed. Please address these in the order listed.

1. **Complete Cloud Sync:** Implement robust cloud synchronization, especially on app close.
2. **Complete Video Processing:** Implement text extraction from video frames (visual text).
3. **Editable Transcripts:** Implement the UI and logic for editing audio transcripts with timestamps and reasons.
4. **Full Progress Transparency:** Enhance the progress reporting to include a more detailed and structured log view.

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
