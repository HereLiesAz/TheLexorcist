package com.hereliesaz.lexorcist.model

data class Template(
    val id: String,
    val name: String,
    val description: String,
    val content: String,
    val authorName: String,
    val authorEmail: String,
    val court: String?,
)
