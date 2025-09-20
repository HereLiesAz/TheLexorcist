# Script Addons for The Lexorcist

This document provides a curated library of scripts designed to be used as "addons" within The Lexorcist application. They range from simple one-click tagging tools to powerful analysis and workflow automation.

For the purpose of these examples, we assume the script environment provides the following API:

*   `evidence`: An object containing the current piece of evidence being processed.
*   `case`: An object representing the entire case.
*   `addTag(tagName)`: A function to add a tag to the current piece of evidence.
*   `setSeverity(level)`: A function to set a severity level (e.g., "Low", "Medium", "High").
*   `linkToAllegation(allegationName)`: A function to link the evidence to a specific allegation.
*   `createNote(noteText)`: A function to add a note or observation to the evidence.

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

**4. Tag Admissions of Guilt**
*   **Description:** Flags evidence with an "Admission" tag if it contains phrases that could be interpreted as an admission of wrongdoing.
*   **Script:**
    ```javascript
    const admissionPhrases = ["i know i messed up", "it was my fault", "i'm sorry i did that", "i shouldn't have"];
    if (admissionPhrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Admission");
    }
    ```

---

### **Communication Analysis**

*Scripts that analyze the tone, style, and content of communications.*

**5. Analyze Communication Hazards**
*   **Description:** A comprehensive tool that scans for multiple types of communication hazards. It detects profanity, manipulative language (gaslighting, coercion), and direct threats, then applies all relevant tags and a severity level.
*   **Script:**
    ```javascript
    let severity = "None";
    let notes = [];

    const hazardChecks = {
        "Profanity": {
            keywords: ["f***", "s***"],
            note: "Contains profanity."
        },
        "Gaslighting": {
            keywords: ["you're crazy", "that never happened", "you're too sensitive"],
            note: "Contains potential gaslighting phrases.",
            severity: "Medium"
        },
        "Coercion": {
            regex: /if you (don't|won't) (.+?), (then )?i will (.+)/i,
            note: "Contains a coercive 'if-then' statement.",
            severity: "High"
        },
        "Threat": {
            keywords: ["i'm going to hurt you", "i'll kill you"],
            note: "Contains a direct threat of violence.",
            severity: "Critical"
        }
    };

    for (const [hazard, check] of Object.entries(hazardChecks)) {
        const text = evidence.text.toLowerCase();
        let found = false;
        if (check.keywords && check.keywords.some(k => text.includes(k))) {
            found = true;
        } else if (check.regex && check.regex.test(evidence.text)) {
            found = true;
        }

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
        if (severity !== "None") {
            setSeverity(severity);
        }
    }
    ```

**6. Track Threat Escalation**
*   **Description:** If evidence is already tagged as a "Threat", this script compares its severity to past threats from the same person to identify and tag escalating behavior.
*   **Script:**
    ```javascript
    if (evidence.tags.includes("Threat")) {
        const pastThreats = case.evidence.filter(e =>
            e.tags.includes("Threat") && new Date(e.metadata.date) < new Date(evidence.metadata.date)
        );
        const pastSeverity = pastThreats.map(e => e.metadata.severity);
        if (pastSeverity.includes("Medium") && evidence.metadata.severity === "High") {
            addTag("Escalating Behavior");
            createNote("Severity has escalated from Medium to High.");
        }
    }
    ```

