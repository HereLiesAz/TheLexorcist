package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * Represents a piece of evidence.
 *
 * Marked as [Immutable] to optimize Jetpack Compose performance.
 * The [List] and [Map] properties are normally considered unstable by the Compose compiler,
 * which would cause unnecessary recompositions of UI components taking [Evidence] as a parameter.
 * By annotating this class as [Immutable], we promise that the data will not be mutated after creation,
 * allowing Compose to skip recomposition when the object reference hasn't changed.
 */
@Immutable
@Parcelize
data class Evidence(
    val id: Int = 0,
    val caseId: Long,
    val spreadsheetId: String,
    val type: String,
    val content: String,
    val formattedContent: String?,
    val mediaUri: String?,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val allegationId: String?,
    val allegationElementName: String?,
    val category: String,
    val tags: List<String>,
    val commentary: String? = null,
    val linkedEvidenceIds: List<Int> = emptyList(),
    val parentVideoId: String? = null,
    val entities: Map<String, List<String>> = emptyMap(),
    val isSelected: Boolean = false,
    val transcriptEdits: List<com.hereliesaz.lexorcist.model.TranscriptEdit> = emptyList(),
    val audioTranscript: String? = null,
    val videoOcrText: String? = null,
    val duration: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
    val fileSize: Long = 0L,
    val fileHash: String? = null,
    val isDuplicate: Boolean = false
) : Parcelable
