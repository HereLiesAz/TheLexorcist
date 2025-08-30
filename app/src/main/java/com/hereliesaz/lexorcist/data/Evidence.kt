package com.hereliesaz.lexorcist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single piece of evidence in a legal case.
 *
 * This data class stores the details of an evidence item, including its content,
 * source, and associated metadata.
 *
 * @property id The unique identifier for the evidence.
 * @property caseId The ID of the case this evidence belongs to.
 * @property allegationId The ID of the allegation this evidence is related to, if any.
 * @property content The textual content of the evidence.
 * @property timestamp The timestamp when the evidence was recorded or created.
 * @property sourceDocument The name or identifier of the source document for the evidence.
 * @property documentDate The date of the source document.
 * @property tags A list of tags associated with the evidence for categorization and filtering.
 */

@Entity(tableName = "evidence")
data class Evidence(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val caseId: Int = 0,
    val allegationId: Int? = null,
    val content: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val tags: List<String> = emptyList(),
    val category: String = "",
    val parentVideoId: String? = null
)
