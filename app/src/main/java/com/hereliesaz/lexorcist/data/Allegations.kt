package com.hereliesaz.lexorcist.data

// Room annotations removed
data class Allegation(
    val id: Int = 0, // No longer an auto-generated PrimaryKey by Room. Consider its new role.
    val caseId: Int, // No longer a Foreign Key managed by Room. Represents the link to a Case.
    val text: String
)
