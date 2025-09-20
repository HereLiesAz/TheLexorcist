# Addons Curation Proposal

## 1. Introduction

Based on the new context that these scripts will serve as user-facing "addons," this proposal re-imagines the script library not as developer documentation, but as a curated feature for end-users.

The goal is to organize the scripts into a logical, user-friendly collection that is easy to browse and provides immediate, understandable value.

## 2. Proposal 1: Re-organize into User-Friendly Categories

The current "Level" system is developer-focused. I propose replacing it with the following purpose-driven categories, which would be much clearer on an "Extras/addons" screen:

*   **Basic Tagging:** For simple, one-click tagging operations.
*   **Communication Analysis:** For scripts that analyze the tone and content of messages.
*   **Evidence Integrity:** For scripts that find contradictions, gaps, or anomalies.
*   **Financial Analysis:** For all money-related scripts.
*   **Workflow Automation:** For scripts that perform multi-step actions.
*   **Dynamic UI & Advanced Examples:** A section for the most complex or meta examples.

## 3. Proposal 2: Refine Names and Descriptions

Many scripts need clearer, more benefit-oriented names and descriptions. For example:

*   **From:** `Alibi Corroborator/Breaker`
    **To:** `Check for Alibi Conflicts`
    **New Description:** "Scans all evidence to see if a stated alibi conflicts with information from another piece of evidence."

*   **From:** `Third-Party Witness Identifier`
    **To:** `Find Potential Witnesses`
    **New Description:** "Scans evidence for mentions of other people (e.g., 'I told [Name]...') who could be potential witnesses."

*   **From:** `Communication Blackout Detector`
    **To:** `Identify Gaps in Communication`
    **New Description:** "Checks the dates on messages from the same source and flags any unusually long gaps in communication (e.g., more than 14 days)."

This renaming and re-describing process will be applied to most scripts to enhance clarity for the end-user.

## 4. Proposal 3: Remove Final "Filler" Script

While most simple scripts have value as standalone addons, one script remains a poor example and adds clutter.

*   **Script to be Removed:**
    *   `8. Contradiction Finder` (the original, simple promise vs. denial script).
*   **Reasoning:** This script's logic is too niche and specific to be a useful general-purpose addon. The concept of "finding contradictions" is much better served by the more robust `Check for Alibi Conflicts` script and the advanced AI-powered examples. It is the last true "filler" script.

## 5. The Curated Addon Library (Proposed)

If this proposal is approved, the `SCRIPT_EXAMPLES.md` file will be rewritten to reflect this curated structure. It will be organized by the new categories, with refined names and descriptions, and the one filler script removed. The total number of scripts will be high, but they will be well-organized and user-focused.

I am ready to implement this final curation upon your approval. This will complete the transformation of the script examples from a developer document to a user-ready feature set.
