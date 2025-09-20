# Script Addons for The Lexorcist

This document provides a curated library of scripts designed to be used as "addons" within The Lexorcist application. They range from simple one-click tagging tools to powerful analysis and workflow automation.

For the purpose of these examples, we assume the script environment provides the following API:

*   `evidence`: An object containing the current piece of evidence being processed.
*   `case`: An object representing the entire case.
*   `addTag(tagName)`: A function to add a tag to the current piece of evidence.
*   `setSeverity(level)`: A function to set a severity level (e.g., "Low", "Medium", "High").
*   `linkToAllegation(allegationName)`: A function to link the evidence to a specific allegation.
*   `createNote(noteText)`: A function to add a note or observation to the evidence.
*   `AI.generate(prompt, callback)`: An asynchronous function that sends a prompt to a generative AI and returns the result to the callback function.
*   `MenuItem.setLabel(text)`, `MenuItem.setVisible(boolean)`: Functions to dynamically control a scriptable menu item in the UI.

---

### **Basic Tagging**

*Simple, one-click addons for common tagging operations.*

**1. Tag Financial Mentions**
*   **Description:** Scans the evidence text and adds a "Financial" tag if it finds any mention of money or financial transactions.
*   **Script:**
    ```javascript
    const financialRegex = /[$€£¥]|\b(dollar|euro|yen|pound|payment|paid|owe|money)\b/i;
    if (financialRegex.test(evidence.text)) {
        addTag("Financial");
    }
    ```

**2. Tag Appointments & Deadlines**
*   **Description:** Scans the evidence text and adds a "Deadline/Appointment" tag if it finds mentions of court dates, meetings, or other deadlines.
*   **Script:**
    ```javascript
    const deadlineRegex = /\b(appointment|meeting|deadline|due on|court date)\b/i;
    if (deadlineRegex.test(evidence.text)) {
        addTag("Deadline/Appointment");
    }
    ```

**3. Tag Custody Violations**
*   **Description:** Specifically for family law, this adds a "Custody Violation" tag if the text mentions phrases related to violating a custody agreement.
*   **Script:**
    ```javascript
    const violationPhrases = ["you can't see the kids", "i'm not bringing him back", "it's not your weekend"];
    if (violationPhrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Custody Violation");
        linkToAllegation("Child Custody Interference");
    }
    ```

---

### **Advanced Analysis**

*Scripts that perform more complex, multi-step analysis.*

**4. Analyze Communication Hazards**
*   **Description:** A comprehensive tool that scans for multiple types of communication hazards. It detects profanity, manipulative language (gaslighting, coercion), and direct threats, then applies all relevant tags and a severity level.
*   **Script:**
    ```javascript
    let severity = "None";
    let notes = [];

    const hazardChecks = {
        "Profanity": { keywords: ["f***", "s***"], note: "Contains profanity." },
        "Gaslighting": { keywords: ["you're crazy", "that never happened"], note: "Contains potential gaslighting phrases.", severity: "Medium" },
        "Coercion": { regex: /if you (don't|won't) (.+?), (then )?i will (.+)/i, note: "Contains a coercive 'if-then' statement.", severity: "High" },
        "Threat": { keywords: ["i'm going to hurt you", "i'll kill you"], note: "Contains a direct threat of violence.", severity: "Critical" }
    };

    for (const [hazard, check] of Object.entries(hazardChecks)) {
        const text = evidence.text.toLowerCase();
        let found = false;
        if (check.keywords && check.keywords.some(k => text.includes(k))) found = true;
        else if (check.regex && check.regex.test(evidence.text)) found = true;

        if (found) {
            addTag(hazard);
            notes.push(check.note);
            if (check.severity) {
                const severityLevels = { "Medium": 1, "High": 2, "Critical": 3 };
                if (severityLevels[check.severity] > (severityLevels[severity] || 0)) {
                    severity = check.severity;
                }
            }
        }
    }

    if (notes.length > 0) {
        addTag("Communication Hazard");
        createNote(`Hazard Analysis: ${notes.join(' ')}`);
        if (severity !== "None") setSeverity(severity);
    }
    ```

