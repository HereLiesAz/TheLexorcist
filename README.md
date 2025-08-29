# The Lexorcist

The Lexorcist is a powerful, user-friendly Android application designed to assist users in preparing for legal cases, particularly those involving the review and organization of extensive digital evidence. It provides a suite of tools to streamline the process of handling, annotating, and presenting evidence, making it an indispensable asset for legal professionals and individuals representing themselves.

## Key Features

- **OCR-based Evidence Tracking:** Automatically digitize receipts and other financial documents by taking a picture. The app uses Google's ML Kit to recognize and extract text.
- **Data Parsing:** Intelligently parses recognized text to identify key information such as the total amount, date, and vendor, which can be used as evidence.
- **Local Storage:** Securely stores all extracted financial data on the device using a Room database, ensuring the integrity of the evidence.
- **Evidence History:** View a list of all saved financial entries, which can be used as a chain of custody for the evidence.
- **Case Management**: Create and manage multiple cases, each with its own set of evidence and notes.
- **Advanced Filtering and Sorting**: Quickly find relevant information with robust filtering and sorting capabilities.
- **Custom Tagging**: Apply custom tags to evidence for easy identification and grouping.
- **Timeline View**: Visualize the sequence of events with an interactive timeline.
- **Secure and Private**: All data is stored locally on your device to ensure privacy and security.

## Technologies Used

- **Kotlin:** The primary programming language for the application.
- **Jetpack Compose:** Used for building the user interface.
- **Google ML Kit:** Powers the text recognition (OCR) functionality.
- **Room:** For local database storage of financial entries.
- **AzNavRail:** A library used for navigation components in the UI.

## Getting Started

To build and run Lexorcist from the source code, you will need:

- Android Studio (latest version recommended)
- An Android device or emulator running API level 26 or higher

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/the-lexorcist.git
   ```
2. **Open the project in Android Studio.**
3. **Connect an Android device or start an emulator.**
4. **Build and run the app.** Android Studio will handle the rest of the build process, including downloading the required dependencies.

**Note:** You may need to configure your `local.properties` file with the path to your Android SDK, but this is typically handled automatically by Android Studio.

## Contributing

Contributions are welcome! If you'd like to contribute to The Lexorcist, please follow these steps:

1. **Fork the repository.**
2. **Create a new branch for your feature or bug fix.**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes and commit them with a clear commit message.**
4. **Push your changes to your fork.**
   ```bash
   git push origin feature/your-feature-name
   ```
5. **Create a pull request.**

Please make sure your code adheres to the existing code style and that you add or update tests as appropriate.

## Versioning

This project uses `com.palantir.git-version` to automatically generate a version number from the git history.
The version is in the format of `<tag>-<commit-count>-<short-hash>` (e.g., `1.0.0-5-gbe0ddce`).
When a commit is tagged with a version number (e.g., `1.0.0`), the version will be just that tag.

## License

The Lexorcist is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