**7. Analyze Communication Style**
*   **Description:** Analyzes the linguistic style (e.g., sentence length, word choice) of a piece of evidence and compares it to the author's known baseline from other evidence. Flags messages that may have been written by someone else.
*   **Script:**
    ```javascript
    function getAvgSentenceLength(text) {
        const sentences = text.split(/[.!?]+/).filter(s => s.length > 0);
        return text.length / sentences.length;
    }

    const authorEvidence = case.evidence.filter(e => e.metadata.author === evidence.metadata.author && e.id !== evidence.id);
    if (authorEvidence.length > 5) { // Only run if there's a decent baseline
        const baselineText = authorEvidence.map(e => e.text).join(' ');
        const baselineAvg = getAvgSentenceLength(baselineText);
        const currentAvg = getAvgSentenceLength(evidence.text);

        // If current average is more than 50% different from the baseline
        if (Math.abs(currentAvg - baselineAvg) / baselineAvg > 0.5) {
            addTag("Atypical Style");
            createNote(`Communication style is inconsistent with author's baseline. (Current: ${currentAvg.toFixed(1)} chars/sentence, Baseline: ${baselineAvg.toFixed(1)})`);
        }
    }
    ```

---

### **Evidence & Case Integrity**

*Tools for finding gaps, conflicts, and anomalies in your evidence.*

**8. Find Potential Witnesses**
*   **Description:** Scans evidence for mentions of other people (e.g., "I told [Name]...") who could be potential witnesses and adds a "Potential Witness" tag.
*   **Script:**
    ```javascript
    const witnessRegex = /\b(ask|talk to|with|saw|told) ([A-Z][a-z]+)\b/g;
    let match;
    while ((match = witnessRegex.exec(evidence.text)) !== null) {
        const witnessName = match[2];
        if (witnessName.toLowerCase() !== "you" && witnessName.toLowerCase() !== "me") {
            addTag(`Potential Witness: ${witnessName}`);
        }
    }
    ```

**9. Detect Evidence Gaps**
*   **Description:** Scans text for mentions of other documents (e.g., "the contract") or conversations (e.g., "in the email"). If that item doesn't appear to be in the case file, it tags the current evidence as having an "Evidence Gap" and creates a task note to request the missing item.
*   **Script:**
    ```javascript
    const mentionedItems = {
        "document": /\b(contract|agreement|lease|receipt|invoice)\b/ig,
        "email": /\b(email|emailed)\b/ig,
        "message": /\b(whatsapp|text|message)\b/ig
    };

    for (const [itemType, regex] of Object.entries(mentionedItems)) {
        const matches = [...evidence.text.matchAll(regex)];
        if (matches.length > 0) {
            for (const match of matches) {
                const keyword = match[1];
                const hasEvidence = case.evidence.some(e => e.text.toLowerCase().includes(keyword));

                if (!hasEvidence) {
                    addTag("Evidence Gap");
                    const note = `Evidence mentions a '${keyword}' (${itemType}) that may be missing.`;
                    createNote(note);
                    createNote(`TASK: Request missing ${itemType} related to '${keyword}' mentioned in evidence from ${evidence.metadata.date}`);
                }
            }
        }
    }
    ```

**10. Identify Gaps in Communication**
*   **Description:** Checks the dates on messages from the same source and adds a "Communication Gap" tag if it finds an unusually long silence (e.g., more than 14 days).
*   **Script:**
    ```javascript
    const lastCommunication = case.evidence
        .filter(e => e.metadata.source === evidence.metadata.source && new Date(e.metadata.date) < new Date(evidence.metadata.date))
        .sort((a, b) => new Date(b.metadata.date) - new Date(a.metadata.date))[0];

    if (lastCommunication) {
        const daysSince = (new Date(evidence.metadata.date) - new Date(lastCommunication.metadata.date)) / (1000 * 3600 * 24);
        if (daysSince > 14) {
            addTag("Communication Gap");
            createNote(`A ${Math.round(daysSince)}-day gap in communication occurred before this message.`);
        }
    }
    ```

**11. Check for Alibi Conflicts**
*   **Description:** Scans all evidence to see if a stated alibi (e.g., "I was at home at 8pm") conflicts with information from another piece of evidence.
*   **Script:**
    ```javascript
    const alibiRegex = /i was at (.+?) at (\d{1,2}:\d{2}[ap]m)/i;
    const match = evidence.text.match(alibiRegex);
    if (match) {
        const location = match[1].trim();
        const time = match[2];
        addTag(`Alibi: ${location} at ${time}`);

        const conflictingEvidence = case.evidence.find(e => {
            const conflictRegex = new RegExp(`i saw you at (?!${location})(.+?) at ${time}`, "i");
            return conflictRegex.test(e.text);
        });

        if (conflictingEvidence) {
            addTag("Alibi Conflict");
            createNote(`This alibi may conflict with evidence from ${conflictingEvidence.metadata.date}.`);
        }
    }
    ```

**12. Find Duplicate Evidence**
*   **Description:** Uses a file's checksum (a unique digital fingerprint, if available in metadata) to find duplicate or near-duplicate pieces of evidence.
*   **Script:**
    ```javascript
    if (evidence.metadata.phash) {
        const duplicates = case.evidence.filter(e =>
            e.id !== evidence.id && e.metadata.phash === evidence.metadata.phash
        );
        if (duplicates.length > 0) {
            addTag("Duplicate");
            createNote(`This is a duplicate of evidence: ${duplicates.map(d => d.id).join(', ')}.`);
        }
    }
    ```

**13. Check for Metadata Anomalies**
*   **Description:** Compares the creation date from a photo's metadata (EXIF, if available) to the date the user provided, flagging any significant discrepancies.
*   **Script:**
    ```javascript
    if (evidence.metadata.exif && evidence.metadata.exif.dateCreated) {
        const exifDate = new Date(evidence.metadata.exif.dateCreated);
        const providedDate = new Date(evidence.metadata.date);
        const diffDays = Math.abs(exifDate - providedDate) / (1000 * 3600 * 24);
        if (diffDays > 1) {
            addTag("Metadata Anomaly");
            createNote(`EXIF creation date (${exifDate.toDateString()}) differs from provided date (${providedDate.toDateString()}).`);
            setSeverity("High");
        }
    }
    ```

---

### **Heuristic & AI-like Analysis**

*Advanced tools that use self-contained logic to approximate AI-driven insights.*

**14. Heuristic Sentiment Analysis**
*   **Description:** Scores the emotional tone of the text by looking for positive and negative keywords defined within the script.
*   **Script:**
    ```javascript
    const sentimentDict = {
        "happy": 2, "love": 3, "great": 2, // positive
        "sad": -2, "hate": -3, "terrible": -2, "angry": -2, // negative
        "promise": 1, "help": 1,
        "threat": -3, "stupid": -2, "idiot": -2
    };
    const words = evidence.text.toLowerCase().split(/\s+/);
    let score = 0;
    words.forEach(word => {
        if (sentimentDict[word]) {
            score += sentimentDict[word];
        }
    });

    addTag(`Sentiment Score: ${score}`);
    if (score > 5) {
        addTag("Positive Sentiment");
    } else if (score < -5) {
        addTag("Negative Sentiment");
        setSeverity("Medium");
    }
    ```

**15. Basic Text Summarizer**
*   **Description:** Creates a simple summary of the evidence by extracting the first and last sentences.
*   **Script:**
    ```javascript
    const sentences = evidence.text.match( /[^\.!\?]+[\.!\?]+/g );
    if (sentences && sentences.length > 2) {
        const first = sentences[0].trim();
        const last = sentences[sentences.length - 1].trim();
        const summary = `Summary: "${first} ... ${last}"`;
        createNote(summary);
    }
    ```

**16. Heuristic Authenticity Score**
*   **Description:** Calculates a simple "Authenticity Score" based on available metadata. The score decreases if the EXIF data shows signs of editing software or date mismatches.
*   **Script:**
    ```javascript
    let score = 100;
    let notes = [];

    if (evidence.metadata.exif) {
        // Check for common editing software in EXIF
        const software = evidence.metadata.exif.software || "";
        if (software.toLowerCase().includes("photoshop") || software.toLowerCase().includes("gimp")) {
            score -= 40;
            notes.push("Evidence may have been processed with editing software.");
        }

        // Check for date mismatch
        if (evidence.metadata.exif.dateCreated) {
            const diffDays = Math.abs(new Date(evidence.metadata.exif.dateCreated) - new Date(evidence.metadata.date)) / (1000 * 3600 * 24);
            if (diffDays > 1) {
                score -= 25;
                notes.push("EXIF creation date and provided date do not match.");
            }
        }
    } else {
        score -= 10; // No EXIF data is a small penalty
        notes.push("No EXIF metadata available for verification.");
    }

    addTag(`Authenticity Score: ${score}/100`);
    if(notes.length > 0) {
        createNote(notes.join(" "));
    }
    ```
