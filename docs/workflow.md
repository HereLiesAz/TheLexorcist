# Core Application Workflow

This document outlines the primary workflow of The Lexorcist, from evidence capture to final document generation.

---

## 1. Evidence Capture and Processing

1.  **Import:** A user captures or imports an image, audio, or video file.
2.  **OCR & Transcription:** The app performs OCR on images or transcription on audio/video to extract text.
3.  **Formatting:** The extracted text is automatically formatted with Markdown code blocks.
4.  **Raw File Storage:** A copy of the original, raw evidence file is saved into a dedicated "raw" folder within the case directory.
5.  **Indexing:**
    *   If text was extracted, the evidence and its metadata (date, source, etc.) are saved and indexed.
    *   If no text is found, the file is indexed as "non-textual evidence."
6.  **Progress Reporting:** The entire process (loading, OCR/transcription, formatting, indexing) is made transparent to the user with a progress bar, a summary of the current task, and a detailed live log.

---

## 2. Scripting and Automation

-   **Script Runner:** After text is extracted, the app's script runner analyzes it using user-defined rules (keywords, patterns) to automatically apply relevant tags (e.g., "threat," "slander").
-   **Change Detection:** On every screen load, the app runs all active scripts on all evidence, but only if the evidence or the script has been modified since the last run.
-   **Google Apps Script Integration:** The scripting engine supports calling Google Apps Script functions, allowing for powerful automation with Google services.

---

## 3. Evidence Organization and Review

-   **Automatic Cleanup:** On the Review screen, users can initiate an "Automatic Cleanup" process. This feature is designed to:
    *   **Merge Image Series:** Combine sequential screenshots (e.g., of a long conversation) into a single PDF with merged text.
    *   **Detect Duplicates:** Identify and group duplicate evidence files based on file hashes or text similarity, allowing the user to merge them.
-   **Exhibit Creation:** Once cleaned, evidence is collected into **Exhibits**.
    *   Exhibits are thematic groupings of evidence designed to prove one or more required elements of a legal allegation.
    *   The app maintains a database of these legal elements to guide the user in creating effective exhibits.

---

## 4. Document Generation and Finalization

1.  **Paperwork Generation:**
    *   From the Review screen, the "Paperwork" button triggers the generation of all legal documents for the case.
    *   The process iterates through each Exhibit, finds its associated template, and uses a service (like Google Apps Script) to populate the template with the evidence data.
2.  **Case Finalization:**
    *   The "Finalize" button on the Review screen opens a dialog for the user to package the case.
    *   The user can select which files and folders from the case directory to include in the final archive.
    *   The archive can be exported as a standard `.zip` file or as a `.lex` file, which is a format The Lexorcist can directly import as a case archive or backup.