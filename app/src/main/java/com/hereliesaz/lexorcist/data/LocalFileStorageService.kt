package com.hereliesaz.lexorcist.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named // Added this import
import javax.inject.Singleton

@Singleton
class LocalFileStorageService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
    private val settingsManager: SettingsManager,
    private val syncManager: SyncManager,
    @param:Named("googleDrive") private val googleDriveProvider: CloudStorageProvider,
    @param:Named("dropbox") private val dropboxProvider: CloudStorageProvider,
    @param:Named("oneDrive") private val oneDriveProvider: CloudStorageProvider
) : StorageService {

    private val storageDir: File by lazy {
        val customLocation = settingsManager.getStorageLocation()
        val dir = if (customLocation != null) {
            File(Uri.parse(customLocation).path!!)
        } else {
            context.getExternalFilesDir(null) ?: context.filesDir
        }
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val spreadsheetFile: File by lazy { File(storageDir, "lexorcist_data.xlsx") }

    companion object {
        private const val CASES_SHEET_NAME = "Cases"
        private const val EVIDENCE_SHEET_NAME = "Evidence"
        private const val ALLEGATIONS_SHEET_NAME = "Allegations"
        private const val TRANSCRIPT_EDITS_SHEET_NAME = "TranscriptEdits"

        private val CASES_HEADER = listOf("ID", "Name", "Plaintiffs", "Defendants", "Court", "FolderID", "LastModified", "IsArchived")
        private val EVIDENCE_HEADER = listOf("EvidenceID", "CaseID", "Type", "Content", "FormattedContent", "MediaUri", "Timestamp", "SourceDocument", "DocumentDate", "AllegationID", "Category", "Tags", "Commentary", "LinkedEvidenceIDs", "ParentVideoID", "Entities")
        private val ALLEGATIONS_HEADER = listOf("AllegationID", "CaseID", "Text")
        private val TRANSCRIPT_EDITS_HEADER = listOf("EditID", "EvidenceID", "Timestamp", "Reason", "NewContent")
    }

    private suspend fun <T> execute(block: (XSSFWorkbook) -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            val workbook = if (spreadsheetFile.exists() && spreadsheetFile.length() > 0) {
                FileInputStream(spreadsheetFile).use { XSSFWorkbook(it) }
            } else {
                XSSFWorkbook()
            }
            val result = block(workbook)
            FileOutputStream(spreadsheetFile).use { workbook.write(it) }
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun findRowById(sheet: XSSFSheet, id: String, idColumn: Int): Row? {
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            if (row.getCell(idColumn)?.stringCellValue == id) {
                return row
            }
        }
        return null
    }

    private fun findRowById(sheet: XSSFSheet, id: Int, idColumn: Int): Row? {
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            // Use safe numeric cell value retrieval
            if (getIntCellValueSafe(row.getCell(idColumn)) == id) {
                return row
            }
        }
        return null
    }

    // Helper functions to safely get cell values
    private fun getNumericCellValueSafe(cell: Cell?): Double? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            CellType.FORMULA -> {
                try {
                    if (cell.cachedFormulaResultType == CellType.NUMERIC) {
                        cell.numericCellValue
                    } else if (cell.cachedFormulaResultType == CellType.STRING) {
                        cell.stringCellValue.toDoubleOrNull()
                    } else {
                        null
                    }
                } catch (e: IllegalStateException) {
                    null // Error during formula evaluation or unsupported cached type
                }
            }
            else -> null
        }
    }

    private fun getIntCellValueSafe(cell: Cell?): Int? {
        return getNumericCellValueSafe(cell)?.toInt()
    }

    private fun getLongCellValueSafe(cell: Cell?): Long? {
        return getNumericCellValueSafe(cell)?.toLong()
    }

    private fun getBooleanCellValueSafe(cell: Cell?): Boolean? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.STRING -> {
                val sVal = cell.stringCellValue.trim().lowercase()
                when (sVal) {
                    "true" -> true
                    "false" -> false
                    else -> null
                }
            }
            CellType.NUMERIC -> cell.numericCellValue != 0.0
            else -> null
        }
    }

    suspend fun moveFilesToNewLocation(oldLocation: String, newLocation: String) {
        withContext(Dispatchers.IO) {
            val oldDir = File(Uri.parse(oldLocation).path!!)
            val newDir = File(Uri.parse(newLocation).path!!)
            if (oldDir.exists() && oldDir.isDirectory) {
                oldDir.copyRecursively(newDir, overwrite = true)
                oldDir.deleteRecursively()
            }
        }
    }

    // --- Case Implementations ---

    override suspend fun getAllCases(): Result<List<Case>> = execute { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: return@execute emptyList()
        (1..sheet.lastRowNum).mapNotNull { i ->
            val row = sheet.getRow(i) ?: return@mapNotNull null
            Case(
                id = i,
                spreadsheetId = row.getCell(0)?.stringCellValue ?: "",
                name = row.getCell(1)?.stringCellValue ?: "",
                plaintiffs = row.getCell(2)?.stringCellValue ?: "",
                defendants = row.getCell(3)?.stringCellValue ?: "",
                court = row.getCell(4)?.stringCellValue ?: "",
                folderId = row.getCell(5)?.stringCellValue,
                lastModifiedTime = getLongCellValueSafe(row.getCell(6)) ?: 0L,
                isArchived = getBooleanCellValueSafe(row.getCell(7)) ?: false
            )
        }
    }

    override suspend fun createCase(case: Case): Result<Case> = execute { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: workbook.createSheet(CASES_SHEET_NAME).also {
            it.createRow(0).apply { CASES_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val newId = UUID.randomUUID().toString()
        val newCase = case.copy(spreadsheetId = newId, lastModifiedTime = System.currentTimeMillis())
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue(newCase.spreadsheetId)
            createCell(1).setCellValue(newCase.name)
            createCell(2).setCellValue(newCase.plaintiffs)
            createCell(3).setCellValue(newCase.defendants)
            createCell(4).setCellValue(newCase.court)
            createCell(5).setCellValue(newCase.folderId)
            createCell(6).setCellValue(newCase.lastModifiedTime!!.toDouble())
            createCell(7).setCellValue(newCase.isArchived)
        }
        newCase
    }

    override suspend fun updateCase(case: Case): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: throw IOException("Cases sheet not found.")
        val row = findRowById(sheet, case.spreadsheetId, 0) ?: throw IOException("Case with id ${case.spreadsheetId} not found.")
        row.getCell(1)?.setCellValue(case.name)
        row.getCell(2)?.setCellValue(case.plaintiffs)
        row.getCell(3)?.setCellValue(case.defendants)
        row.getCell(4)?.setCellValue(case.court)
        row.getCell(5)?.setCellValue(case.folderId)
        row.getCell(6)?.setCellValue(System.currentTimeMillis().toDouble())
        row.getCell(7)?.setCellValue(case.isArchived)
    }

    override suspend fun deleteCase(case: Case): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: throw IOException("Cases sheet not found.")
        val row = findRowById(sheet, case.spreadsheetId, 0) ?: throw IOException("Case with id ${case.spreadsheetId} not found.")
        sheet.removeRow(row)
        if (row.rowNum < sheet.lastRowNum) {
            sheet.shiftRows(row.rowNum + 1, sheet.lastRowNum, -1)
        }
    }

    // --- Evidence Implementations ---

    override suspend fun getEvidenceForCase(caseSpreadsheetId: String): Result<List<Evidence>> = execute { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: return@execute emptyList()
        val editsSheet = workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME)

        val allEdits = editsSheet?.let {
            (1..it.lastRowNum).mapNotNull { j ->
                val editRow = it.getRow(j) ?: return@mapNotNull null
                val evidenceId = getIntCellValueSafe(editRow.getCell(1))
                if (evidenceId != null) {
                    evidenceId to com.hereliesaz.lexorcist.model.TranscriptEdit(
                        timestamp = getLongCellValueSafe(editRow.getCell(2)) ?: 0L,
                        reason = editRow.getCell(3)?.stringCellValue ?: "",
                        content = editRow.getCell(4)?.stringCellValue ?: ""
                    )
                } else {
                    null
                }
            }
        }?.groupBy({ it.first }, { it.second }) ?: emptyMap()

        (1..sheet.lastRowNum).mapNotNull { i ->
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(1)?.stringCellValue != caseSpreadsheetId) return@mapNotNull null

            val idCell = row.getCell(0)
            val evidenceId = getIntCellValueSafe(idCell) ?: 0

            val timestampCell = row.getCell(6) // Corrected index for Evidence Timestamp
            val timestamp = getLongCellValueSafe(timestampCell) ?: 0L

            val documentDateCell = row.getCell(8) // Corrected index for Evidence DocumentDate
            val documentDate = getLongCellValueSafe(documentDateCell) ?: 0L

            val allegationIdCell = row.getCell(9) // Corrected index for Evidence AllegationID
            val allegationId = getIntCellValueSafe(allegationIdCell)

            val tagsCell = row.getCell(11) // Corrected index for Evidence Tags
            val tags = (tagsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }

            val linkedIdsCell = row.getCell(13) // Corrected index for Evidence LinkedEvidenceIDs
            val linkedIds = (linkedIdsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }

            val entitiesCell = row.getCell(15) // Corrected index for Evidence Entities
            val entities: Map<String, List<String>> = gson.fromJson(
                entitiesCell?.stringCellValue ?: "{}",
                object : TypeToken<Map<String, List<String>>>() {}.type
            )

            val transcriptEdits = allEdits[evidenceId] ?: emptyList()

            Evidence(
                id = evidenceId,
                caseId = caseSpreadsheetId.hashCode().toLong(), // This seems to be a placeholder, consider if it should be from sheet
                spreadsheetId = caseSpreadsheetId,
                type = row.getCell(2)?.stringCellValue ?: "",
                content = row.getCell(3)?.stringCellValue ?: "",
                formattedContent = row.getCell(4)?.stringCellValue,
                mediaUri = row.getCell(5)?.stringCellValue,
                timestamp = timestamp,
                sourceDocument = row.getCell(7)?.stringCellValue ?: "", // Corrected index for Evidence SourceDocument
                documentDate = documentDate,
                allegationId = allegationId,
                category = row.getCell(10)?.stringCellValue ?: "", // Corrected index for Evidence Category
                tags = tags,
                commentary = row.getCell(12)?.stringCellValue, // Corrected index for Evidence Commentary
                linkedEvidenceIds = linkedIds,
                parentVideoId = row.getCell(14)?.stringCellValue, // Corrected index for Evidence ParentVideoID
                entities = entities,
                transcriptEdits = transcriptEdits
            )
        }
    }

    override suspend fun addEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Evidence> = execute { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: workbook.createSheet(EVIDENCE_SHEET_NAME).also {
            it.createRow(0).apply { EVIDENCE_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val newEvidence = evidence.copy(id = sheet.physicalNumberOfRows)
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue(newEvidence.id.toDouble())
            createCell(1).setCellValue(caseSpreadsheetId)
            createCell(2).setCellValue(newEvidence.type)
            createCell(3).setCellValue(newEvidence.content)
            createCell(4).setCellValue(newEvidence.formattedContent)
            createCell(5).setCellValue(newEvidence.mediaUri)
            createCell(6).setCellValue(newEvidence.timestamp.toDouble())
            createCell(7).setCellValue(newEvidence.sourceDocument)
            createCell(8).setCellValue(newEvidence.documentDate.toDouble())
            newEvidence.allegationId?.let { createCell(9).setCellValue(it.toDouble()) }
            createCell(10).setCellValue(newEvidence.category)
            createCell(11).setCellValue(newEvidence.tags.joinToString(","))
            createCell(12).setCellValue(newEvidence.commentary ?: "")
            createCell(13).setCellValue(newEvidence.linkedEvidenceIds.joinToString(","))
            createCell(14).setCellValue(newEvidence.parentVideoId ?: "")
            createCell(15).setCellValue(gson.toJson(newEvidence.entities))
        }
        newEvidence
    }

    override suspend fun updateEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val row = findRowById(sheet, evidence.id, 0) ?: throw IOException("Evidence with id ${evidence.id} not found.")
        row.getCell(2)?.setCellValue(evidence.type)
        row.getCell(3)?.setCellValue(evidence.content)
        row.getCell(4)?.setCellValue(evidence.formattedContent)
        row.getCell(5)?.setCellValue(evidence.mediaUri)
        row.getCell(6)?.setCellValue(evidence.timestamp.toDouble())
        row.getCell(7)?.setCellValue(evidence.sourceDocument)
        row.getCell(8)?.setCellValue(evidence.documentDate.toDouble())
        evidence.allegationId?.let { row.getCell(9)?.setCellValue(it.toDouble()) }
        row.getCell(10)?.setCellValue(evidence.category)
        row.getCell(11)?.setCellValue(evidence.tags.joinToString(","))
        row.getCell(12)?.setCellValue(evidence.commentary ?: "")
        row.getCell(13)?.setCellValue(evidence.linkedEvidenceIds.joinToString(","))
        row.getCell(14)?.setCellValue(evidence.parentVideoId ?: "")
        row.getCell(15)?.setCellValue(gson.toJson(evidence.entities))
    }

    override suspend fun deleteEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val row = findRowById(sheet, evidence.id, 0) ?: throw IOException("Evidence with id ${evidence.id} not found.")
        sheet.removeRow(row)
        if (row.rowNum < sheet.lastRowNum) {
            sheet.shiftRows(row.rowNum + 1, sheet.lastRowNum, -1)
        }
    }

    override suspend fun uploadFile(caseSpreadsheetId: String, fileUri: Uri, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val caseDir = File(storageDir, caseSpreadsheetId)
            val rawDir = File(caseDir, "raw").apply { if (!exists()) mkdirs() }
            val destinationFile = File(rawDir, "file_${System.currentTimeMillis()}.${mimeType.substringAfter('/')}")
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.Success(destinationFile.absolutePath)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // --- Allegation Implementations ---

    override suspend fun getAllegationsForCase(caseSpreadsheetId: String): Result<List<Allegation>> = execute { workbook ->
        val sheet = workbook.getSheet(ALLEGATIONS_SHEET_NAME) ?: return@execute emptyList()
        (1..sheet.lastRowNum).mapNotNull { i ->
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(1)?.stringCellValue != caseSpreadsheetId) return@mapNotNull null
            Allegation(
                id = getIntCellValueSafe(row.getCell(0)) ?: 0,
                spreadsheetId = caseSpreadsheetId,
                text = row.getCell(2)?.stringCellValue ?: ""
            )
        }
    }

    override suspend fun addAllegation(caseSpreadsheetId: String, allegation: Allegation): Result<Allegation> = execute { workbook ->
        val sheet = workbook.getSheet(ALLEGATIONS_SHEET_NAME) ?: workbook.createSheet(ALLEGATIONS_SHEET_NAME).also {
            it.createRow(0).apply { ALLEGATIONS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val newAllegation = allegation.copy(id = sheet.physicalNumberOfRows)
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue(newAllegation.id.toDouble())
            createCell(1).setCellValue(caseSpreadsheetId)
            createCell(2).setCellValue(newAllegation.text)
        }
        newAllegation
    }

    // --- Transcript Implementations ---

    override suspend fun updateTranscript(evidence: Evidence, newTranscript: String, reason: String): Result<Unit> = execute { workbook ->
        val evidenceSheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val evidenceRow = findRowById(evidenceSheet, evidence.id, 0) ?: throw IOException("Evidence with id ${evidence.id} not found.")

        evidenceRow.getCell(3)?.setCellValue(newTranscript)

        val editsSheet = workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME) ?: workbook.createSheet(TRANSCRIPT_EDITS_SHEET_NAME).also {
            it.createRow(0).apply { TRANSCRIPT_EDITS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        editsSheet.createRow(editsSheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue((editsSheet.physicalNumberOfRows).toDouble()) // EditID
            createCell(1).setCellValue(evidence.id.toDouble()) // EvidenceID
            createCell(2).setCellValue(System.currentTimeMillis().toDouble()) // Timestamp
            createCell(3).setCellValue(reason) // Reason
            createCell(4).setCellValue(newTranscript) // NewContent
        }
    }

    override suspend fun synchronize(): Result<Unit> {
        val selectedProvider = settingsManager.getSelectedCloudProvider()
        val cloudStorageProvider = when (selectedProvider) {
            "GoogleDrive" -> googleDriveProvider
            "Dropbox" -> dropboxProvider
            "OneDrive" -> oneDriveProvider
            else -> null
        }

        return if (cloudStorageProvider != null) {
            syncManager.synchronize(cloudStorageProvider, this)
        } else {
            Result.Success(Unit) // No provider selected, so nothing to sync
        }
    }
}
