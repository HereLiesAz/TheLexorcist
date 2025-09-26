package com.hereliesaz.lexorcist.data

data class Script(
    val name: String,
    val author: String,
    val description: String,
    val content: String,
    val id: String = ""
)
