# The Lexorcist - Development Roadmap

This document outlines the tasks required to bring The Lexorcist to a fully functional and stable state, realizing its core vision as an automated legal evidence management tool.

**Status Legend:** `[x]` = Done, `[/]` = Partially Done, `[ ]` = Not Started

---

### **Part 1: Stabilization and Core Integration**

These tasks are the highest priority and must be completed before adding new features. The primary goal is to get the application into a runnable, stable state and ensure all existing architectural components are properly integrated.

- [ ] **1.1. Fix Build Errors:**
    - The project currently fails to compile due to unresolved references (`TheLexorcistTheme`, `Icons.filled`).
    - **Action:** Investigate the `MainActivity.kt`, `ScriptEditorActivity.kt`, and `CasesScreen.kt` files to correct the theme and icon import/reference issues identified in the build log. Ensure the project can be built and deployed to a device or emulator.

- [ ] **1.2. Integrate `DataParser.kt`:**
    - The `DataParser.kt` file, which is designed to extract generic entities like names, dates, and locations, is currently unused.
    - **Action:** Modify the evidence processing pipeline in the ViewModels (`OcrViewModel`/`MainViewModel`) to call `DataParser.tagData()` on the extracted OCR text. The results of this parsing should be made available to the `ScriptRunner` so that user-defined scripts can leverage this pre-parsed information.

- [ ] **1.3. Refactor Monolithic `MainViewModel`:**
    - The `MainViewModel` is overly large and handles logic that belongs in more specialized ViewModels.
    - **Action:** Systematically move logic from `MainViewModel` into `AuthViewModel`, `CaseViewModel`, `EvidenceViewModel`, and `OcrViewModel`. For example, all OCR and image review logic should reside solely in `OcrViewModel`. Case creation and selection should be in `CaseViewModel`.

### **Part 2: Feature Enhancement & Vision Alignment**

Once the app is stable, these tasks will enhance the core workflow and bring the application closer to the user's vision.

- [ ] **2.1. Implement Enhanced Metadata Extraction:**
    - The current implementation uses the system time when creating evidence. The vision requires extracting actual dates from the source material.
    - **Action:** Add a library or custom code to read EXIF data from images to get the "Date Taken."
    - **Action:** For screenshots of messages, enhance `DataParser.kt` with regex patterns to identify and extract timestamps commonly found in messaging apps.
    - **Action:** The extracted date should be the default `documentDate` for a new `Evidence` object, with the current time used as a fallback.

- [ ] **2.2. Solidify the Scripting Engine:**
    - The `ScriptRunner.kt` is functional but needs to be fully integrated with the UI.
    - **Action:** Ensure that the script saved in the `ScriptEditorScreen` is the one used by `ScriptRunner` when processing new evidence.
    - **Action:** Provide clear documentation and examples within the app (perhaps in a help section or placeholder text in the editor) on how to write scripts to tag evidence based on keywords (e.g., `if (evidence.content.includes("threat")) { parser.tags.push("Threat"); }`).

- [ ] **2.3. Full CRUD for Evidence:**
    - While evidence can be added, the full lifecycle is not complete.
    - **Action:** Ensure that editing or deleting evidence in the `DataReviewScreen` correctly updates the corresponding row in the Google Sheet.
    - **Action:** When evidence is updated, re-run the script runner against it to update its tags.

### **Part 3: Polish and User Experience**

- [ ] **3.1. Add Comprehensive Tests:**
    - The project has very few tests.
    - **Action:** Add unit tests for all ViewModels, repositories, and the `ScriptRunner`.
    - **Action:** Add integration tests for the database and Google API interactions.

- [ ] **3.2. Refine UI and Error Handling:**
    - Provide more specific error messages for API failures, script errors, or parsing issues.
    - Add loading indicators for all network and long-running operations.

- [ ] **3.3. Complete UI Screens:**
    - Fully implement the functionality for all screens outlined in the navigation rail, ensuring they are driven by the refactored ViewModels.
