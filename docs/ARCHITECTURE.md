# Application Architecture

## Overview

The Lexorcist is a modern Android application built using the recommended **Guide to App Architecture** principles from Google. It follows the **MVVM (Model-View-ViewModel)** architectural pattern and leverages **Jetpack Compose** for the UI.

## Layers

The application is structured into three primary layers:

### 1. UI Layer
*   **Components**: Activities (`MainActivity`), Composables (`CasesScreen`, `TimelineScreen`), and ViewModels (`CaseViewModel`, `ReviewViewModel`).
*   **Responsibility**: Displays data to the user and captures user events.
*   **State Management**: Uses `StateFlow` to expose immutable state snapshots to the UI.
*   **UI Toolkit**: Jetpack Compose.

### 2. Domain Layer (Optional/Implicit)
*   While not explicitly separated into a `domain` module, the `ViewModel`s and `UseCase`-like service classes (e.g., `CleanupService`, `ScriptRunner`) encapsulate business logic.

### 3. Data Layer
*   **Components**: Repositories (`CaseRepository`, `EvidenceRepository`), Data Sources (`LocalFileStorageService`, `GoogleApiService`), and Models (`Case`, `Evidence`).
*   **Responsibility**: Manages application data, exposing it to the UI layer. Handles data retrieval, storage, and synchronization.
*   **Single Source of Truth**: The `LocalFileStorageService` (backed by an Excel spreadsheet) acts as the primary local data source, synchronized with cloud providers.

## Key Technologies

*   **Dependency Injection**: **Hilt** is used for dependency injection throughout the app.
*   **Asynchronous Programming**: **Kotlin Coroutines** and **Flow** are used for background tasks and reactive data streams.
*   **Local Storage**:
    *   **Excel (.xlsx)**: Used as the primary database format for portability and user accessibility.
    *   **DataStore**: Used for simple key-value storage (e.g., script execution state).
    *   **EncryptedSharedPreferences**: Used for storing sensitive credentials.
*   **Cloud Integration**:
    *   **Google Drive & Sheets API**: For cloud storage and synchronization.
    *   **Microsoft Graph API**: For Outlook email import.
    *   **Gmail API**: For Gmail import.
*   **Machine Learning**: **ML Kit** is used for on-device OCR.
*   **Scripting**: **Mozilla Rhino** is used to execute user-defined JavaScript automations in a secure sandbox.

## Security Architecture

*   **Script Sandboxing**: The `ScriptRunner` service uses a `ClassShutter` to prevent user scripts from accessing Java classes, ensuring that scripts can only interact with the exposed safe API (`lex` object).
*   **Path Sanitization**: The `LocalFileStorageService` strictly sanitizes all file paths and identifiers to prevent Path Traversal vulnerabilities.
*   **Formula Injection Prevention**: Inputs written to spreadsheets are sanitized (by prepending `'`) to prevent CSV/Formula Injection attacks.
*   **Encrypted Storage**: Sensitive authentication tokens are stored in `EncryptedSharedPreferences`.

## Data Flow

1.  **User Action**: The user interacts with the UI (e.g., takes a photo).
2.  **ViewModel**: The UI calls a method in `CaseViewModel`.
3.  **Service/Repository**: The ViewModel delegates the work to a service (e.g., `OcrProcessingService`).
4.  **Data Source**: The service uses `LocalFileStorageService` to save the file and metadata to the local Excel sheet.
5.  **State Update**: The repository emits the updated data via a `Flow`.
6.  **UI Update**: The ViewModel processes the Flow and updates its `StateFlow`, causing the Compose UI to recompose.

## Directory Structure

*   `di/`: Hilt modules for dependency injection.
*   `ui/`: Composable screens and UI components.
*   `viewmodel/`: ViewModels handling UI logic.
*   `data/`: Repositories and data models.
*   `service/`: Background services and business logic (OCR, Scripting, API clients).
*   `utils/`: Utility classes and extensions.
