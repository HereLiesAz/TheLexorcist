package com.hereliesaz.lexorcist.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

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
