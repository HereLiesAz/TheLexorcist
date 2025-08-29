# TODO - The Lexorcist (Revised)

This document outlines the tasks and improvements for The Lexorcist application, based on a recent code review. The previous `TODO.md` was found to be outdated.

## Critical 🎯
- [ ] **Implement Data Parsing:** The app currently performs OCR but does not parse the extracted text to identify key information (e.g., total amount, date, vendor from a receipt). This is a core feature that is completely missing. The existing `DataParser.kt` is a good starting point but is not currently used.
- [ ] **Complete Placeholder Screens:** The navigation rail includes links to "Timeline," and "Visualization" screens. These screens are currently placeholders and need to be fully implemented to match the app's advertised features.
- [ ] **User-Friendly Error Handling:** The app's error handling is minimal. When something goes wrong (e.g., network error, API failure, OCR failure), the user is often shown a generic error message or no message at all. Implement specific, user-friendly error messages for common failure scenarios.

## High Priority 🚀
- [ ] **Improve OCR Accuracy and Options:**
    - The current OCR process uses default settings. Research and implement advanced ML Kit options (e.g., different recognizer options, language hints) to improve accuracy.
    - The image preprocessing in `MainViewModel.kt` (grayscale, adaptive threshold) is a good start, but could be enhanced with more advanced techniques like noise reduction, skew correction, or allowing the user to crop the image before processing.
- [ ] **Implement Offline Support:** The app is entirely dependent on an internet connection and Google services. This is a major limitation.
    - Implement a local database (e.g., using Room) to cache data for offline viewing.
    - Implement a synchronization mechanism to sync local data with Google Drive/Sheets when the app is online.
- [ ] **Refactor `MainViewModel`:** The `MainViewModel` is currently a monolith that handles too many responsibilities (UI state, OCR, file parsing, Google API calls, etc.).
    - Break it down into smaller, more focused ViewModels (e.g., `CaseViewModel`, `EvidenceViewModel`, `OcrViewModel`, `SettingsViewModel`).
    - Move the Google API calls into a dedicated repository layer.

## Medium Priority ✨
- [ ] **UI/UX Polish:**
    - The overall UI is functional but could be improved. Review and refine layouts, add clearer instructions, and provide better user feedback (e.g., loading indicators).
    - The `DataReviewScreen` is functional, but the `EditEvidenceDialog` is very basic. Improve this dialog to make it easier to edit multi-line content and manage tags.
- [ ] **Add Unit and Integration Tests:** The project has very few tests.
    - Add unit tests for the `DataParser`, ViewModels, and other logic.
    - Add integration tests for the database and Google API interactions.
- [ ] **Expense Categorization:** Implement the feature to allow users to categorize expenses. This is mentioned in the old `TODO.md` and is a valuable feature for organization and reporting.
- [ ] **Incomplete Spreadsheet Parsing:** The `parseSpreadsheetFile` function in `MainViewModel.kt` is a stub. Implement the logic to correctly parse data from `.xls` and `.xlsx` files.

## Low Priority 🛠️
- [ ] **Refactor Hardcoded Strings:** Many strings (folder names, sheet names, UI text) are hardcoded. Move these to `strings.xml` to improve maintainability and prepare for localization.
- [ ] **Flexible Spreadsheet Schema:** The spreadsheet schema is hardcoded. Consider moving this to a configuration file or providing a way for users to map columns to data fields.
- [ ] **Improve `DataParser`:** The regex-based `DataParser` can be improved.
    - Add support for more date formats and edge cases.
    - Explore using more advanced NLP libraries for more robust entity extraction.

## Future Vision / Architectural Evolution 🔭
- [ ] **Cloud Sync with other Providers:** To reduce reliance on Google, consider adding support for other cloud providers like Dropbox or OneDrive.
- [ ] **Collaboration Features:** Allow multiple users to collaborate on a single case.
- [ ] **Advanced Reporting:** Generate detailed reports and visualizations from the collected evidence.
- [ ] **AI-Powered Analysis:** Use AI to analyze evidence, identify patterns, and suggest connections.
- [ ] **Multi-Platform Support:** Develop a web or desktop version of the app for a more powerful evidence management experience.
