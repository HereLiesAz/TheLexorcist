package com.hereliesaz.lexorcist.model

data class TaggedEvidence(
    val id: String,
    val tags: List<String>,
    val content: String,
    val relevance: Int = 0,
    val notes: String = ""
)
