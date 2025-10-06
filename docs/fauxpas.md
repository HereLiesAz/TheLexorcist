# Faux Pas: Critical Warnings

This document lists critical warnings and behaviors that must be strictly avoided during development. Ignoring these directives is considered a major failure.

---

## 1. Do Not Guess How `AzNavRail` Works

-   **The Rule:** When it comes to the DSL-style `AzNavRail` library, **NEVER, NEVER, NEVER** guess how it functions.
-   **The Mandate:** You are required to read the official documentation and follow the instructions precisely.
-   **Rationale:** The library has a specific design and implementation. Guessing will lead to incorrect implementation, bugs, and wasted time.
-   **Documentation:** [https://github.com/HereLiesAz/AzNavRail](https://github.com/HereLiesAz/AzNavRail)

---

## 2. Do Not Use `FusedLocationProviderClient`

-   **The Rule:** Do not use `FusedLocationProviderClient` or any other direct location service API to access a user's location history.
-   **The Mandate:** The location history feature **must** be implemented via file import only.
-   **Rationale:** Direct access to a user's real-time or historical location data via Android APIs is not the intended or approved method for this feature. The design requires the user to explicitly provide this data, for example, through a Google Takeout `Records.json` file.
-   **Implementation:** Use the `LocationHistoryParser` for processing imported location history files.

---

## 3. Preserve Critical Implementations

Certain parts of the application have been implemented in a specific way to solve critical UI and data handling problems. These implementations **must not** be altered.

-   **Draggable Item Preview (`DraggableItem`)**:
    -   **Location:** `ExhibitsScreen.kt`
    -   **Functionality:** The drag preview for an `EvidenceDisplayItem` must originate from the exact position of the item being dragged.
    -   **Implementation:** This is achieved using `onGloballyPositioned` to get the item's `positionInWindow()` and adding it to the drag offset. **Do not change this logic.**

-   **Template Preview Dialog (`TemplatesScreen.kt`)**:
    -   **Location:** `TemplatesScreen.kt`
    -   **Functionality:** Tapping a `TemplateItem` card must open a full-screen `TemplatePreviewDialog` that displays the template's content in a `WebView`.
    -   **Implementation:** This feature is already correctly implemented and must be preserved as is.