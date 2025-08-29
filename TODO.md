# The Lexorcist - Comprehensive To-Do List

This document outlines the required tasks to develop The Lexorcist into a functional and clearly-defined legal evidence management tool. It is divided into two main parts: addressing the app's unclear purpose and tackling dysfunctional or incomplete features.

**Status Legend:** `[x]` = Done, `[/]` = Partially Done, `[ ]` = Not Started

---

### **Part 1: Address Unclear Purpose & Refocus on Legal Evidence**

The most significant issue is the disconnect between the app's stated purpose as a "legal evidence tracker" and the heavy emphasis on financial tracking in its documentation and feature planning.

- [ ] **1.1. Revise `README.md`:**
    - Rewrite the `README.md` to use general legal evidence terminology. Replace financial terms (e.g., "receipts", "vendors", "amounts") with broader terms like "documents," "exhibits," "artifacts," "photos," etc.
    - The features should be described in a way that reflects this broader scope (e.g., "OCR for text-based evidence" instead of for "financial documents").
    - **Note:** `README.md` still contains financial terminology.

- [ / ] **1.2. Align Planning Documents (`TODO.md`, `FUTURE_PLANS.md`):**
    - Rephrase or replace financially-focused tasks.
    - **Action:** Change "Expense Categorization" to "**Evidence Categorization**".
    - **Action:** Re-evaluate "Financial Analysis for Legal Cases" and consider a more general "**Timeline & Connection Analysis**" feature.
    - **Note:** This `TODO.md` has been updated, but `FUTURE_PLANS.md` still contains a mix of financial and legal terms and lists some implemented features as "future".

- [ / ] **1.3. Audit Codebase for Terminology:**
    - Perform a full audit of the source code (comments, variable names, UI strings) to ensure consistent, non-financial terminology is used.
    - **Action:** Generalize `DataParser.kt` to extract entities relevant to legal documents (names, dates, locations) rather than just "total" and "vendor."
    - **Note:** `DataParser.kt` is implemented with general-purpose parsing but is currently unused in the app. Other parts of the codebase might still use financial-specific terms.

---

### **Part 2: Address Dysfunctional and Incomplete Features**

The project is in a very early stage, with many critical features missing or non-functional.

#### **Critical 🎯**

- [ / ] **2.1. Implement Core Backend:**
    - **Define Data Models:** The `model` directory is empty. Define the core data models for `Case`, `Evidence` (with fields for type, date, description, tags, etc.), and other necessary entities.
    - **Implement Database:** The `db` directory is empty. Implement a Room database to provide offline storage for all data. This addresses the "Implement Offline Support" goal.
    - **Note:** Data models exist in `app/src/main/java/com/hereliesaz/lexorcist/model/`. The backend is implemented using **Google Sheets**, not a local Room database as suggested. The `db` directory is still empty.

- [ / ] **2.2. Implement Core Feature Logic:**
    - **Data Parsing:** The `DataParser.kt` is unused. Implement the logic to parse text extracted by the OCR. This is a critical missing feature.
    - **Spreadsheet Parsing:** The `parseSpreadsheetFile` function in `MainViewModel.kt` is a stub. Implement it to handle `.xls` and `.xlsx` files, mapping columns to evidence fields.
    - **Note:** `DataParser.kt` is implemented and has tests, but it is not called from the application code. The `parseSpreadsheetFile` function in `MainViewModel.kt` is implemented, not a stub.

- [ / ] **2.3. Build Out Placeholder UI Screens:**
    - **Timeline Screen:** Implement the UI and logic to visualize evidence in a chronological timeline.
    - **Visualization Screen:** Implement the UI and logic for data visualization.
    - **Script Editor Screen:** Implement the UI and functionality for the script editor.
    - **Note:** Basic implementations for these screens exist (e.g., `TimelineScreen.kt`). They are not just placeholders but require more sophisticated logic and UI.

- [ / ] **2.4. User-Friendly Error Handling:**
    - Implement specific, user-friendly error messages for common failure scenarios (e.g., OCR failure, file read errors, network issues).
    - **Note:** Basic error handling is implemented in `MainViewModel.kt` via `showError`, but messages are generic.

#### **High Priority 🚀**

- [ ] **2.5. Refactor and Improve Code Quality:**
    - **Refactor `MainViewModel`:** Break down the monolithic `MainViewModel` into smaller, focused ViewModels (`CaseViewModel`, `EvidenceViewModel`, `OcrViewModel`, etc.).
    - **Move API Calls:** Move Google API calls into a dedicated repository layer.
    - **Note:** `MainViewModel.kt` is still a monolithic class. A `GoogleApiService` exists, but the ViewModel contains a lot of direct interaction logic.

- [ / ] **2.6. Improve OCR Accuracy and Options:**
    - Research and implement advanced ML Kit options (e.g., different recognizer options, language hints).
    - Enhance image preprocessing with techniques like noise reduction, skew correction, or allowing user cropping before processing.
    - **Note:** An image preprocessing step (`preprocessImageForOcr`) is implemented in `MainViewModel.kt`.

- [ / ] **2.7. Add Tests:**
    - The project lacks tests. Add unit and integration tests for ViewModels, data parsing, and database operations.
    - **Note:** The project has unit tests for `DataParser.kt` in `app/src/test/java/com/hereliesaz/lexorcist/`. However, the rest of the app lacks tests.

#### **Medium Priority ✨**

- [ ] **2.8. UI/UX Polish:**
    - Review and refine layouts, add clearer instructions, and provide better user feedback (e.g., loading indicators).
    - Improve the `EditEvidenceDialog` in the `DataReviewScreen` to make it easier to edit multi-line content and manage tags.
    - **Note:** The UI is functional but basic.

- [ / ] **2.9. Refactor Hardcoded Strings:**
    - Move hardcoded strings (folder names, sheet names, UI text) to `strings.xml` to improve maintainability and prepare for localization.
    - **Note:** Many strings are externalized in `strings.xml`, but many others (especially for SharedPreferences keys, logging, and error messages) are still hardcoded in `MainViewModel.kt` and other files.

#### **Low Priority / Future Vision 🔭**

- [ ] **3.1. Flexible Spreadsheet Schema:**
    - Consider moving the hardcoded spreadsheet schema to a configuration file or providing a way for users to map columns to data fields.

- [ ] **3.2. Cloud Sync with other Providers:**
    - To reduce reliance on Google, consider adding support for other cloud providers like Dropbox or OneDrive.

- [ ] **3.3. Collaboration Features:**
    - Allow multiple users to collaborate on a single case.

- [ ] **3.4. Advanced Reporting:**
    - Generate detailed reports and visualizations from the collected evidence.

- [ ] **3.5. AI-Powered Analysis:**
    - Use AI to analyze evidence, identify patterns, and suggest connections.

- [ ] **3.6. Multi-Platform Support:**
    - Develop a web or desktop version of the app for a more powerful evidence management experience.
