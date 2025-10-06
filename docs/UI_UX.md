# UI/UX Guidelines

This document contains strict rules and guidelines for the user interface and user experience of The Lexorcist application.

---

## General Rules

1.  **Component Alignment:** Except for the `AzNavRail` component itself, all UI components on every screen (e.g., buttons, text fields, cards) must be **right-aligned**. This rule does not apply to the text content *within* those components.
2.  **Button Components:** All buttons must be one of the following:
    *   An `AzButton`, `AzToggle`, or `AzCycler` from the `com.hereliesaz.AzNavRail` library.
    *   A text-only button with no background, stroke, or other decorations.
    *   If any other type of button, toggle, or FAB is found, it **must be replaced**.
3.  **Data Persistence:** Any selection made by the user (e.g., selecting a case, an allegation, a script, a template) must be immediately "enabled" and persist. The state must be saved and restored automatically. Deselection must also persist in the same manner.
4.  **Loading Animations:** All instances where data is being loaded or a background task is running (e.g., fetching cases, loading evidence, synchronizing with the cloud) must be clearly indicated to the user with a loading animation.

---

## Specific Component Behavior

1.  **AzNavRail Library:** When working with the `AzNavRail` library, you must **never guess** how it works. Read the official documentation and follow the instructions precisely.
    *   **Documentation:** [https://github.com/HereLiesAz/AzNavRail](https://github.com/HereLiesAz/AzNavRail)
2.  **Saving and Syncing:**
    *   All changes to a case must be saved immediately and automatically.
    *   The case folder must be synchronized with the selected cloud service (e.g., Google Drive) frequently.
    *   Synchronization **must always** be attempted when the application is closed. The app needs to be explicit about its closing process to ensure this happens.
3.  **Drag-and-Drop Preview:** On the 'Assign' tab of the Exhibits screen, when a user drags an `EvidenceDisplayItem`, the drag preview (the 'ghost' image) **must** originate from the exact position of the item being dragged, not from the top of the screen. This is a critical implementation detail that must not be altered.
4.  **Template Preview:** On the Templates screen, tapping a `TemplateItem` card **must** open a full-screen `TemplatePreviewDialog` that displays the template's content in a `WebView`. This functionality is implemented and must be preserved.