# Data Layer and Architecture

This document describes the data storage strategy and file formats used in The Lexorcist application.

---

## **CRITICAL ARCHITECTURAL DIRECTIVE: Local-First**

The application follows a **local-first** data management strategy. This is a core principle that must be respected in all development.

-   **Primary Storage:** All user data, including case information, evidence, and exhibits, is stored in a single spreadsheet file named `lexorcist_data.xlsx` located within the application's private storage directory (or a user-configured location).
-   **Service:** The `LocalFileStorageService` is the single source of truth for reading and writing to this file. It handles all CRUD operations.
-   **Offline Functionality:** This approach ensures that the app is always fully functional, even when there is no internet connectivity.
-   **Synchronization:**
    *   Upon loading, the app attempts to synchronize with the configured cloud service (Google Drive, Dropbox, OneDrive) via `SyncManager` to ensure it is using the most recent saved state.
    *   Synchronization is also attempted whenever the application is closed or significant changes are made.

---

## Security Measures

The Data Layer implements strict security controls to prevent common vulnerabilities associated with file handling and spreadsheet manipulation.

### Path Sanitization
To prevent **Path Traversal** attacks, where a malicious file name could overwrite critical system files:
*   All file names and directory names derived from user input (e.g., Case IDs, file attachments) are sanitized.
*   Only alphanumeric characters, dashes (`-`), and underscores (`_`) are allowed in path segments.
*   The `LocalFileStorageService.sanitizeSafePathSegment` function enforces this rule.

### Formula Injection Prevention
To prevent **CSV/Formula Injection** attacks, where malicious cell data executes code when opened in Excel/Sheets:
*   All user-controlled string inputs written to the spreadsheet are sanitized using `SpreadsheetUtils.sanitizeForSpreadsheet`.
*   Any string starting with `=`, `+`, `-`, or `@` is prepended with a single quote (`'`), forcing the spreadsheet application to treat it as a string literal rather than an executable formula.

### Encrypted Storage
*   Sensitive authentication tokens (e.g., for Dropbox, OneDrive) and user preferences are stored in **EncryptedSharedPreferences**, ensuring they are encrypted at rest.

---

## Data File Formats

### Core Database (`lexorcist_data.xlsx`)
This Excel file contains multiple sheets acting as database tables:
*   **Cases**: Metadata for each legal case.
*   **Evidence**: All evidence items associated with cases.
*   **Exhibits**: Definitions of exhibits grouping evidence.
*   **Allegations**: Specific allegations associated with cases.
*   **TranscriptEdits**: History of edits made to audio transcripts.

### Assets
The application's static data catalogs are located in `app/src/main/assets/`:
-   **`allegations.csv`**: Master list of legal allegations.
-   **`exhibits.csv`**: Master list of exhibit types.
-   **`jurisdictions.csv`**: List of courts.
-   **`default_scripts.csv`**: Default automation scripts.
-   **`default_templates.csv`**: Default document templates.

---

## Google Services Configuration

-   To enable Google services (Drive sync, Sheets, etc.), you must use the `google-services.template.json` file located at the root of the `/app/` folder.
-   This template should be used to create the final `google-services.json` file required to build the project.
