# 20 Example Scripts for The Lexorcist

This document provides 20 unique script examples for The Lexorcist's script builder. These scripts are designed to showcase the power and flexibility of the system, starting from simple keyword tagging and progressing to more complex, analytical, and even case-management-oriented functions.

For the purpose of these examples, we assume the script environment provides the following API:

*   `evidence`: An object containing the current piece of evidence being processed.
    *   `evidence.text`: The text content of the evidence (from OCR or transcription).
    *   `evidence.metadata`: An object with metadata like `date`, `source`, etc.
*   `case`: An object representing the entire case.
    *   `case.allegations`: A list of allegations for the case.
    *   `case.evidence`: A list of all other evidence in the case.
*   `addTag(tagName)`: A function to add a tag to the current piece of evidence.
*   `setSeverity(level)`: A function to set a severity level (e.g., "Low", "Medium", "High", "Critical").
*   `linkToAllegation(allegationName)`: A function to link the evidence to a specific allegation.
*   `createNote(noteText)`: A function to add a note or observation to the evidence.

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

**2. Financial Mention Tagger**
*   **Description:** Identifies and tags any mention of money or financial transactions.
*   **Script:**
    ```javascript
    const financialRegex = /[$€£¥]|\b(dollar|euro|yen|pound|payment|paid|owe|money)\b/i;
    if (financialRegex.test(evidence.text)) {
        addTag("Financial");
    }
    ```

