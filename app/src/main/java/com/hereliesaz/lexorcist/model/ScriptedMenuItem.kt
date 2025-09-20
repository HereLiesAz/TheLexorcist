package com.hereliesaz.lexorcist.model

data class ScriptedMenuItem(
    val id: String,
    val text: String,
    val isVisible: Boolean = true,
    val onClick: () -> Unit
)
