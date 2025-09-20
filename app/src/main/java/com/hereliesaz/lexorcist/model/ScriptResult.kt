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
    var linkedAllegation: String? = null
)
