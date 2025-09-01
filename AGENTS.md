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

### **CRITICAL ARCHITECTURAL DIRECTIVE**

 **DO NOT USE LOCAL DATABASES.**

 Under **NO CIRCUMSTANCES** should you attempt to implement a local database, such as **Room** or **SQLite**. The application's core design relies **exclusively** on the Google Sheets API for data storage.

 **This is a non-negotiable architectural constraint.**

 Any attempt to introduce a local database will be considered a fundamental misunderstanding of the project's design and will be rejected. This approach ensures that the user maintains full control and visibility of their data in a familiar format (Google Sheets) and simplifies the application by avoiding complex data synchronization logic.

 If a task appears to require data persistence, the solution **must** involve the Google Sheets API. If you are uncertain how to proceed, you must ask for clarification.

---

### UI and UX

1. Except for the AzNavRail, all components on every screen (text boxes, not text) need to be right aligned.
2. The topmost component on every screen that scrolls should have a default vertical initial position at halfway down the screen. When scrolling, the items should still scroll to the top of the screen. We're only talking about the topmost component's initial starting position.

---

### Before You Begin...

1.  **Analyze the Full Project:** The core architecture for the above workflow is already in place. Familiarize yourself with `OcrViewModel.kt` (for image processing), `ScriptRunner.kt` (for the tagging engine), `GoogleApiService.kt` (for Sheets/Drive integration), and the `raw` resources folder (for Apps Script and HTML templates).
2.  **Prioritize Stability:** The app currently has build errors. Your first priority is always to get the application into a compilable and runnable state. Do not add new features until the existing ones are stable.
3.  **Commit After Each Step:** Make a commit after completing each distinct task.
4.  **Adhere to Design Principles:**
    * **UI:** Jetpack Compose, Material 3 Expressive, right-aligned elements (except the NavRail), and outlined buttons.
    * **Theme:** The color scheme is generated dynamically from a random seed color.
    * **Documentation:**
