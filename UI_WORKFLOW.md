# The Lexorcist - UI Workflow and Interface Guide

This document outlines the user interface (UI) workflow for The Lexorcist application, detailing how a user will interact with the app and providing a comprehensive list of all interfaces and their elements.

## UI Workflow

The primary workflow for a user is as follows:

1.  **Authentication (Complete):** The user can sign in with their Google account.
2.  **Case Management (Complete):** The user can create, select, archive, and delete cases. All changes are saved automatically.
3.  **Evidence Gathering (Complete):** The user can add text, image, audio, and video evidence. The OCR and transcription functionalities are implemented.
4.  **Evidence Review and Tagging (Complete):** The user can review evidence and assign allegations. The automatic tagging is implemented through the script runner.
5.  **Timeline Visualization (Complete):** The user can view the evidence in a timeline.
6.  **Document Generation (Incomplete):** This feature is not yet implemented.
7.  **Customization (Complete):** The user can create and edit scripts. Scripts are saved automatically.
8.  **Add-ons (Complete):** The user can browse and share scripts and templates.

## Interfaces and Elements

### 1. Sign-In Screen

*   **Sign In with Google Button (Complete):** A button that initiates the Google Sign-In flow.

### 2. Home Screen

*   **App Name Text (Complete):** Displays "The Lexorcist".
*   **Welcome Text (Complete):** A short welcome message and instructions on how to use the app.
*   **Create New Case Button (Complete):** A button to open the "Create New Case" dialog.

### 3. Cases Screen

*   **Search Bar (Complete):** A text field to search for cases by name.
*   **Case List (Complete):** A scrollable list of all the user's cases.
*   **Case Item (Complete):** An item in the case list that displays the case name. A long press on a case item reveals options to delete or archive the case.
*   **Create New Case FAB (Complete):** A floating action button to open the "Create New Case" dialog.
*   **Delete Case Dialog (Complete):** A dialog that asks for confirmation before deleting a case.
*   **Create Case Dialog (Complete):** A dialog to create a new case.
    *   **Case Name (Complete)**
    *   **Exhibit Sheet Name (Complete)**
    *   **Case Number (Complete)**
    *   **Case Section (Complete)**
    *   **Judge (Complete)**
    *   **Create Button (Complete)**
    *   **Cancel Button (Complete)**

### 4. Add Evidence Screen

*   **Add Text Evidence Button (Complete):** A button to add text evidence.
*   **Add Image Evidence Button (Complete):** A button to add image evidence.
*   **Add Audio Evidence Button (Complete):** A button to add audio evidence.
*   **Add Video Evidence Button (Complete):** A button to add video evidence.

### 5. Timeline Screen

*   **Case Name Text (Complete):** Displays the name of the selected case.
*   **Timeline View (Complete):** A scrollable view that displays the evidence in chronological order.
*   **Evidence Card (Complete):** An item in the timeline that displays a summary of the evidence.

### 6. Data Review Screen

*   **Case Name Text (Complete):** Displays the name of the selected case.
*   **Evidence List (Complete):** A scrollable list of all the evidence in the selected case.
*   **Evidence Item (Complete):** An item in the evidence list that displays a summary of the evidence.
*   **Edit Evidence Button (Complete):** A button to edit the details of an evidence item.
*   **Delete Evidence Button (Complete):** A button to delete an evidence item.
*   **Edit Evidence Dialog (Complete):** A dialog to edit the details of an evidence item.
*   **Delete Evidence Dialog (Complete):** A dialog that asks for confirmation before deleting an evidence item.

### 7. Evidence Details Screen

*   **Evidence Content (Complete):** Displays the full content of the evidence.
*   **Evidence Metadata (Complete):** Displays the metadata of the evidence.
*   **Commentary Text Field (Complete):** A text field to add or edit a commentary for the evidence.
*   **Save Commentary Button (Complete):** A button to save the commentary.

### 8. Script Editor Screen

*   **Script Editor (Complete):** A text field to write and edit the script.
*   **Save Script Button (Complete):** The button saves the script automatically.
*   **Script Builder (Complete):** A section with buttons to insert predefined script snippets.

### 9. Settings Screen

*   **Theme Settings (Complete):** A section to change the theme of the app.
*   **Cache Settings (Complete):** A section to clear the app's cache.
*   **Sign Out Button (Complete):** A button to sign out from the application.

### 10. Add-ons Browser Screen

*   **Scripts List (Complete):** A list of available scripts to install.
*   **Templates List (Complete):** A list of available templates to install.
*   **Share Add-on FAB (Complete):** A floating action button to share a new script or template.

### 11. Share Add-on Screen

*   **Name Text Field (Complete):** A text field to enter the name of the add-on.
*   **Description Text Field (Complete):** A text field to enter the description of the add-on.
*   **Content Text Field (Complete):** A text field to enter the content of the add-on.
*   **Type Toggle (Complete):** A button to toggle between "Script" and "Template".
*   **Share Button (Complete):** A button to share the add-on.

### 12. Templates Screen

*   **Create New Template Button (Complete):** A button to create a new template.
*   **Import Template Button (Complete):** A button to import a template from a file.
*   **Templates List (Complete):** A list of available templates.
*   **Template Item (Complete):** An item in the template list that displays a preview of the template.
*   **Template Editor (Complete):** A dialog to create or edit a template.
    *   **Name Text Field (Complete)**
    *   **Description Text Field (Complete)**
    *   **Content Text Field (Complete)**
    *   **Save Button (Complete)**
    *   **Cancel Button (Complete)**
