# Scripting in The Lexorcist

This document provides over 60 unique script examples for The Lexorcist's script builder. These scripts are designed to showcase the power and flexibility of the system, starting from simple keyword tagging and progressing to more complex, analytical, and even case-management-oriented functions.

## Scripting APIs

The scripting environment provides two main ways to interact with the application: a set of simple, global functions for common tasks, and a more powerful, namespaced `lex` object for advanced features.

---

## Part 1: Basic Scripting with Global Functions

These examples use the legacy global functions. They are simple, direct, and perfect for common tagging operations.

**Available Globals:**

*   `evidence`: An object containing the current piece of evidence being processed.
*   `case`: An object representing the entire case, including a list of all other evidence (`case.evidence`).
*   `addTag(tagName)`: Adds a tag to the current evidence.
*   `setSeverity(level)`: Sets a severity level for the evidence (e.g., "Low", "Medium", "High").
*   `linkToAllegation(allegationName)`: Links the evidence to a specific allegation.
*   `createNote(noteText)`: Adds a note to the evidence.

### **Level 1: Basic Tagging & Extraction**

**1. Profanity Tagger**
*   **Description:** Tags evidence containing common curse words.
*   **Script:**
    ```javascript
    const curses = ["fuck", "shit", "bitch", "asshole"];
    if (curses.some(word => evidence.text.toLowerCase().includes(word))) {
        addTag("Profanity");
    }
    ```

**2. Financial Transaction Tagger**
*   **Description:** Identifies and tags any mention of money or financial transactions.
*   **Script:**
    ```javascript
    const financialRegex = /[$€£¥]|\b(dollar|euro|yen|pound|payment|paid|owe|money)\b/i;
    if (financialRegex.test(evidence.text)) {
        addTag("Financial");
    }
    ```

**3. Contact Information Extractor**
*   **Description:** Finds and tags phone numbers and email addresses.
*   **Script:**
    ```javascript
    const emailRegex = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i;
    const phoneRegex = /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/;
    if (emailRegex.test(evidence.text)) { addTag("Email Address"); }
    if (phoneRegex.test(evidence.text)) { addTag("Phone Number"); }
    ```

**4. Appointment & Deadline Tagger**
*   **Description:** Scans for mentions of specific dates, appointments, or deadlines.
*   **Script:**
    ```javascript
    const deadlineRegex = /\b(appointment|meeting|deadline|due on|court date)\b/i;
    if (deadlineRegex.test(evidence.text)) {
        addTag("Deadline/Appointment");
    }
    ```

### **Level 2: Pattern Recognition & Simple Analysis**

**5. Gaslighting Detector**
*   **Description:** Identifies common gaslighting phrases.
*   **Script:**
    ```javascript
    const phrases = ["you're crazy", "that never happened", "you're imagining things", "you're too sensitive"];
    if (phrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Gaslighting");
        linkToAllegation("Emotional Abuse");
    }
    ```

**6. Threat Severity Assessor**
*   **Description:** Assigns a severity level based on the type of threat.
*   **Script:**
    ```javascript
    const low = ["i'll make you pay"];
    const med = ["i'll ruin your life"];
    const high = ["i'm going to hurt you", "i'll kill you"];
    const text = evidence.text.toLowerCase();

    if (high.some(p => text.includes(p))) { addTag("Threat"); setSeverity("Critical"); }
    else if (med.some(p => text.includes(p))) { addTag("Threat"); setSeverity("High"); }
    else if (low.some(p => text.includes(p))) { addTag("Threat"); setSeverity("Medium"); }
    ```

**7. Custody Violation Tagger**
*   **Description:** For family law, looks for violations of custody agreements.
*   **Script:**
    ```javascript
    const phrases = ["you can't see the kids", "i'm not bringing him back", "it's not your weekend"];
    if (phrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Custody Violation");
        linkToAllegation("Child Custody Interference");
    }
    ```

**8. Admission of Guilt Detector**
*   **Description:** Flags phrases that could be interpreted as an admission of wrongdoing.
*   **Script:**
    ```javascript
    const phrases = ["i know i messed up", "it was my fault", "i'm sorry i did that", "i shouldn't have"];
    if (phrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Admission");
    }
    ```

### **Level 3: Cross-Evidence Analysis**

**9. Stalking Pattern Detector**
*   **Description:** Correlates location mentions across multiple pieces of evidence.
*   **Script:**
    ```javascript
    const match = evidence.text.match(/i saw you at (.+?)\b/i);
    if (match) {
        addTag(`Location Mention: ${match[1]}`);
        const otherMentions = case.evidence.filter(e => e.tags.some(t => t.startsWith("Location Mention:")));
        if (otherMentions.length > 2) {
            addTag("Stalking Pattern");
            linkToAllegation("Stalking");
            createNote(`Multiple uninvited location mentions detected.`);
        }
    }
    ```

**10. Contradiction Finder**
*   **Description:** Scans other evidence to find direct contradictions.
*   **Script:**
    ```javascript
    const promiseMatch = evidence.text.match(/i promise to (pay you|give you)(.+?)\b/i);
    if (promiseMatch) {
        const item = promiseMatch[2].trim();
        const contradiction = case.evidence.find(e => {
            const denialMatch = e.text.match(/i never promised to (pay you|give you)(.+?)\b/i);
            return denialMatch && denialMatch[2].trim() === item;
        });
        if (contradiction) {
            addTag("Contradiction");
            createNote(`This contradicts evidence from ${new Date(contradiction.documentDate).toLocaleDateString()}.`);
        }
    }
    ```

