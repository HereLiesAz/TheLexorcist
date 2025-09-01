# The Lexorcist - UI Workflow and Interface Guide

This document outlines the user interface (UI) workflow for The Lexorcist application, detailing how a user will interact with the app and providing a comprehensive list of all interfaces and their elements.

## UI Workflow

The primary workflow for a user is as follows:

1.  **Authentication:** The user starts the application and is prompted to sign in with their Google account. This is a one-time setup process.
2.  **Case Management:** After signing in, the user can create a new case or select an existing one. A case is a container for all the evidence related to a specific legal matter.
3.  **Evidence Gathering:** The user can add evidence to a case in various forms:
    *   **Text:** Manually enter text as evidence.
    *   **Images:** Import images from the device's gallery or take a new photo. The app will perform Optical Character Recognition (OCR) on the image to extract the text.
    *   **Audio:** Record audio as evidence. The app will transcribe the audio to text.
4.  **Evidence Review and Tagging:** The user can review the evidence, and the app will automatically tag it based on user-defined scripts. The user can also manually add or edit tags.
5.  **Timeline Visualization:** The user can view the evidence in a chronological timeline to understand the sequence of events.
6.  **Document Generation:** The user can generate court-ready documents from the evidence, such as cover sheets, affidavits, and tables of exhibits, using predefined templates.
7.  **Customization:** The user can customize the app's behavior by editing the tagging scripts and managing the document templates.

## Interfaces and Elements

### 1. Sign-In Screen

This is the first screen the user sees if they are not signed in.

*   **Sign In with Google Button:** A button that initiates the Google Sign-In flow.

### 2. Home Screen

The main landing screen after the user is authenticated.

*   **App Name Text:** Displays "The Lexorcist".
*   **Welcome Text:** A short welcome message and instructions on how to use the app.
*   **Create New Case Button:** A button to open the "Create New Case" dialog.

### 3. Cases Screen

This screen displays a list of all the user's cases.

*   **Search Bar:** A text field to search for cases by name.
*   **Sort Button:** A button to change the sort order of the cases (by date or by name).
*   **Case List:** A scrollable list of all the user's cases.
*   **Case Item:** An item in the case list that displays the case name. A long press on a case item reveals options to delete or archive the case.
*   **Create New Case FAB:** A floating action button to open the "Create New Case" dialog.
*   **Delete Case Dialog:** A dialog that asks for confirmation before deleting a case.
*   **Create Case Dialog:** A dialog to create a new case with the following fields:
    *   **Case Name:** The name of the case.
    *   **Exhibit Sheet Name:** The name of the Google Sheet that will store the evidence for this case.
    *   **Case Number:** The case number.
    *   **Case Section:** The case section.
    *   **Judge:** The name of the judge.
    *   **Create Button:** A button to create the new case.
    *   **Open Existing Case Button (not implemented):** A button to open an existing case from Google Drive.
    *   **Cancel Button:** A button to close the dialog.

### 4. Add Evidence Screen

This screen allows the user to add new evidence to a case.

*   **Add Text Evidence Button:** A button to navigate to the "Add Text Evidence" screen.
*   **Add Image Evidence Button (not implemented):** A button to import an image from the device's gallery or take a new photo.
*   **Add Audio Evidence Button (not implemented):** A button to record audio as evidence.

### 5. Add Text Evidence Screen

This screen allows the user to add text evidence to a case.

*   **Evidence Text Field:** A text field to enter the content of the evidence.
*   **Save Button:** A button to save the text evidence.

### 6. Timeline Screen

This screen displays a chronological timeline of all the evidence in a case.

*   **Case Name Text:** Displays the name of the selected case.
*   **Search Bar:** A text field to search for evidence in the timeline.
*   **Timeline View:** A scrollable view that displays the evidence in chronological order.
*   **Evidence Card:** An item in the timeline that displays a summary of the evidence, including its content, category, and date.

### 7. Data Review Screen

This screen allows the user to review and manage all the evidence in a case.

*   **Case Name Text:** Displays the name of the selected case.
*   **Evidence List:** A scrollable list of all the evidence in the selected case.
*   **Evidence Item:** An item in the evidence list that displays a summary of the evidence.
*   **Edit Evidence Button:** A button to open the "Edit Evidence" dialog.
*   **Delete Evidence Button:** A button to delete the evidence.
*   **Edit Evidence Dialog:** A dialog to edit the details of an evidence item, including its content, source document, category, and tags.
*   **Delete Evidence Dialog:** A dialog that asks for confirmation before deleting an evidence item.

### 8. Evidence Details Screen

This screen displays the full details of a single evidence item.

*   **Evidence Content:** Displays the full content of the evidence.
*   **Evidence Metadata:** Displays the metadata of the evidence (e.g., source, category, tags).
*   **Commentary Text Field:** A text field to add or edit a commentary for the evidence.
*   **Save Commentary Button:** A button to save the commentary.

### 9. Script Editor Screen

This screen allows the user to write and edit the JavaScript code that is used to automatically tag evidence.

*   **Script Editor:** A text field to write and edit the script.
*   **Save Script Button:** A button to save the script.
*   **Script Builder:** A section with buttons to insert predefined script snippets.

### 10. Settings Screen

This screen allows the user to configure the app's settings.

*   **Dark Mode Switch:** A switch to enable or disable dark mode.
*   **Sign Out Button:** A button to sign out from the application.

### 11. Add-ons Browser Screen (not fully implemented)

This screen will allow users to browse and install community-created scripts and templates.

*   **Scripts List:** A list of available scripts to install.
*   **Templates List:** A list of available templates to install.
*   **Share Add-on FAB:** A floating action button to share a new script or template.

### 12. Share Add-on Screen (not fully implemented)

This screen will allow users to share their own scripts and templates with the community.

*   **Name Text Field:** A text field to enter the name of the add-on.
*   **Description Text Field:** A text field to enter the description of the add-on.
*   **Content Text Field:** A text field to enter the content of the add-on (the script code or the template HTML).
*   **Type Toggle:** A button to toggle between "Script" and "Template".
*   **Share Button:** A button to share the add-on.
