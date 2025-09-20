package com.hereliesaz.lexorcist.model

/**
 * Data class to hold the structured results from a script execution.
 * This allows scripts to return more than just tags, providing richer output
 * for both modern and legacy scripts.
 */
data class ScriptResult(
    val tags: MutableList<String> = mutableListOf(),
    var severity: String? = null,
    var note: String? = null,
    var linkedAllegation: String? = null
)