---
## Part 2: Advanced Scripting with the `lex` Object

For advanced functionality, scripts can use the global `lex` object, which provides access to powerful subsystems for AI, UI, and Google Workspace integration.

### **`lex.ai.local` - On-Device Semantic AI**
*   `calculateSimilarity(text1, text2)`: Returns a similarity score (0.0 to 1.0) between two strings.

**11. Semantic Gaslighting Detector**
*   **Description:** Uses semantic similarity to detect phrases that are variations of gaslighting, even if they don't use exact keywords.
*   **Script:**
    ```javascript
    const examples = [
        "You are being irrational and overly emotional.",
        "That is not what happened, you are remembering it wrong.",
        "I was just joking, you are too sensitive."
    ];
    let isGaslighting = examples.some(ex => lex.ai.local.calculateSimilarity(evidence.text, ex) > 0.7);
    if (isGaslighting) {
        addTag("Gaslighting (Semantic)");
        linkToAllegation("Emotional Abuse");
    }
    ```

**12. Find Semantically Similar Evidence**
*   **Description:** Searches the entire case for other pieces of evidence that are semantically similar to the current one.
*   **Script:**
    ```javascript
    const similarEvidence = [];
    case.evidence.forEach(e => {
        if (e.id !== evidence.id) { // Don't compare to itself
            const similarity = lex.ai.local.calculateSimilarity(evidence.text, e.text);
            if (similarity > 0.8) { // High similarity threshold
                similarEvidence.push(`ID ${e.id} (${(similarity*100).toFixed(0)}%)`);
            }
        }
    });
    if (similarEvidence.length > 0) {
        createNote("Found similar evidence: " + similarEvidence.join(', '));
        addTag("Similar Evidence Found");
    }
    ```

### **`lex.ai.generate` - Cloud-Based Generative AI**
*   `generateContent(prompt)`: Generates text content based on a prompt using a powerful cloud AI.

**13. AI-Powered Evidence Summary**
*   **Description:** Generates a concise summary of long evidence text and adds it as a note.
*   **Script:**
    ```javascript
    if (evidence.text.length > 500) { // Only summarize long texts
        const prompt = "Summarize the following text for a legal case file in two sentences: " + evidence.text;
        const summary = lex.ai.generate.generateContent(prompt);
        if (summary) {
            createNote("AI Summary: " + summary);
            addTag("AI Summary");
        }
    }
    ```

### **`lex.ui` - Dynamic UI Manipulation**
*   `addOrUpdate(id, label, isVisible, onClickAction)`: Adds or updates a menu item in the navigation rail.
*   `remove(id)`: Removes a menu item.
*   `clearAll()`: Removes all scripted menu items.
*   **`onClickAction` format**: `command:payload` (e.g., `show_toast:Hello!`, `run_script:addTag('Clicked')`).

**14. Add a "Mark as Reviewed" Quick-Action Button**
*   **Description:** Adds a button to the main UI that, when clicked, runs a tiny script to tag the current evidence as reviewed.
*   **Script:**
    ```javascript
    const scriptToRun = `addTag('Manually Reviewed'); lex.ui.remove('mark_reviewed_btn');`;
    lex.ui.addOrUpdate(
        "mark_reviewed_btn",
        "Mark Reviewed",
        true,
        "run_script:" + scriptToRun
    );
    ```

**15. Create a Custom "AI Tip of the Day" Screen**
*   **Description:** Creates a new screen accessible from the main menu showing a helpful tip generated by the AI.
*   **Script:**
    ```javascript
    const tip = lex.ai.generate.generateContent("Give me a one-sentence productivity tip for lawyers.");
    const screenSchema = {
      title: "AI Tip of the Day",
      elements: [
        { type: "text", properties: { text: tip, size: 20 } }
      ]
    };
    const action = "scripted_screen/" + JSON.stringify(screenSchema);
    lex.ui.addOrUpdate("tip_of_the_day", "AI Tip", true, action);
    ```

### **`lex.google` - Google Workspace Integration**
*   `runAppsScript(scriptId, functionName, parameters)`: Executes a function in a Google Apps Script project.

**16. Generate a Document from a Template**
*   **Description:** Triggers a Google Apps Script to generate a formal document from a template, populating it with evidence data.
*   **Script:**
    ```javascript
    // Assumes a Google Apps Script is set up to handle this.
    const caseId = evidence.caseId;
    const templateId = "your_google_doc_template_id";
    const appsScriptId = "your_google_apps_script_id";

    const result = lex.google.runAppsScript(
        appsScriptId,
        "generateDocumentFromTemplate",
        [caseId, evidence.id, templateId]
    );

    if (result && result.documentUrl) {
        addTag("Doc Generated");
        createNote("Generated document: " + result.documentUrl);
    } else {
        addTag("Doc Gen Failed");
    }
    ```

---
*Note: The remaining scripts are conceptual examples demonstrating the potential of the scripting engine. Their full functionality may depend on future API additions.*