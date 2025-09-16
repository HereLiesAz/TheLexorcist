# The Lexorcist - Development Roadmap

This document outlines the tasks required to bring The Lexorcist to a fully functional and stable state, realizing its core vision as an automated legal evidence management tool.

**Status Legend:** `[x]` = Done, `[/]` = Partially Done, `[ ]` = Not Started

---

### **Part 1: Stabilization and Core Integration**

These tasks are the highest priority and must be completed before adding new features. The primary goal is to get the application into a runnable, stable state and ensure all existing architectural components are properly integrated.

- [x] **1.1. Fix Build Errors:**
    - The project now compiles and runs successfully.

- [x] **1.2. Integrate `DataParser.kt`:**
    - The `DataParser.kt` is now used in the evidence processing pipeline.

- [x] **1.3. Refactor Monolithic `MainViewModel`:**
    - The `MainViewModel` has been refactored, and the logic has been moved to more specialized ViewModels.

### **Part 2: Feature Enhancement & Vision Alignment**

Once the app is stable, these tasks will enhance the core workflow and bring the application closer to the user's vision.

- [x] **2.1. Implement Enhanced Metadata Extraction:**
    - The app now extracts EXIF data from images and uses regex to find timestamps in screenshots.

- [x] **2.2. Solidify the Scripting Engine:**
    - The script from the `ScriptEditorScreen` is now used by the `ScriptRunner`.

- [x] **2.3. Full CRUD for Evidence:**
    - The app now supports full CRUD operations for evidence, and the changes are reflected in the spreadsheet.

### **Part 3: Polish and User Experience**

- [ ] **3.1. Add Comprehensive Tests:**
    - The project has very few tests.
    - **Action:** Add unit tests for all ViewModels, repositories, and the `ScriptRunner`.
    - **Action:** Add integration tests for the database and Google API interactions.

- [x] **3.2. Refine UI and Error Handling:**
    - The UI has been updated to be more consistent and user-friendly.
    - Loading indicators and more specific error messages have been added.

- [x] **3.3. Complete UI Screens:**
    - All screens in the navigation rail are now fully functional.

### **Part 4: Document Generation**

- [ ] **4.1. Implement Document Generation:**
    - This feature is not yet implemented.
    - **Action:** Implement the document generation feature using the Google Apps Script backend and HTML templates.
