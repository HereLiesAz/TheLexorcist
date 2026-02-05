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

-   **Layout:** Displays a list of cases. Includes a Search Bar and a Floating Action Button (FAB) to create new cases.
-   **Empty State:** If no cases exist (and no search query is active), a call-to-action button to create a new case is displayed.
-   **Interaction:**
    *   **Tap:** Selects a case, loading its data (Evidence, Allegations, etc.) and navigating to the case detail view.
    *   **Long-Press:** Activates a context mode for the selected case, revealing "Delete" and "Archive" options.

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

-   **Layout:** The screen layout adapts based on screen size, typically offering a multi-pane view for Allegations and Evidence.
-   **Action Buttons:** The screen features three primary action buttons at the bottom:
    *   **Organize:** Initiates the cleanup scan (duplicates, image series).
    *   **Generate:** Opens a dialog to generate legal documents based on templates.
    *   **Finalize:** Opens a dialog to package selected case files into an archive (`.zip` or `.lex`) for export.
-   **Functionality:** Users can review evidence, toggle selection, edit metadata, and delete items.

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
-   **Security:** The `WebView` is configured to disable JavaScript and file access for security.

---

## Timeline Screen (`TimelineScreen.kt`)

-   **Layout:** Displays evidence items in a chronological vertical timeline using the `JetLime` library.
-   **Sorting:** Items are sorted by their `documentDate`.
-   **Empty State:** If there is no evidence to display, the screen shows a placeholder timeline event to demonstrate functionality.
-   **Performance:** The screen uses stable keys and `remember` blocks to optimize list rendering and avoid unnecessary recompositions.
