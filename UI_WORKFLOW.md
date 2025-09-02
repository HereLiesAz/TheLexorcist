# The Lexorcist - UI Workflow and Interface Guide

This document outlines the user interface (UI) workflow for The Lexorcist application, detailing how a user will interact with the app and providing a comprehensive list of all interfaces and their elements.

## UI Workflow

The primary workflow for a user is as follows:

1.  **Authentication (Partially Complete):** The user can sign in with their Google account, but the error handling and user feedback can be improved.
2.  **Case Management (Partially Complete):** The UI for case management exists in `CasesScreen.kt`. The user can create a new case, but the case is not saved anywhere because the repository is not implemented. The user cannot select an existing case.
3.  **Evidence Gathering (Partially Complete):** The UI for adding text evidence exists in `EvidenceScreen.kt`. Image and audio evidence are not implemented. The OCR and transcription functionalities are missing.
4.  **Evidence Review and Tagging (Partially Complete):** The UI for reviewing evidence exists in `ReviewScreen.kt`. The automatic tagging is not fully implemented because the script runner is not fully integrated.
5.  **Timeline Visualization (Partially Complete):** The user can view the evidence in a timeline, but the timeline is not fully functional because the evidence loading is not working correctly.
6.  **Document Generation (Incomplete):** This feature is not implemented.
7.  **Customization (Partially Complete):** The UI for editing scripts exists in `ScriptEditorScreen.kt`. The script management is not fully implemented.

## Interfaces and Elements

### 1. Sign-In Screen

*   **Sign In with Google Button (Complete):** A button that initiates the Google Sign-In flow.

### 2. Home Screen

*   **App Name Text (Complete):** Displays "The Lexorcist".
*   **Welcome Text (Complete):** A short welcome message and instructions on how to use the app.
*   **Create New Case Button (Complete):** A button to open the "Create New Case" dialog.

### 3. Cases Screen

*   **Search Bar (Complete):** A text field to search for cases by name.
*   **Sort Button (Complete):** A button to change the sort order of the cases (by date or by name).
*   **Case List (Partially Complete):** A scrollable list of all the user's cases. The UI is implemented in `CasesScreen.kt`, but it is not functional because the repository is not implemented.
*   **Case Item (Partially Complete):** An item in the case list that displays the case name. A long press on a case item reveals options to delete or archive the case. The UI is implemented in `CasesScreen.kt`, but it is not functional because the repository is not implemented.
*   **Create New Case FAB (Complete):** A floating action button to open the "Create New Case" dialog.
*   **Delete Case Dialog (Partially Complete):** A dialog that asks for confirmation before deleting a case. The UI is implemented in `CasesScreen.kt`, but it is not functional because the repository is not implemented.
*   **Create Case Dialog (Partially Complete):** A dialog to create a new case. The dialog is functional, but the created case is not saved.
    *   **Case Name (Complete)**
    *   **Exhibit Sheet Name (Complete)**
    *   **Case Number (Complete)**
    *   **Case Section (Complete)**
    *   **Judge (Complete)**
    *   **Create Button (Partially Complete):** The button creates a case in the view model, but the case is not persisted.
    *   **Open Existing Case Button (Incomplete):** This button is not implemented.
    *   **Cancel Button (Complete)**

### 4. Add Evidence Screen

*   **Add Text Evidence Button (Complete):** A button to navigate to the "Add Text Evidence" screen.
*   **Add Image Evidence Button (Incomplete):** This button is not implemented.
*   **Add Audio Evidence Button (Incomplete):** This button is not implemented.

### 5. Add Text Evidence Screen

*   **Evidence Text Field (Complete):** A text field to enter the content of the evidence.
*   **Save Button (Partially Complete):** The button saves the evidence to the view model, but the evidence is not persisted.

### 6. Timeline Screen

*   **Case Name Text (Complete):** Displays the name of the selected case.
*   **Search Bar (Complete):** A text field to search for evidence in the timeline.
*   **Timeline View (Partially Complete):** A scrollable view that displays the evidence in chronological order. The UI is implemented in `TimelineScreen.kt` but is not functional because the evidence loading is not working correctly.
*   **Evidence Card (Partially Complete):** An item in the timeline that displays a summary of the evidence. The UI is implemented in `TimelineScreen.kt` but is not functional because the evidence loading is not working correctly.

### 7. Data Review Screen

*   **Case Name Text (Complete):** Displays the name of the selected case.
*   **Evidence List (Partially Complete):** A scrollable list of all the evidence in the selected case. The UI is implemented in `ReviewScreen.kt` but is not functional because the repository is not implemented.
*   **Evidence Item (Partially Complete):** An item in the evidence list that displays a summary of the evidence. The UI is implemented in `ReviewScreen.kt` but is not functional because the repository is not implemented.
*   **Edit Evidence Button (Incomplete):** This button is not implemented.
*   **Delete Evidence Button (Incomplete):** This button is not implemented.
*   **Edit Evidence Dialog (Incomplete):** A dialog to edit the details of an evidence item. This is not functional because the repository is not implemented.
*   **Delete Evidence Dialog (Incomplete):** A dialog that asks for confirmation before deleting an evidence item. This is not functional because the repository is not implemented.

### 8. Evidence Details Screen

*   **Evidence Content (Partially Complete):** Displays the full content of the evidence. The UI is implemented in `EvidenceDetailsScreen.kt` but is not functional because the evidence loading is not working correctly.
*   **Evidence Metadata (Partially Complete):** Displays the metadata of the evidence. The UI is implemented in `EvidenceDetailsScreen.kt` but is not functional because the evidence loading is not working correctly.
*   **Commentary Text Field (Partially Complete):** A text field to add or edit a commentary for the evidence. The commentary is not persisted.
*   **Save Commentary Button (Partially Complete):** A button to save the commentary. The commentary is not persisted.

### 9. Script Editor Screen

*   **Script Editor (Complete):** A text field to write and edit the script.
*   **Save Script Button (Partially Complete):** The button saves the script, but the script is not used by the script runner.
*   **Script Builder (Complete):** A section with buttons to insert predefined script snippets.

### 10. Settings Screen

*   **Dark Mode Switch (Complete):** A switch to enable or disable dark mode.
*   **Sign Out Button (Complete):** A button to sign out from the application.

### 11. Add-ons Browser Screen (not fully implemented)

*   **Scripts List (Incomplete):** A list of available scripts to install.
*   **Templates List (Incomplete):** A list of available templates to install.
*   **Share Add-on FAB (Incomplete):** A floating action button to share a new script or template.

### 12. Share Add-on Screen (not fully implemented)

*   **Name Text Field (Incomplete):** A text field to enter the name of the add-on.
*   **Description Text Field (Incomplete):** A text field to enter the description of the add-on.
*   **Content Text Field (Incomplete):** A text field to enter the content of the add-on.
*   **Type Toggle (Incomplete):** A button to toggle between "Script" and "Template".
*   **Share Button (Incomplete):** A button to share the add-on.
