package com.hereliesaz.lexorcist.model

data class ScriptedMenuItem(
    val id: String,
    var text: String, // Changed to var to allow dynamic label changes
    var isVisible: Boolean = true, // Changed to var for dynamic visibility
    val onClickAction: String? = null // Replaced onClick lambda with a scriptable string action
)
