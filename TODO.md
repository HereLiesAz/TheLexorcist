# TODO

This file lists the immediate tasks that need to be addressed to improve the Lexorcist application.

## High Priority
- [x] **Improve OCR Accuracy:** The current text recognition can sometimes be inaccurate, especially with crumpled receipts or unusual fonts. Research and implement techniques to improve the reliability of the ML Kit OCR. This could involve pre-processing the image (e.g., binarization, noise reduction) before sending it to the recognizer.
- [ ] **Implement Edit/Delete Functionality:** Users currently cannot edit or delete financial entries after they have been saved. This is a critical feature for correcting mistakes or removing duplicate entries.

## Medium Priority
- [ ] **Categorize Expenses:** Add a feature that allows users to categorize their expenses (e.g., "Food," "Transportation," "Utilities"). This will help with organization and future reporting features.
- [ ] **Add Data Visualization:** Integrate a simple charting library to provide users with a visual representation of their spending habits. A pie chart showing expenses by category would be a good starting point.
- [ ] **Refine the UI:** The current UI is functional but could be improved for a better user experience. This includes adding clearer instructions, improving the layout of the expense list, and providing better feedback to the user during the OCR process.

## Low Priority
- [ ] **Add Unit Tests:** The project currently lacks a comprehensive suite of unit tests. Adding tests for the data parsing logic and database operations would improve the stability of the app.
- [ ] **Improve Error Handling:** Provide more specific error messages to the user when something goes wrong (e.g., OCR failure, database errors).
