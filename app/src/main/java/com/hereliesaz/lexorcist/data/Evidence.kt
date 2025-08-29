package com.hereliesaz.lexorcist.data

// Room annotations removed
data class Evidence(
    val id: Int = 0, // No longer an auto-generated PrimaryKey by Room.
    val caseId: Int, // Represents the link to a Case.
    val allegationId: Int? = null, // Represents the link to an Allegation.
    val content: String,
    val timestamp: Long,
    val sourceDocument: String,
    val documentDate: Long,
    val tags: List<String> = emptyList()
)
