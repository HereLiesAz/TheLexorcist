package com.hereliesaz.lexorcist.model

/**
 * Data class to hold the structured results from a script execution.
 * This allows scripts to return more than just tags, providing richer output
 * for both modern and legacy scripts. This object is populated by the script
 * during its execution and returned to the calling service.
 */
data class ScriptResult(
    /** A list of tags to be added to the evidence. Can be populated by the legacy `addTag()` function or modern `tags.push()` method. */
    val tags: MutableList<String> = mutableListOf(),
    /** An optional severity level (e.g., "Low", "Medium", "High") set by the legacy `setSeverity()` function. */
    var severity: String? = null,
    /** An optional note to be added to the evidence, set by the legacy `createNote()` function. */
    var note: String? = null,
    /** An optional allegation to link the evidence to, set by the legacy `linkToAllegation()` function. */
    var linkedAllegation: String? = null,
    /**
     * UI components the script asked for via `lex.ui.addOrUpdate(...)`, keyed by id.
     *
     * `lex.ui` is documented in SCRIPT_EXAMPLES.md and called by two of the
     * scripts seeded into every install from `assets/default_scripts.csv`, but
     * ScriptRunner never registered the namespace, so those scripts threw
     * `TypeError: Cannot read property "addOrUpdate" from undefined` on every
     * run. The error surfaced as a Result.Error that the caller discarded.
     */
    val uiComponents: MutableMap<String, UiComponentModel> = mutableMapOf(),
    /**
     * Ids the script asked to remove via `lex.ui.remove(...)`, and whether it
     * called `lex.ui.clearAll()`.
     */
    val removedUiComponentIds: MutableSet<String> = mutableSetOf(),
    var clearAllUiComponents: Boolean = false,
)
