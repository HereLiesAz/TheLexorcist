package com.hereliesaz.lexorcist.data

data class Evidence(
    val id: Int,
    val spreadsheetId: String,
    val content: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val allegationId: Int?,
    val category: String,
    val tags: List<String>,
    val commentary: String? = null,
    val linkedEvidenceIds: List<Int> = emptyList()
)