**5. Find Potential Witnesses**
*   **Description:** Scans evidence for mentions of other people (e.g., "I told [Name]...") who could be potential witnesses.
*   **Script:**
    ```javascript
    const witnessRegex = /\b(ask|talk to|with|saw|told) ([A-Z][a-z]+)\b/g;
    let match;
    while ((match = witnessRegex.exec(evidence.text)) !== null) {
        if (match[2].toLowerCase() !== "you" && match[2].toLowerCase() !== "me") {
            addTag(`Potential Witness: ${match[2]}`);
        }
    }
    ```

**6. Detect Evidence Gaps and Create Tasks**
*   **Description:** Scans text for mentions of other documents or conversations. If that item doesn't appear to be in the case file, it creates a task note to request the missing item.
*   **Script:**
    ```javascript
    const mentionedItems = {
        "document": /\b(contract|agreement|lease|receipt|invoice)\b/ig,
        "conversation": /\b(email|whatsapp|text|message)\b/ig
    };

    for (const [itemType, regex] of Object.entries(mentionedItems)) {
        const matches = [...evidence.text.matchAll(regex)];
        if (matches.length > 0) {
            for (const match of matches) {
                const keyword = match[0];
                const hasEvidence = case.evidence.some(e => e.text.toLowerCase().includes(keyword));
                if (!hasEvidence) {
                    addTag("Evidence Gap");
                    createNote(`TASK: Request missing ${itemType} related to '${keyword}' from evidence dated ${evidence.metadata.date}`);
                }
            }
        }
    }
    ```

---

### **Generative AI Addons**

*Powerful tools that leverage a real Generative AI service.*

**7. AI-Powered Summarization**
*   **Description:** Sends the evidence text to a generative AI to produce a concise, one-sentence summary and adds it as a note.
*   **Script:**
    ```javascript
    const prompt = "Summarize the following text in a single, neutral sentence: \n\n" + evidence.text;
    AI.generate(prompt, function(summary) {
        createNote("AI Summary: " + summary);
    });
    ```

**8. AI-Powered Sentiment Analysis**
*   **Description:** Asks a generative AI to analyze the sentiment of the text and return a score from -10 (very negative) to 10 (very positive).
*   **Script:**
    ```javascript
    const prompt = "Analyze the sentiment of the following text. Respond with only a number from -10 to 10, where -10 is extremely negative and 10 is extremely positive. Text: \n\n" + evidence.text;
    AI.generate(prompt, function(response) {
        const score = parseInt(response, 10);
        if (!isNaN(score)) {
            addTag(`AI Sentiment Score: ${score}`);
            if (score < -5) setSeverity("Medium");
        } else {
            createNote("AI Sentiment Analysis failed to return a valid score.");
        }
    });
    ```

**9. AI-Powered PII Detection**
*   **Description:** Uses generative AI to find any Personally Identifiable Information (PII) in the text, such as names, addresses, or phone numbers, and adds a warning tag.
*   **Script:**
    ```javascript
    const prompt = "Analyze the following text and list any Personally Identifiable Information (like names, addresses, phone numbers, emails, social security numbers) that you find. If you find none, respond with only the word 'None'. Text:\n\n" + evidence.text;
    AI.generate(prompt, function(response) {
        if (response.toLowerCase().trim() !== 'none') {
            addTag("PII Detected by AI");
            createNote("AI analysis found potential PII in this evidence: " + response);
            setSeverity("High");
        }
    });
    ```

---

### **Dynamic Menu Items**

*Scripts that dynamically change the custom menu item in the navigation rail.*

**10. Update Menu Item with Evidence Count**
*   **Description:** A simple script that sets the menu item's label to show the current number of evidence items in the case.
*   **Script:**
    ```javascript
    MenuItem.setLabel("Evidence: " + case.evidence.length);
    ```

**11. Show/Hide 'Flag Critical' Button**
*   **Description:** This script shows the menu item only if the currently selected evidence is *not* already marked as "Critical". Clicking it applies the "Critical" severity and then hides the button.
*   **Script:**
    ```javascript
    const isCritical = evidence.metadata.severity === "Critical";
    MenuItem.setVisible(!isCritical);
    MenuItem.setLabel("Flag as Critical");

    // The onClick for the menu item must be defined separately
    // in the UI where the script is attached. This script only controls the appearance.
    ```
