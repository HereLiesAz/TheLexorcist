package com.hereliesaz.lexorcist.model

data class Evidence(
    val content: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val tags: List<String> = emptyList(),
    var id: Int = 0,
    var caseId: Int = 0,
    var allegationId: Int? = null
)
