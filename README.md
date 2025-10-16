# The Lexorcist

The Lexorcist empowers lawyers and their clients to capture, automatically analyze, and process digital evidence, transforming raw data into court-ready documentation with powerful, scriptable automation.

Whether you're dealing with harassment screenshots, recorded threats,  slanderous slander, or proof of efforts, The Lexorcist provides the tools to streamline the entire workflow—from a picture on a phone to a fully prepared exhibit. A lawyer can literally hand this app to their client and let the legal assistants and interns take a long coffee break. No more spinning heads and green vomit. It's like Holy Water to legalitarians facing the demonic tedium of legal discovery.  You're on your own with the raspy voice and growls of profanity. 

## The Vision

This project is in an advanced stage of development, with a robust architecture designed to realize a specific vision: to create a seamless pipeline where a user can take a photo of evidence, have the app's OCR extract the text, and then use a custom, user-defined rules engine to automatically tag and categorize that evidence. This categorized data is then organized in a Google Sheet, where a powerful backend script can be triggered to generate all necessary legal paperwork, such as cover sheets, metadata reports, and affidavits.

## Getting Started

Follow these instructions to set up the development environment and get the application running on an Android emulator or a physical device.

### Prerequisites

-   **Android Studio:** The official integrated development environment (IDE) for Android app development. You can download it from the [Android Developer website](https://developer.android.com/studio).
-   **Java Development Kit (JDK):** Version 17 or higher. Android Studio often comes with its own embedded JDK, which is recommended.
-   **Google Account:** Required for Google Drive and Google Sheets integration.

### Setup Instructions

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/HereLiesAz/TheLexorcist.git
    cd TheLexorcist
    ```

2.  **Configure Google Services:**
    The application uses Google services for features like cloud storage and document generation. You'll need to provide your own configuration file.
    -   Locate the template file at `app/google-services.template.json`.
    -   Copy this file and rename the copy to `app/google-services.json`.
    -   Follow the instructions in the [Firebase documentation](https://firebase.google.com/docs/android/setup) to obtain your own `google-services.json` file and replace the contents of the copied file with your own configuration.

3.  **Build the Application:**
    The project uses the Gradle wrapper to ensure a consistent build environment. Open a terminal in the project's root directory and run the following command:
    ```bash
    ./gradlew :app:assembleDebug
    ```
    This command will download the required dependencies and compile the debug version of the application.

4.  **Run the Application:**
    -   Open the project in Android Studio.
    -   Android Studio will automatically sync the Gradle project.
    -   Select a run configuration (usually `app`).
    -   Choose an available emulator or connect a physical Android device.
    -   Click the "Run" button (green play icon).

## Key Features

- **Multi-Source Evidence Pipeline:** Capture evidence from images (OCR), audio (transcription), video (frame-by-frame OCR), and location history files.
- **Customizable Tagging Engine:** Define your own keywords, patterns, and "dorks" using JavaScript to automatically tag evidence.
- **AI-Powered Analysis:** Go beyond simple keywords. The scripting engine includes a local, on-device AI for semantic analysis and a cloud-based generative AI for creating new content.
- **Google Suite Integration:** Each case gets its own Google Sheet, where all evidence and its metadata are neatly organized.
- **Automatic Document Generation:** The app leverages a powerful Google Apps Script backend to instantly generate court-ready documents from customizable HTML templates.
- **Interactive Timeline:** Visualize the chronology of your evidence with an interactive timeline.
- **Secure and On-Device:** Core processing like OCR and transcription happens on-device to ensure privacy and security.
- **Transparent Progress Reporting:** See detailed, real-time logs of the evidence processing pipeline.
- **Configurable Storage:** Choose where your case folders are stored on your device and enable or disable cloud synchronization with providers like Google Drive and Dropbox.
- **Evidence Management:** Organize evidence into exhibits, clean up duplicates, and merge related items on the Review screen.
- **Location History Import:** Import your location history from Google Takeout and filter it by a specific date range to add precise location data to your case.

## Project Roadmap

The project's roadmap and a detailed list of tasks are tracked in the `TODO.md` file. This includes the "Production Readiness and Remediation Protocol," which outlines the steps to bring the application to a production-ready state.

## Localization

This application and its documentation are also available in Spanish.

- [Política de privacidad (Privacy Policy)](PRIVACY_POLICY_ES.md)
- [Términos de servicio (Terms of Service)](TERMS_OF_SERVICE_ES.md)
- [Ejemplos de scripts (Script Examples)](SCRIPT_EXAMPLES_ES.md)

