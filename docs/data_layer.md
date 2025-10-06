# Data Layer and Architecture

This document describes the data storage strategy and file formats used in The Lexorcist application.

---

## **CRITICAL ARCHITECTURAL DIRECTIVE: Local-First**

The application follows a **local-first** data management strategy. This is a core principle that must be respected in all development.

-   **Primary Storage:** All user data, including case information and evidence, is stored in a single spreadsheet file named `lexorcist_data.xlsx` located within the application's folder.
-   **Offline Functionality:** This approach ensures that the app is always fully functional, even when there is no internet connectivity.
-   **Synchronization:**
    *   Upon loading, the app **must** attempt to synchronize with the configured cloud service to ensure it is using the most recent saved state.
    *   Synchronization must also be attempted whenever the application is closed.

---

## Data File Formats

The application's primary data catalogs have been consolidated into a series of CSV files located in the `app/src/main/assets/` directory. These files serve as the new single source of truth for catalog data.

-   **`allegations.csv`**: The master list of all possible legal allegations.
-   **`exhibits.csv`**: The master list of all exhibit types, their descriptions, and their mapping to allegations via `applicable_allegation_ids`.
-   **`jurisdictions.csv`**: A comprehensive list of all courts, used for populating dropdown menus.
-   **`default_scripts.csv`**: The default scripts that are available in the Script Builder.
-   **`default_templates.csv`**: The default templates that are available on the Templates screen.

---

## Google Services Configuration

-   To enable Google services (Drive sync, Sheets, etc.), you must use the `google-services.template.json` file located at the root of the `/app/` folder.
-   This template should be used to create the final `google-services.json` file required to build the project.