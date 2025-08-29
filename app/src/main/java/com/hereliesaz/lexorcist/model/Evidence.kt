package com.hereliesaz.lexorcist.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a single piece of evidence, typically extracted from a document.
 *
 * This data class is a Room entity that stores evidence in a local database.
 *
 * @property id The unique identifier for the evidence.
 * @property amount The monetary value, if any. This may be deprecated in a future version.
 * @property timestamp The timestamp when the evidence was recorded or created.
 * @property sourceDocument The name or identifier of the source document.
 * @property documentDate The date of the source document.
 * @property allegationId The ID of the allegation this evidence is related to.
 */
@Entity(tableName = "evidence")
data class Evidence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var caseId: Long = 0, // Added this line
    val amount: Double?,
    val timestamp: Date,
    val sourceDocument: String,
    val documentDate: Date,
    val allegationId: String,
    val category: String? = null
)
