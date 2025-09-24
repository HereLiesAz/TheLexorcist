# The Lexorcist

The Lexorcist empowers lawyers and their clients to capture, automatically analyze, and process digital evidence, transforming raw data into court-ready documentation with powerful, scriptable automation.

Whether you're dealing with harassment screenshots, recorded threats,  slanderous slander, or proof of efforts, The Lexorcist provides the tools to streamline the entire workflow—from a picture on a phone to a fully prepared exhibit. A lawyer can literally hand this app to their client and let the legal assistants and interns take a long coffee break. No more spinning heads and green vomit. It's like Holy Water to legalitarians facing the demonic tedium of legal discovery.  You're on your own with the raspy voice and growls of profanity. 

## The Vision

This project is in an advanced stage of development, with a robust architecture designed to realize a specific vision: to create a seamless pipeline where a user can take a photo of evidence, have the app's OCR extract the text, and then use a custom, user-defined rules engine to automatically tag and categorize that evidence. This categorized data is then organized in a Google Sheet, where a powerful backend script can be triggered to generate all necessary legal paperwork, such as cover sheets, metadata reports, and affidavits.

## Key Features

- **Automated Evidence Pipeline:** Capture images or screenshots and let the app handle the rest. The Lexorcist uses OCR to extract text and then feeds it into a powerful, user-scriptable engine for analysis.
- **Customizable Tagging Engine:** Define your own keywords, patterns, and "dorks" using JavaScript to automatically tag evidence. This allows you to create highly specific rules for identifying content relevant to your case, such as threats, hate speech, or contract violations.
    - **AI-Powered Analysis:** Go beyond simple keywords. The scripting engine now includes a local, on-device AI for semantic analysis (understanding the *meaning* of text) and a cloud-based generative AI for creating new content, allowing for incredibly sophisticated and nuanced evidence processing.
- **Google Suite Integration:** Each case gets its own Google Sheet, where all evidence and its metadata are neatly organized. This serves as a central hub for your case data.
- **Automatic Document Generation:** The app leverages a powerful Google Apps Script backend. From your case's Google Sheet, you can instantly generate a variety of court-ready documents using customizable HTML templates.
- **Timeline View:** Visualize the chronology of your evidence with an interactive timeline, making it easy to see the sequence of events.
- **Secure and On-Device:** All OCR and data parsing happens on your device to ensure privacy and security.
- **Transparent Progress:** See detailed, real-time logs of the evidence processing pipeline, so you know exactly what's happening with your data at all times.
- **Configurable Storage:** Choose where your case folders are stored on your device and enable or disable cloud synchronization to fit your workflow.

## Localization

This application and its documentation are also available in Spanish.

- [Política de privacidad (Privacy Policy)](PRIVACY_POLICY_ES.md)
- [Términos de servicio (Terms of Service)](TERMS_OF_SERVICE_ES.md)
- [Ejemplos de scripts (Script Examples)](SCRIPT_EXAMPLES_ES.md)

