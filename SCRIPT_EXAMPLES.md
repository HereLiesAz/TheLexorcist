# Scripting in The Lexorcist

This document provides over 60 unique script examples for The Lexorcist's script builder. These scripts are designed to showcase the power and flexibility of the system, starting from simple keyword tagging and progressing to more complex, analytical, and even case-management-oriented functions.

## Scripting APIs

The scripting environment provides two main ways to interact with the application: a set of simple, global functions for common tasks, and a more powerful, namespaced `lex` object for advanced features.

### Global Functions & Variables (Legacy API)

For basic operations, scripts have access to the following global variables and functions:

*   `evidence`: An object containing the current piece of evidence being processed.
*   `case`: An object representing the entire case.
*   `tags`: A mutable array of strings. Scripts can add tags by calling `tags.push("My Tag")`.
*   `addTag(tagName)`: Adds a tag to the `tags` list.
*   `setSeverity(level)`: Sets a severity level for the evidence (e.g., "Low", "Medium", "High").
*   `linkToAllegation(allegationName)`: Links the evidence to a specific allegation.
*   `createNote(noteText)`: Adds a note to the evidence.

### The `lex` Object (Modern API)

For advanced functionality, scripts can use the global `lex` object, which provides access to powerful subsystems:

*   **`lex.ai.local`**: Interact with the on-device AI model (Legal-BERT) for semantic analysis.
    *   `getEmbedding(text: String): FloatArray`: Returns a 768-dimension vector representing the text.
    *   `calculateSimilarity(text1: String, text2: String): Double`: Returns the cosine similarity between two texts.
*   **`lex.ai.generate`**: Interact with the cloud-based generative AI (Gemini).
    *   `generateContent(prompt: String): String`: Generates text content based on a prompt.
*   **`lex.ui`**: Create and manage dynamic UI elements.
    *   `addOrUpdate(id, label, isVisible, onClickAction)`: Adds or updates a menu item in the navigation rail.
    *   `remove(id)`: Removes a menu item.
    *   `clearAll()`: Removes all scripted menu items.

---

### **Level 1: Basic Tagging & Extraction**

**1. Profanity Tagger**
*   **Description:** A simple script to tag any evidence containing common curse words. Useful for establishing a general tone of communication.
*   **Script:**
    ```javascript
    const curses = ["f***", "s***", "b****", "a******"]; // Add more as needed
    const text = evidence.text.toLowerCase();
    if (curses.some(word => text.includes(word))) {
        addTag("Profanity");
    }
    ```

... (contents of scripts 2-58) ...

**59. Identify the "Most Persuasive" Evidence**
*   **Description:** An AI ranks all evidence in the spreadsheet based on relevance, admissibility, and impact, then creates a "Top5Evidence" sheet with links to these key pieces.
*   **Script:**
    ```javascript
    Spreadsheet.createSheet("Top5Evidence");
    const allEvidence = Spreadsheet.query("Evidence", "SELECT *");
    const rankings = AI.analyze("PersuasivenessRanker", { evidence: allEvidence, allegations: case.allegations });
    rankings.slice(0, 5).forEach(item => {
        Spreadsheet.appendRow("Top5Evidence", { "EvidenceID": item.id, "Rank": item.rank, "Reason": item.reason });
    });
    ```

**60. AI-Powered Spreadsheet Query via Natural Language**
*   **Description:** A user writes a question in a "Queries" sheet. The script sends it to an AI that converts it into a formal query, executes it, and pastes the results into the sheet.
*   **Script:**
    ```javascript
    const newQueries = Spreadsheet.query("Queries", "SELECT * WHERE Result IS NULL");
    newQueries.forEach(query => {
        const formalQuery = AI.analyze("NLQtoSQL", { question: query.Question });
        try {
            const results = Spreadsheet.query(formalQuery.sheet, formalQuery.sql);
            const resultsAsText = JSON.stringify(results, null, 2);
            Spreadsheet.updateCell("Queries", `B${query.rowNumber}`, resultsAsText);
        } catch (e) {
            Spreadsheet.updateCell("Queries", `B${query.rowNumber}`, `Error: ${e.message}`);
        }
    });
    ```
---

## 61. Semantic Gaslighting Detector (Functional)
*   **Description:** This script uses semantic similarity to detect phrases that are variations of gaslighting, even if they don't use the exact keywords from a predefined list.
*   **Script:**
    ```javascript
    const gaslightingExamples = [
        "You are being irrational and overly emotional.",
        "That is not what happened, you are remembering it wrong.",
        "I was just joking, you are too sensitive.",
        "You are making a big deal out of nothing.",
        "I am sorry you feel that way."
    ];
    const evidenceText = evidence.text;
    let isGaslighting = false;
    gaslightingExamples.forEach(example => {
        if (lex.ai.local.calculateSimilarity(evidenceText, example) > 0.7) {
            isGaslighting = true;
        }
    });
    if (isGaslighting) {
        addTag("Gaslighting (Semantic)");
        linkToAllegation("Emotional Abuse");
    }
    ```

---

## 62. Simple Tutorial: "Hello, Dynamic Screens!"
*   **Description:** This script demonstrates the most basic use of the `lex.ui` object. It creates a new, clickable menu item in the main navigation rail. When you click this item, it opens a brand new screen that is completely defined by a JSON schema within the script.
*   **Script:**
    ```javascript
    const myScreenSchema = {
      title: "Hello World",
      elements: [
        { type: "text", properties: { text: "Welcome to your first scripted screen!", size: 20 } },
        { type: "spacer", properties: { height: 16 } },
        { type: "button", properties: { label: "Say Hello", onClickAction: "show_toast:Hello from a scripted button!" } }
      ]
    };
    lex.ui.addOrUpdate("my_first_screen_button", "My Screen", true, "scripted_screen/" + JSON.stringify(myScreenSchema));
    ```

---

## 63. Advanced Tutorial: AI-Powered In-App Guide
*   **Description:** This script demonstrates combining the AI and UI APIs. It creates a "Tutorial & Tip" menu item that opens a custom screen featuring a "Tip of the Day" freshly generated by the AI.
*   **Script:**
    ```javascript
    const tipPrompt = "In one sentence, give me a useful and uncommon productivity tip for legal professionals.";
    // Note: This uses the cloud-based generative AI, not the local one.
    const aiGeneratedTip = lex.ai.generate.generateContent(tipPrompt);
    const tutorialSchema = {
      title: "App Tutorial",
      elements: [
        { type: "text", properties: { text: "Welcome to The Lexorcist!", size: 24 } },
        { type: "text", properties: { text: "This app helps you organize, analyze, and build your case narrative.", size: 16 } },
        { type: "spacer", properties: { height: 24 } },
        { type: "text", properties: { text: "AI Tip of the Day:", size: 20 } },
        { type: "text", properties: { text: aiGeneratedTip, size: 16 } },
        { type: "spacer", properties: { height: 24 } },
        { type: "text", properties: { text: "Use the navigation rail on the left to manage Cases, Evidence, and more.", size: 16 } }
      ]
    };
    lex.ui.addOrUpdate("ai_tutorial_screen_button", "Tutorial & Tip", true, "scripted_screen/" + JSON.stringify(tutorialSchema));
    ```