**3. Contact Information Extractor**
*   **Description:** Finds and tags phone numbers and email addresses, making it easy to find contact details later.
*   **Script:**
    ```javascript
    const emailRegex = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i;
    const phoneRegex = /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/;
    if (emailRegex.test(evidence.text)) {
        addTag("Email Address");
    }
    if (phoneRegex.test(evidence.text)) {
        addTag("Phone Number");
    }
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

---

### **Level 2: Pattern Recognition & Simple Analysis**

**5. Gaslighting Detector**
*   **Description:** Identifies phrases commonly used in gaslighting to deny a person's reality.
*   **Script:**
    ```javascript
    const gaslightingPhrases = ["you're crazy", "that never happened", "you're imagining things", "you're too sensitive", "I never said that"];
    const text = evidence.text.toLowerCase();
    if (gaslightingPhrases.some(phrase => text.includes(phrase))) {
        addTag("Gaslighting");
        linkToAllegation("Emotional Abuse");
    }
    ```

**6. Threat Severity Assessor**
*   **Description:** A more nuanced threat detector that assigns a severity level based on the type of threat.
*   **Script:**
    ```javascript
    const lowSeverity = ["i'll make you pay"];
    const medSeverity = ["i'll ruin your life"];
    const highSeverity = ["i'm going to hurt you", "i'll kill you"];
    const text = evidence.text.toLowerCase();

    if (highSeverity.some(p => text.includes(p))) {
        addTag("Threat");
        setSeverity("Critical");
    } else if (medSeverity.some(p => text.includes(p))) {
        addTag("Threat");
        setSeverity("High");
    } else if (lowSeverity.some(p => text.includes(p))) {
        addTag("Threat");
        setSeverity("Medium");
    }
    ```

**7. Custody Violation Tagger**
*   **Description:** Specifically for family law cases, this script looks for violations of custody agreements.
*   **Script:**
    ```javascript
    const violationPhrases = ["you can't see the kids", "i'm not bringing him back", "it's not your weekend"];
    if (violationPhrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Custody Violation");
        linkToAllegation("Child Custody Interference");
    }
    ```

**8. Admission of Guilt Detector**
*   **Description:** Flags phrases that could be interpreted as an admission of wrongdoing.
*   **Script:**
    ```javascript
    const admissionPhrases = ["i know i messed up", "it was my fault", "i'm sorry i did that", "i shouldn't have"];
    if (admissionPhrases.some(p => evidence.text.toLowerCase().includes(p))) {
        addTag("Admission");
    }
    ```

---

### **Level 3: Cross-Evidence Analysis**

**9. Stalking Pattern Detector**
*   **Description:** This script looks for patterns of stalking by correlating location mentions across multiple pieces of evidence.
*   **Script:**
    ```javascript
    const locationRegex = /i saw you at (.+?)\b/i;
    const match = evidence.text.match(locationRegex);
    if (match) {
        const location = match[1];
        addTag(`Location Mention: ${location}`);
        const otherMentions = case.evidence.filter(e =>
            e.tags.some(t => t.startsWith("Location Mention:"))
        );
        if (otherMentions.length > 2) {
            addTag("Stalking Pattern");
            linkToAllegation("Stalking");
            createNote(`Multiple uninvited location mentions detected. Potential stalking behavior.`);
        }
    }
    ```

**10. Contradiction Finder**
*   **Description:** Scans other evidence to find direct contradictions.
*   **Script:**
    ```javascript
    const promiseRegex = /i promise to (pay you|give you)(.+?)\b/i;
    const denialRegex = /i never promised to (pay you|give you)(.+?)\b/i;

    const promiseMatch = evidence.text.match(promiseRegex);
    if (promiseMatch) {
        const promisedItem = promiseMatch[2].trim();
        const contradiction = case.evidence.find(e => {
            const denialMatch = e.text.match(denialRegex);
            return denialMatch && denialMatch[2].trim() === promisedItem;
        });
        if (contradiction) {
            addTag("Contradiction");
            createNote(`This contradicts evidence from ${contradiction.metadata.date}.`);
        }
    }
    ```

**11. Escalation Tracker**
*   **Description:** Tracks the severity of threats over time to show a pattern of escalation.
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

**12. Alibi Corroborator/Breaker**
*   **Description:** Checks for consistencies or inconsistencies in alibis.
*   **Script:**
    ```javascript
    const alibiRegex = /i was at (.+?) at (\d{1,2}:\d{2}[ap]m)/i;
    const match = evidence.text.match(alibiRegex);
    if (match) {
        const location = match[1];
        const time = match[2];
        addTag(`Alibi: ${location} at ${time}`);
        const conflictingEvidence = case.evidence.find(e =>
            e.text.includes(`i saw you at a different place at ${time}`)
        );
        if (conflictingEvidence) {
            addTag("Alibi Conflict");
        }
    }
    ```

---

### **Level 4: Advanced Analysis & Case Management**

**13. Third-Party Witness Identifier**
*   **Description:** Scans for mentions of other people who could be potential witnesses.
*   **Script:**
    ```javascript
    const witnessRegex = /\b(ask|talk to|with|saw|told) ([A-Z][a-z]+)\b/g;
    let match;
    while ((match = witnessRegex.exec(evidence.text)) !== null) {
        const witnessName = match[2];
        if (witnessName.toLowerCase() !== "you") {
            addTag(`Potential Witness: ${witnessName}`);
        }
    }
    ```

**14. Document Request Generator**
*   **Description:** Identifies mentions of documents that you might need to request in discovery.
*   **Script:**
    ```javascript
    const docRegex = /\b(contract|agreement|lease|receipt|invoice|bank statement)\b/ig;
    let match;
    while ((match = docRegex.exec(evidence.text)) !== null) {
        addTag("Mentioned Document");
        createNote(`Discovery suggestion: Request the '${match[0]}' mentioned here.`);
    }
    ```

**15. Communication Blackout Detector**
*   **Description:** Identifies sudden and prolonged gaps in communication, which can be significant.
*   **Script:**
    ```javascript
    const lastCommunication = case.evidence
        .filter(e => e.metadata.source === evidence.metadata.source)
        .sort((a, b) => new Date(b.metadata.date) - new Date(a.metadata.date))[1]; // Get the one before this one

    if (lastCommunication) {
        const daysSince = (new Date(evidence.metadata.date) - new Date(lastCommunication.metadata.date)) / (1000 * 3600 * 24);
        if (daysSince > 14) { // 2-week gap
            addTag("Communication Gap");
            createNote(`A ${Math.round(daysSince)}-day gap in communication occurred before this message.`);
        }
    }
    ```

**16. "If This, Then That" Coercion Analyzer**
*   **Description:** A more advanced script to detect coercive "if-then" statements.
*   **Script:**
    ```javascript
    const coercionRegex = /if you (don't|won't) (.+?), (then )?i will (.+)/i;
    const match = evidence.text.match(coercionRegex);
    if (match) {
        const demand = match[2];
        const consequence = match[4];
        addTag("Coercion");
        linkToAllegation("Coercion/Blackmail");
        createNote(`Coercive statement detected. Demand: '${demand}', Consequence: '${consequence}'.`);
    }
    ```

---

### **Level 5: AI-Powered & Generative Scripts (Conceptual)**

*These scripts might require more advanced capabilities than simple JS, like hypothetical AI/ML libraries available in the script environment.*

**17. Sentiment Shift Analysis**
*   **Description:** Uses a hypothetical sentiment analysis library to track emotional tone over time and flags drastic shifts.
*   **Script:**
    ```javascript
    // Assumes a hypothetical Sentiment library
    const currentSentiment = Sentiment.analyze(evidence.text).score; // e.g., -0.8 (very negative)
    const recentEvidence = case.evidence.slice(-5);
    const avgPastSentiment = recentEvidence.reduce((acc, e) => acc + Sentiment.analyze(e.text).score, 0) / recentEvidence.length;

    if (Math.abs(currentSentiment - avgPastSentiment) > 1.5) { // Detects a major shift
        addTag("Sentiment Shift");
        createNote(`Drastic sentiment shift from avg ${avgPastSentiment.toFixed(2)} to ${currentSentiment.toFixed(2)}.`);
    }
    ```

**18. Legal Element Mapper**
*   **Description:** Tries to map evidence to the specific legal elements of an allegation. For example, for "Breach of Contract", it looks for evidence of an offer, acceptance, and a breach.
*   **Script:**
    ```javascript
    if (case.allegations.includes("Breach of Contract")) {
        if (evidence.text.toLowerCase().includes("i accept your offer")) {
            addTag("Contract - Acceptance");
        }
        if (evidence.text.toLowerCase().includes("failed to deliver")) {
            addTag("Contract - Breach");
        }
    }
    ```

**19. Evidence Gap Suggester**
*   **Description:** A smart script that suggests what evidence might be missing.
*   **Script:**
    ```javascript
    if (evidence.tags.includes("Mentioned Document")) {
        const docName = evidence.text.match(/\b(contract|agreement|etc)\b/i)[0];
        const hasDocument = case.evidence.some(e => e.metadata.type === 'document' && e.metadata.title.includes(docName));
        if (!hasDocument) {
            createNote(`Missing Evidence: The '${docName}' mentioned here has not been added to the case.`);
        }
    }
    ```

**20. Narrative Summary Generator**
*   **Description:** The ultimate script. For a given allegation, it pulls all linked evidence and attempts to create a chronological summary of events.
*   **Script:**
    ```javascript
    // This is highly conceptual
    if (case.allegations.includes("Harassment")) {
        const harassmentEvidence = case.evidence
            .filter(e => e.allegations.includes("Harassment"))
            .sort((a, b) => new Date(a.metadata.date) - new Date(a.metadata.date));

        let summary = "Summary of Harassment:\n";
        harassmentEvidence.forEach(e => {
            summary += `- On ${e.metadata.date}, a message containing '${e.tags.join(', ')}' was received.\n`;
        });

        // In a real scenario, this would be saved to a case summary file or note.
        // For now, we'll add a note to the last piece of evidence.
        if (evidence.id === harassmentEvidence.slice(-1)[0].id) {
            createNote(summary);
        }
    }
    ```
