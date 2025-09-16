# The Lexorcist

The Lexorcist empowers lawyers and their clients to capture, automatically analyze, and process digital evidence, transforming raw data into court-ready documentation with powerful, scriptable automation.

Whether you're dealing with harassment screenshots, recorded threats,  slanderous slander, or proof of efforts, The Lexorcist provides the tools to streamline the entire workflow—from a picture on a phone to a fully prepared exhibit. A lawyer can literally hand this app to their client and let the legal assistants and interns take a long coffee break. No more spinning heads and green vomit. It's like Holy Water to legalitarians facing the demonic tedium of legal discovery.  You're on your own with the raspy voice and growls of profanity. 

## The Vision

This project is in an advanced stage of development, with a robust architecture designed to realize a specific vision: to create a seamless pipeline where a user can take a photo of evidence, have the app's OCR extract the text, and then use a custom, user-defined rules engine to automatically tag and categorize that evidence. This categorized data is then organized in a Google Sheet, where a powerful backend script can be triggered to generate all necessary legal paperwork, such as cover sheets, metadata reports, and affidavits.

## Key Features

- **Automated Evidence Pipeline:** Capture images or screenshots and let the app handle the rest. The Lexorcist uses OCR to extract text and then feeds it into a powerful, user-scriptable engine for analysis.
- **Customizable Tagging Engine:** Define your own keywords, patterns, and "dorks" using JavaScript to automatically tag evidence. This allows you to create highly specific rules for identifying content relevant to your case, such as threats, hate speech, or contract violations.
- **Google Suite Integration:** Each case gets its own Google Sheet, where all evidence and its metadata are neatly organized. This serves as a central hub for your case data.
- **Automatic Document Generation:** The app leverages a powerful Google Apps Script backend. From your case's Google Sheet, you can instantly generate a variety of court-ready documents using customizable HTML templates.
- **Timeline View:** Visualize the chronology of your evidence with an interactive timeline, making it easy to see the sequence of events.
- **Secure and On-Device:** All OCR and data parsing happens on your device to ensure privacy and security.

## Technical Details

- **Local-First Architecture:** The application follows a local-first data management strategy. All user data, including case information and evidence, is stored in a single spreadsheet file (`lexorcist_data.xlsx`) within the application's folder, and synchronized with cloud services. This approach ensures that the app is always functional, even when offline.
- **UI and UX:** The app is built with Jetpack Compose and Material 3 Expressive. The UI is designed to be right-aligned, with the exception of the navigation rail. All buttons are styled as outlined buttons. The app uses a custom font called "Ithea".

## License

The Lexorcist is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

This app is agent-maintained.
