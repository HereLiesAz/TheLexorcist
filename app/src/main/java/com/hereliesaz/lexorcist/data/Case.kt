package com.hereliesaz.lexorcist.data

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.Entity

/**
 * Represents a legal case within the Lexorcist application.
 *
 * This data class holds the essential information for a case, including its name,
 * associated Google Sheets spreadsheet ID, and IDs for various generated documents.
 *
 * @property id The unique identifier for the case.
 * @property name The name of the case.
 * @property spreadsheetId The ID of the Google Sheets spreadsheet containing the case data.
 * @property scriptId The ID of the Google Apps Script associated with the case (optional).
 * @property generatedPdfId The ID of the generated PDF document for the case (optional).
 * @property sourceHtmlSnapshotId The ID of the HTML snapshot used as the source for the generated PDF (optional).
 * @property originalMasterHtmlTemplateId The ID of the original master HTML template used for the case (optional).
 * @property folderId The ID of the Google Drive folder for this case (optional).
 * @property plaintiffs The names of the plaintiffs in the case (optional).
 * @property defendants The names of the defendants in the case (optional).
 * @property court The court where the case is filed (optional).
 * @property isArchived Indicates if the case is archived.
 * @property lastModifiedTime The timestamp of when the case was last modified (optional).
 */
@Entity(tableName = "cases")
data class Case(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "spreadsheetId")
    val spreadsheetId: String, // Assuming this is non-nullable based on one of the original definitions
    @ColumnInfo(name = "scriptId")
    var scriptId: String? = null,
    @ColumnInfo(name = "generatedPdfId")
    val generatedPdfId: String? = null,
    @ColumnInfo(name = "sourceHtmlSnapshotId")
    val sourceHtmlSnapshotId: String? = null,
    @ColumnInfo(name = "originalMasterHtmlTemplateId")
    val originalMasterHtmlTemplateId: String? = null,
    @ColumnInfo(name = "folderId")
    val folderId: String? = null,
    @ColumnInfo(name = "plaintiffs")
    val plaintiffs: String? = null,
    @ColumnInfo(name = "defendants")
    val defendants: String? = null,
    @ColumnInfo(name = "court")
    val court: String? = null,
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "lastModifiedTime")
    val lastModifiedTime: Long? = null
)
