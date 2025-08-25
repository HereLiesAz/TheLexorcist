# Lexorcist

Lexorcist is an Android application designed to help users track their expenses by digitizing paper receipts. Using the device's camera, Lexorcist can perform Optical Character Recognition (OCR) on a receipt, extract relevant financial information, and store it in a local database. This provides a convenient way to manage and review expenses without manually entering data.

## Features

- **OCR-based Expense Tracking:** Automatically digitize receipts by taking a picture. The app uses Google's ML Kit to recognize and extract text.
- **Data Parsing:** Intelligently parses recognized text to identify key information such as the total amount, date, and vendor.
- **Local Storage:** Securely stores all extracted financial data on the device using a Room database.
- **Expense History:** View a list of all saved financial entries, sorted by date.

## Technologies Used

- **Kotlin:** The primary programming language for the application.
- **Jetpack Compose:** Used for building the user interface.
- **Google ML Kit:** Powers the text recognition (OCR) functionality.
- **Room:** For local database storage of financial entries.
- **AzNavRail:** A library used for navigation components in the UI.

## Building and Running the App

To build and run Lexorcist from the source code, you will need:

- Android Studio (latest version recommended)
- An Android device or emulator running API level 26 or higher

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Open the project in Android Studio.**
3. **Connect an Android device or start an emulator.**
4. **Build and run the app.** Android Studio will handle the rest of the build process, including downloading the required dependencies.

**Note:** You may need to configure your `local.properties` file with the path to your Android SDK, but this is typically handled automatically by Android Studio.
