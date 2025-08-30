package com.hereliesaz.lexorcist.data

import androidx.room.ColumnInfo
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
 * @property sourceHtmlSnapshotId The ID of the HTML snapshot used as the source for the generated PDF.
 * @property originalMasterHtmlTemplateId The ID of the original master HTML template used for the case.
 */
@Entity(tableName = "cases")
data class Case(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "spreadsheetId")
    val spreadsheetId: String,
    @ColumnInfo(name = "generatedPdfId")
    val generatedPdfId: String? = null,
    @ColumnInfo(name = "sourceHtmlSnapshotId")
    val sourceHtmlSnapshotId: String? = null,
    @ColumnInfo(name = "originalMasterHtmlTemplateId")
    val originalMasterHtmlTemplateId: String? = null,
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false
)
