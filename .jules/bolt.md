# BOLT'S JOURNAL - CRITICAL LEARNINGS ONLY

## 2024-10-23 - Double Filtering Anti-pattern
**Learning:** I discovered a redundant filtering pattern where the `CasesScreen` was re-filtering the list of cases based on the search query, even though the `CaseViewModel` was already performing this filtering on the `cases` StateFlow. This caused unnecessary work on the UI thread during composition.
**Action:** When consuming `StateFlow`s from ViewModels, always check if the data is already pre-processed (filtered/sorted). Trust the ViewModel to be the single source of truth and avoid duplicating logic in the UI layer. Only apply UI-specific transformations (like animation state or transient local filters) in the Composable.
