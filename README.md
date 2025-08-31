# The Lexorcist

The Lexorcist is a powerful, user-friendly Android application designed to exorcise the demonic tedium of legal discovery. It empowers lawyers and their clients to capture, automatically analyze, and process digital evidence, transforming raw data into court-ready documentation with powerful, scriptable automation.

Whether you're dealing with screenshots of harassment, written threats, or examples of slander, The Lexorcist provides the tools to streamline the entire workflow—from a picture on a phone to a fully prepared exhibit.

## The Vision

This project is in an advanced stage of development, with a robust architecture designed to realize a specific vision: to create a seamless pipeline where a user can take a photo of evidence, have the app's OCR extract the text, and then use a custom, user-defined rules engine to automatically tag and categorize that evidence. This categorized data is then organized in a Google Sheet, where a powerful backend script can be triggered to generate all necessary legal paperwork, such as cover sheets, metadata reports, and affidavits.

## Important Note for Developers

> **CRITICAL ARCHITECTURAL DIRECTIVE: DO NOT USE LOCAL DATABASES.**
>
> Under **NO CIRCUMSTANCES** should you attempt to implement a local database, such as **Room** or **SQLite**. The application's core design relies **exclusively** on the Google Sheets API for data storage.
>
> **This is a non-negotiable architectural constraint.**
>
> Any attempt to introduce a local database will be considered a fundamental misunderstanding of the project's design and will be rejected. This approach ensures that the user maintains full control and visibility of their data in a familiar format (Google Sheets) and simplifies the application by avoiding complex data synchronization logic.
>
> If a task appears to require data persistence, the solution **must** involve the Google Sheets API.

## Key Features

- **Automated Evidence Pipeline:** Capture images or screenshots and let the app handle the rest. The Lexorcist uses OCR to extract text and then feeds it into a powerful, user-scriptable engine for analysis.
- **Customizable Tagging Engine:** Define your own keywords, patterns, and "dorks" using JavaScript to automatically tag evidence. This allows you to create highly specific rules for identifying content relevant to your case, such as threats, hate speech, or contract violations.
- **Google Suite Integration:** Each case gets its own Google Sheet, where all evidence and its metadata are neatly organized. This serves as a central hub for your case data.
- **Automatic Document Generation:** The app leverages a powerful Google Apps Script backend. From your case's Google Sheet, you can instantly generate a variety of court-ready documents using customizable HTML templates.
- **Timeline View:** Visualize the chronology of your evidence with an interactive timeline, making it easy to see the sequence of events.
- **Secure and On-Device:** All OCR and data parsing happens on your device to ensure privacy and security.

## License

The Lexorcist is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
