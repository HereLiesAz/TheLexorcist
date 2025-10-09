# Application Screens

This document provides detailed descriptions and requirements for the various screens within The Lexorcist application.

---

## Allegations Screen (`AllegationsScreen.kt`)

-   **Layout:** The screen layout must follow this specific order:
    1.  The title "Allegations".
    2.  A list of the selected allegations that are currently applied to the case.
    3.  A search box for filtering allegations.
    4.  A "Request" button next to a "Sort-by" option on the same row.
    5.  The complete list of all available allegations to select from.
-   **Interaction:** Long-pressing an allegation in the list must bring up an "Allegation Details" dialog.

---

## Cases Screen (`CasesScreen.kt`)

-   **Interaction:** When a user taps a case in the list, it must be visually highlighted (e.g., by changing its background color) to indicate selection.
-   **Functionality:** Selecting a case should load all its associated data, including evidence, parsed text, selected allegations, templates, and scripts, restoring all screens to their most recent state for that case.

---

## Exhibits Screen (`ExhibitsScreen.kt`)

-   **Layout:** This screen must have a tabbed layout with three tabs: 'View', 'Organize', and 'Assign'.
-   **'Organize' Tab:**
    -   This tab (formerly 'Clean Up') must contain a button with the text 'Scan'.
    -   When the 'Scan' button is pressed, it must be hidden and replaced with an `AzLoad` animation and a `LinearProgressIndicator` that fills the screen's width. These loading indicators must remain visible until the cleanup suggestion scan is complete.
-   **'Assign' Tab:**
    *   **Layout:** The left column displays pertinent exhibit types derived from the case's allegations. The right column displays unassigned evidence.
    *   **Drag-and-Drop:** When an `EvidenceDisplayItem` is dragged from the right column, the drag preview must originate from the exact position of the item being dragged.
-   **Interaction:** Clicking on an Exhibit displays its description, a list of pertinent evidence types, its completeness and influence factor, and a list of its contents. Tapping a piece of evidence here allows the user to remove it, tag it, or perform other scripted actions.
-   **Data:** The list of exhibits is pulled from `exhibits.csv`. Exhibits are displayed under the 'View' tab if their `applicable_allegation_ids` match any of the currently selected allegations for the case.

---

## Review Screen (`ReviewScreen.kt`)

-   **Layout:** The screen should feature three primary action buttons: "Automatic Cleanup", "Paperwork", and "Finalize". These must be `AzButton` components.
-   **Functionality:**
    *   **Automatic Cleanup:** Initiates a process to clean up evidence, such as merging sequential screenshots of a conversation or identifying duplicate files.
    *   **Paperwork:** Generates all necessary legal documents for the case's exhibits using the user's selected templates.
    *   **Finalize:** Opens a dialog allowing the user to select which case files to include in a final archive, which can be exported as a `.zip` or a `.lex` file (a renamed `.zip` for easy import back into the app).

---

## Script Builder Screen (`ScriptBuilderScreen.kt`)

-   **Layout:** The screen has three tabs:
    1.  An editor for writing or modifying a script.
    2.  A description tab, which is programmatically generated but can also be edited by the user.
    3.  A list of all scripts that will be applied on the next screen load.
-   **Functionality:**
    *   **Sharing:** A "Share" button opens a dialog for the user to enter their name and email to share the script.
    *   **Editing Shared Scripts:** An "Edit" button must be provided for shared scripts, which emails the script to the original creator for updates.

---

## Templates Screen (`TemplatesScreen.kt`)

-   **Interaction:** Tapping a `TemplateItem` card must open a full-screen `TemplatePreviewDialog` that displays the template's content in a `WebView`.

---

## Timeline Screen (`TimelineScreen.kt`)

-   **Empty State:** If there is no evidence to display, the screen must still show the "Timeline" title and a placeholder extended event to demonstrate to the user what the screen is for and what to expect.