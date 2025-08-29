package com.hereliesaz.lexorcist.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a single financial entry, typically extracted from a receipt or other financial document.
 *
 * This data class is a Room entity that stores financial entries in a local database.
 *
 * @property id The unique identifier for the financial entry.
 * @property amount The monetary value of the entry.
 * @property timestamp The timestamp when the entry was recorded or created.
 * @property sourceDocument The name or identifier of the source document.
 * @property documentDate The date of the source document.
 * @property allegationId The ID of the allegation this entry is related to.
 */
@Entity(tableName = "financial_entries")
data class FinancialEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val timestamp: Date,
    val sourceDocument: String,
    val documentDate: Date,
    val allegationId: String
)
