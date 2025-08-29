package com.hereliesaz.lexorcist.db

// Room annotations removed
data class Case(
    val id: Int = 0, // No longer an auto-generated PrimaryKey by Room. Consider its new role or removal later.
    val name: String,
    val spreadsheetId: String,
    val generatedPdfId: String? = null, // Renamed from masterTemplateId and made nullable
    val sourceHtmlSnapshotId: String? = null, // New field for the processed HTML snapshot ID
    val originalMasterHtmlTemplateId: String? = null // New field for the original HTML template ID used
)
