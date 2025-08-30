package com.hereliesaz.lexorcist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a legal case within the Lexorcist application.
 *
 * This data class holds the essential information for a case, including its name,
 * associated Google Sheets spreadsheet ID, and IDs for various generated documents.
 *
 * @property id The unique identifier for the case.
 * @property name The name of the case.
 * @property spreadsheetId The ID of the Google Sheets spreadsheet containing the case data.
 * @property generatedPdfId The ID of the generated PDF document for the case.
import androidx.room.Entity
import androidx.room.PrimaryKey

 * @property sourceHtmlSnapshotId The ID of the HTML snapshot used as the source for the generated PDF.
 * @property originalMasterHtmlTemplateId The ID of the original master HTML template used for the case.
 */
@Entity(tableName = "cases")
@Entity(tableName = "case")
data class Case(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    var spreadsheetId: String?,
    var scriptId: String? = null,
    val generatedPdfId: String? = null,
    val sourceHtmlSnapshotId: String? = null,
    val originalMasterHtmlTemplateId: String? = null,
    val folderId: String?, // Often the same as 'id' if using Drive folder IDs as PK
    val plaintiffs: String?,
    val defendants: String?,
    val court: String?,
    val isArchived: Boolean = false,
    val lastModifiedTime: Long? = null // Optional: for sorting or tracking
    // Add any other fields relevant to a "Case"
    // Ensure all types are Room-compatible or have TypeConverters
)
