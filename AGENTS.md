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

### Before You Begin...

1.  **Analyze the Full Project:** The core architecture for the above workflow is already in place. Familiarize yourself with `OcrViewModel.kt` (for image processing), `ScriptRunner.kt` (for the tagging engine), `GoogleApiService.kt` (for Sheets/Drive integration), and the `raw` resources folder (for Apps Script and HTML templates).
2.  **Prioritize Stability:** The app currently has build errors. Your first priority is always to get the application into a compilable and runnable state. Do not add new features until the existing ones are stable.
3.  **Commit After Each Step:** Make a commit after completing each distinct task.
4.  **Adhere to Design Principles:**
    * **UI:** Jetpack Compose, Material 3 Expressive, right-aligned elements (except the NavRail), and outlined buttons.
    * **Theme:** The color scheme is generated dynamically from a random seed color.
    * **Documentation:** All public members must have KDoc. Keep all markdown documentation (`README.md`, `TODO.md`, etc.) updated.

### Key Development Tasks

* **Fix Build Errors:** Address all compilation errors, such as the unresolved theme and icon references noted in the build logs.
* **Integrate `DataParser.kt`:** This file is currently unused. Integrate it into the evidence-processing pipeline so that it runs *before* the `ScriptRunner`. It should extract basic entities like dates, names, and locations, which can then be used by the script runner for more complex logic.
* **Enhance Metadata Extraction:** Modify the evidence capture process to extract EXIF data from images (e.g., creation date) and attempt to parse timestamps from message screenshots. This data should be populated in the `Evidence` object.
* **Refactor ViewModels:** The `MainViewModel.kt` is monolithic. Refactor its logic into the more specialized ViewModels that have already been created (`CaseViewModel`, `EvidenceViewModel`, `OcrViewModel`).

Do not change any code or documentation without being explicitly told to do so.
