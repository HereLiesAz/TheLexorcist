package com.hereliesaz.lexorcist.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.hereliesaz.lexorcist.service.GoogleApiService

@Singleton
class LocalFileStorageService @Inject constructor(
    @param:ApplicationContext private val context: Context, // Changed here
    private val gson: Gson,
    private val googleApiService: GoogleApiService
) : StorageService {

    private val storageDir: File by lazy {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
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
        private val TRANSCRIPT_EDITS_HEADER = listOf("EditID", "EvidenceID", "Timestamp", "Reason", "OldContent")
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
            if (row.getCell(idColumn)?.numericCellValue?.toInt() == id) {
                return row
            }
        }
        return null
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
                lastModifiedTime = row.getCell(6)?.numericCellValue?.toLong() ?: 0L,
                isArchived = row.getCell(7)?.booleanCellValue ?: false
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
                val evidenceId = editRow.getCell(1)?.numericCellValue?.toInt()
                if (evidenceId != null) {
                    evidenceId to com.hereliesaz.lexorcist.model.TranscriptEdit(
                        timestamp = editRow.getCell(2)?.numericCellValue?.toLong() ?: 0L,
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
            val evidenceId = idCell?.numericCellValue?.toInt() ?: 0

            val timestampCell = row.getCell(4)
            val timestamp = timestampCell?.numericCellValue?.toLong() ?: 0L

            val documentDateCell = row.getCell(6)
            val documentDate = documentDateCell?.numericCellValue?.toLong() ?: 0L

            val allegationIdCell = row.getCell(7)
            val allegationId = allegationIdCell?.numericCellValue?.toInt()

            val tagsCell = row.getCell(9)
            val tags = (tagsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }

            val linkedIdsCell = row.getCell(11)
            val linkedIds = (linkedIdsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }


            val entitiesCell = row.getCell(13)
            // Explicitly define the type for entities
            val entities: Map<String, List<String>> = gson.fromJson(
                entitiesCell?.stringCellValue ?: "{}",
                object : TypeToken<Map<String, List<String>>>() {}.type
            )

            val transcriptEdits = allEdits[evidenceId] ?: emptyList()

            Evidence(
                id = evidenceId,
                caseId = caseSpreadsheetId.hashCode().toLong(),
                spreadsheetId = caseSpreadsheetId,
                type = row.getCell(2)?.stringCellValue ?: "",
                content = row.getCell(3)?.stringCellValue ?: "",
                formattedContent = row.getCell(4)?.stringCellValue,
                mediaUri = row.getCell(5)?.stringCellValue,
                timestamp = timestamp,
                sourceDocument = row.getCell(7)?.stringCellValue ?: "",
                documentDate = documentDate,
                allegationId = allegationId,
                category = row.getCell(10)?.stringCellValue ?: "",
                tags = tags,
                commentary = row.getCell(12)?.stringCellValue,
                linkedEvidenceIds = linkedIds,
                parentVideoId = row.getCell(14)?.stringCellValue,
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
            val caseDir = File(storageDir, caseSpreadsheetId).apply { if (!exists()) mkdirs() }
            val destinationFile = File(caseDir, "file_${System.currentTimeMillis()}.${mimeType.substringAfter('/')}")
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
                id = row.getCell(0)?.numericCellValue?.toInt() ?: 0,
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

        val oldContent = evidenceRow.getCell(3)?.stringCellValue ?: ""
        evidenceRow.getCell(3)?.setCellValue(newTranscript)

        val editsSheet = workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME) ?: workbook.createSheet(TRANSCRIPT_EDITS_SHEET_NAME).also {
            it.createRow(0).apply { TRANSCRIPT_EDITS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        editsSheet.createRow(editsSheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue((editsSheet.physicalNumberOfRows).toDouble()) // EditID
            createCell(1).setCellValue(evidence.id.toDouble()) // EvidenceID
            createCell(2).setCellValue(System.currentTimeMillis().toDouble()) // Timestamp
            createCell(3).setCellValue(reason) // Reason
            createCell(4).setCellValue(oldContent) // OldContent
        }
    }

    override suspend fun synchronize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!spreadsheetFile.exists()) {
            return@withContext Result.Success(Unit) // Nothing to sync
        }

        val folderIdResult = googleApiService.getOrCreateAppRootFolder()
        if (folderIdResult is Result.Success) {
            val folderId = folderIdResult.data
            // This is a one-way synchronization that uploads the local file to Google Drive.
            // It does not download changes from the cloud or handle conflicts.
            // This will create duplicate files on every sync.
            // TODO: Implement a more robust two-way synchronization mechanism.
            val uploadResult = googleApiService.uploadFile(spreadsheetFile, folderId, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            when (uploadResult) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(uploadResult.exception)
                is Result.UserRecoverableError -> Result.UserRecoverableError(uploadResult.exception)
            }
        } else if (folderIdResult is Result.Error) {
            Result.Error(folderIdResult.exception)
        } else if (folderIdResult is Result.UserRecoverableError) {
            Result.UserRecoverableError(folderIdResult.exception)
        } else {
            Result.Error(Exception("Unknown error getting or creating app root folder"))
        }
    }
}
