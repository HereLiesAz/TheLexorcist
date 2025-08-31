package com.hereliesaz.lexorcist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evidence")
data class Evidence(
    @PrimaryKey(autoGenerate = true) // Assuming DB should generate IDs
    val id: Int = 0,
    val caseId: Long, // Standardized to Long
    val type: String = "", // Added type field
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceDocument: String = "",
    val documentDate: Long = System.currentTimeMillis(),
    val allegationId: Int? = null,
    val category: String = "",
    val commentary: String = "",
    val tags: List<String> = emptyList(),
    val linkedEvidenceIds: List<Int> = emptyList(),
    val parentVideoId: String? = null,
    val entities: Map<String, List<String>> = emptyMap() // From OcrViewModel usage
)
