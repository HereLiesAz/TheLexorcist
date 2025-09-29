# The Lexorcist

The Lexorcist empowers lawyers and their clients to capture, automatically analyze, and process digital evidence, transforming raw data into court-ready documentation with powerful, scriptable automation.

Whether you're dealing with harassment screenshots, recorded threats,  slanderous slander, or proof of efforts, The Lexorcist provides the tools to streamline the entire workflow—from a picture on a phone to a fully prepared exhibit. A lawyer can literally hand this app to their client and let the legal assistants and interns take a long coffee break. No more spinning heads and green vomit. It's like Holy Water to legalitarians facing the demonic tedium of legal discovery.  You're on your own with the raspy voice and growls of profanity. 

## The Vision

This project is in an advanced stage of development, with a robust architecture designed to realize a specific vision: to create a seamless pipeline where a user can take a photo of evidence, have the app's OCR extract the text, and then use a custom, user-defined rules engine to automatically tag and categorize that evidence. This categorized data is then organized in a Google Sheet, where a powerful backend script can be triggered to generate all necessary legal paperwork, such as cover sheets, metadata reports, and affidavits.

## Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

- Android Studio (latest stable version)
- JDK 17

### Installation

1.  **Clone the repo**
    ```sh
    git clone https://github.com/HereLiesAz/The-Lexorcist.git
    ```
2.  **Set up Google Services**
    -   Navigate to the `app/` directory.
    -   Copy the `google-services.template.json` file and rename the copy to `google-services.json`.
    -   Follow the instructions in `GMAIL_API_SETUP.md` to obtain your own Google API credentials and populate the `google-services.json` file.
3.  **Build the Project**
    -   Open the project in Android Studio.
    -   Let Gradle sync and download the necessary dependencies.
    -   Build the application using the command:
        ```sh
        ./gradlew :app:compileDebugKotlin
        ```
4.  **Run the App**
    -   Run the app on an emulator or a physical device.

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

## Localization

This application and its documentation are also available in Spanish.

- [Política de privacidad (Privacy Policy)](PRIVACY_POLICY_ES.md)
- [Términos de servicio (Terms of Service)](TERMS_OF_SERVICE_ES.md)
- [Ejemplos de scripts (Script Examples)](SCRIPT_EXAMPLES_ES.md)

