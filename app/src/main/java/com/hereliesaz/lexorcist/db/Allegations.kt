package com.hereliesaz.lexorcist.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_entries",
    foreignKeys = [
        ForeignKey(
            entity = Case::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Allegation::class,
            parentColumns = ["id"],
            childColumns = ["allegationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FinancialEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val caseId: Int,
    val allegationId: Int? = null,
    val amount: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val category: String = "Uncategorized"
)