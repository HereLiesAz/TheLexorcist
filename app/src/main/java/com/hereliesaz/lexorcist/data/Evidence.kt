package com.hereliesaz.lexorcist.data

import android.net.Uri

data class Evidence(
    val id: String,
    val caseId: Long,
    val spreadsheetId: String,
    val type: String,
    val content: String,
    val timestamp: Long,
    val sourceDocument: String?,
    val documentDate: Long?,
    val allegationId: String?,
    val allegationElementName: String? = null,
    val category: String = "",
    val tags: List<String> = emptyList(),
    var commentary: String? = null,
    val linkedEvidenceIds: List<Int> = emptyList(),
    val parentVideoId: String? = null,
    val entities: Map<String, List<String>> = emptyMap(),
    var isSelected: Boolean = false,
    var formattedContent: String? = null,
    var mediaUri: String? = null
)
