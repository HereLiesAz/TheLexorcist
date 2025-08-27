package com.hereliesaz.lexorcist.db

// Room annotations removed
data class FinancialEntry(
    val id: Int = 0, // No longer an auto-generated PrimaryKey by Room.
    val caseId: Int, // Represents the link to a Case.
    val allegationId: Int? = null, // Represents the link to an Allegation.
    val amount: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val category: String = "Uncategorized"
)
