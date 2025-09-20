# Proposal for Consolidating and Refining Script Examples

## 1. Introduction

This document outlines a proposal to refine the `SCRIPT_EXAMPLES.md` file by consolidating redundant scripts and merging overly simplistic ones into more powerful, comprehensive examples.

The goal is to:
*   **Reduce Redundancy:** Eliminate examples that perform very similar functions.
*   **Increase Power:** Create more robust scripts that combine multiple related checks into a single, more effective tool.
*   **Improve Clarity:** Focus the script library on quality over quantity, making it easier for users to find powerful and relevant examples.

## 2. Proposed Mergers & Consolidations

I have identified three primary areas where multiple scripts can be merged into a single, more sophisticated script.

### Proposal A: Comprehensive PII & Redaction Analyzer

*   **Scripts to be Merged:**
    *   `3. Contact Information Extractor` (finds email/phone)
    *   `27. Automatic Redaction Suggester` (finds SSN/Bank Account)
*   **Reasoning:** These scripts are both performing regex-based PII detection. A user would likely want to find all PII at once, not just one type. Combining them creates a single, powerful tool for identifying and flagging all types of sensitive information.
*   **New Consolidated Script:**
    *   **Name:** `Comprehensive PII Analyzer`
    *   **Description:** A single script that uses a dictionary of regex patterns to find various types of PII, including emails, phone numbers, SSNs, and bank account numbers. When found, it applies a generic `PII Detected` tag, a specific tag for the type (e.g., `PII: Email`), and adds a note suggesting the document be reviewed for redaction.
    *   **Code:**
        ```javascript
        const piiPatterns = {
            "Email Address": /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi,
            "Phone Number": /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/g,
            "Social Security Number": /\b\d{3}-\d{2}-\d{4}\b/g,
            "Bank Account Number": /\b\d{10,16}\b/g
        };

        let piiFound = false;
        for (const [name, regex] of Object.entries(piiPatterns)) {
            if (regex.test(evidence.text)) {
                piiFound = true;
                addTag(`PII: ${name}`);
            }
        }

        if (piiFound) {
            addTag("PII Detected");
            createNote("This evidence contains Personally Identifiable Information. Review for redaction before sharing.");
            setSeverity("High");
        }
        ```

### Proposal B: Advanced Communication Hazard Analyzer

*   **Scripts to be Merged:**
    *   `1. Profanity Tagger`
    *   `5. Gaslighting Detector`
    *   `6. Threat Severity Assessor`
    *   `16. "If This, Then That" Coercion Analyzer`
*   **Reasoning:** These scripts all analyze text for negative, manipulative, or threatening communication styles. A single, unified script provides a more holistic view of communication risk rather than requiring the user to run four separate analyses.
*   **New Consolidated Script:**
    *   **Name:** `Advanced Communication Hazard Analyzer`
    *   **Description:** A multi-stage script that checks for a variety of communication hazards. It tags profanity, identifies common gaslighting and coercive phrases, and assesses threat levels, applying all relevant tags and setting the highest detected severity.
    *   **Code:**
        ```javascript
        let severity = "None";
        let notes = [];

        // Stage 1: Profanity
        const curses = ["f***", "s***"]; // Abridged for example
        if (curses.some(word => evidence.text.toLowerCase().includes(word))) {
            addTag("Profanity");
            notes.push("Contains profanity.");
        }

        // Stage 2: Gaslighting
        const gaslightingPhrases = ["you're crazy", "that never happened"];
        if (gaslightingPhrases.some(phrase => evidence.text.toLowerCase().includes(phrase))) {
            addTag("Gaslighting");
            notes.push("Contains potential gaslighting phrases.");
            severity = "Medium";
        }

        // Stage 3: Coercion
        const coercionRegex = /if you (don't|won't) (.+?), (then )?i will (.+)/i;
        if (coercionRegex.test(evidence.text)) {
            addTag("Coercion");
            notes.push("Contains a coercive 'if-then' statement.");
            severity = "High";
        }

        // Stage 4: Threat Assessment
        const highSeverityThreats = ["i'm going to hurt you", "i'll kill you"];
        if (highSeverityThreats.some(p => evidence.text.toLowerCase().includes(p))) {
            addTag("Threat");
            notes.push("Contains a direct threat of violence.");
            severity = "Critical";
        }

        // Finalization
        if (notes.length > 0) {
            addTag("Communication Hazard");
            createNote(`Hazard Analysis: ${notes.join(' ')}`);
            if (severity !== "None") {
                setSeverity(severity);
            }
        }
        ```

### Proposal C: Unified Evidence Gap Detector

*   **Scripts to be Merged:**
    *   `14. Document Request Generator`
    *   `19. Evidence Gap Suggester`
    *   `67. Proactive Evidence Request Task`
*   **Reasoning:** All three scripts aim to identify what's *missing* from the case file based on the text of existing evidence. Combining them creates a single, proactive tool that not only finds these gaps but also helps the user act on them.
*   **New Consolidated Script:**
    *   **Name:** `Unified Evidence Gap Detector`
    *   **Description:** This script scans text for mentions of documents (e.g., "contract") and communication on other platforms (e.g., "in the email"). If found, it first checks if evidence matching that description already exists in the case. If not, it adds a "Missing Evidence" tag, creates a detailed note, and uses the `Tasks` API to generate a reminder for the user.
    *   **Code:**
        ```javascript
        const mentionedItems = {
            "document": /\b(the contract|agreement|lease|receipt|invoice)\b/ig,
            "email conversation": /\b(in the email|i emailed you)\b/ig,
            "WhatsApp chat": /\b(on whatsapp)\b/ig
        };

        for (const [itemType, regex] of Object.entries(mentionedItems)) {
            const matches = evidence.text.match(regex);
            if (matches) {
                for (const match of matches) {
                    // Check if evidence with this title/keyword already exists
                    const hasEvidence = case.evidence.some(e => e.metadata.title && e.metadata.title.toLowerCase().includes(match.toLowerCase()));

                    if (!hasEvidence) {
                        addTag("Evidence Gap");
                        const note = `Evidence mentions a '${match}' (${itemType}) that may be missing from the case file.`;
                        createNote(note);
                        Tasks.create(`Request missing ${itemType}: '${match}' mentioned in evidence from ${evidence.metadata.date}`);
                    }
                }
            }
        }
        ```

## 3. Summary of Changes

If this proposal is approved, the following changes will be made:

*   **Scripts to be Removed (9):**
    *   `1. Profanity Tagger`
    *   `3. Contact Information Extractor`
    *   `5. Gaslighting Detector`
    *   `6. Threat Severity Assessor`
    *   `14. Document Request Generator`
    *   `16. "If This, Then That" Coercion Analyzer`
    *   `19. Evidence Gap Suggester`
    *   `27. Automatic Redaction Suggester`
    *   `67. Proactive Evidence Request Task`
*   **Scripts to be Added (3):**
    *   `Comprehensive PII Analyzer`
    *   `Advanced Communication Hazard Analyzer`
    *   `Unified Evidence Gap Detector`

This would result in a net reduction of **6 scripts**, while significantly increasing the power and utility of the remaining examples.

## 4. Conclusion

This consolidation effort will make the script examples library more concise and impactful. I believe these changes will provide better, more realistic examples for users.

I am ready to implement these changes upon your approval. Please let me know your feedback.
