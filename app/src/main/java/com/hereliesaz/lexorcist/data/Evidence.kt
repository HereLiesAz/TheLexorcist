package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Evidence(
    val id: Int = 0,
    val caseId: Long,
    val spreadsheetId: String,
    val type: String,
    val content: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val allegationId: Int?,
    val category: String,
    val tags: List<String>,
    val commentary: String? = null,
    val linkedEvidenceIds: List<Int> = emptyList(),
    val parentVideoId: String? = null,
    val entities: Map<String, List<String>> = emptyMap(),
    val isSelected: Boolean = false,
    val transcriptEdits: List<com.hereliesaz.lexorcist.model.TranscriptEdit> = emptyList()
) : Parcelable
