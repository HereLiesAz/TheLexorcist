package com.hereliesaz.lexorcist.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_entries")
data class FinancialEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val caseId: Int,
    val allegationId: Int? = null, // Foreign key to Allegation
    val amount: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val category: String = "Uncategorized"
)