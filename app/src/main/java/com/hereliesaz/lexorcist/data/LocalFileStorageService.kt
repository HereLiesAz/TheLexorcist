package com.hereliesaz.lexorcist.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.service.VideoProcessingWorker
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException
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
import java.util.zip.ZipException // Added import
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LocalFileStorageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val settingsManager: SettingsManager,
    private val syncManager: SyncManager,
    @Named("googleDrive") private val googleDriveProvider: CloudStorageProvider,
    @Named("dropbox") private val dropboxProvider: CloudStorageProvider,
    @Named("oneDrive") private val oneDriveProvider: CloudStorageProvider,
    private val workManager: WorkManager
) : StorageService {

    private val storageDir: File by lazy {
        val customLocation = settingsManager.getStorageLocation()
        val dir = if (customLocation != null) {
            File(customLocation.toUri().path!!)
        } else {
            context.getExternalFilesDir(null) ?: context.filesDir
        }
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val spreadsheetFile: File by lazy { File(storageDir, "lexorcist_data.xlsx") }

    init {
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                Log.i("LocalFileStorageService", "Spreadsheet file does not exist or is empty. Creating a new one.")
                createNewSpreadsheet()
            } else {
                try {
                    // Attempt to open and validate existing spreadsheet
                    FileInputStream(spreadsheetFile).use { fis ->
                        XSSFWorkbook(fis).use { workbook ->
                            var modified = false
                            if (workbook.getSheet(CASES_SHEET_NAME) == null) {
                                createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
                                modified = true
                            }
                            if (workbook.getSheet(EVIDENCE_SHEET_NAME) == null) {
                                createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
                                modified = true
                            }
                            if (workbook.getSheet(ALLEGATIONS_SHEET_NAME) == null) {
                                createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
                                modified = true
                            }
                            if (workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME) == null) {
                                createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
                                modified = true
                            }
                            if (workbook.getSheet(EXHIBITS_SHEET_NAME) == null) {
                                createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)
                                modified = true
                            }

                            // Data migration: Check for and add missing columns to Evidence sheet
                            val evidenceSheet = workbook.getSheet(EVIDENCE_SHEET_NAME)
                            if (evidenceSheet != null) {
                                val headerRow = evidenceSheet.getRow(0)
                                if (headerRow != null) {
                                    val existingHeaders = (0 until headerRow.lastCellNum).mapNotNull { headerRow.getCell(it)?.stringCellValue }
                                    val missingHeaders = EVIDENCE_HEADER.filter { it !in existingHeaders }

                                    if (missingHeaders.isNotEmpty()) {
                                        var lastCellNum = headerRow.lastCellNum.toInt()
                                        if (lastCellNum < 0) lastCellNum = 0
                                        missingHeaders.forEach { header ->
                                            headerRow.createCell(lastCellNum++).setCellValue(header)
                                        }
                                        modified = true
                                        Log.i("LocalFileStorageService", "Added missing headers to Evidence sheet: $missingHeaders")
                                    }
                                }
                            }

                            if (modified) {
                                FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                                Log.i("LocalFileStorageService", "Added missing sheets or headers to existing spreadsheet.")
                            }
                        }
                    }
                } catch (zipEx: ZipException) {
                    Log.w("LocalFileStorageService", "Spreadsheet file '${spreadsheetFile.absolutePath}' is corrupted (ZipException). Deleting and creating a new one.", zipEx)
                    try {
                        if (spreadsheetFile.exists()) {
                            spreadsheetFile.delete()
                        }
                        createNewSpreadsheet()
                    } catch (delEx: Exception) {
                        Log.e("LocalFileStorageService", "Failed to delete corrupted spreadsheet file '${spreadsheetFile.absolutePath}' or create a new one.", delEx)
                        throw RuntimeException("Corrupted spreadsheet (ZipException) encountered, failed to delete it and/or create a new one.", delEx)
                    }
                } catch (noxmlEx: NotOfficeXmlFileException) {
                    Log.w("LocalFileStorageService", "Spreadsheet file '${spreadsheetFile.absolutePath}' is not a valid OOXML file (NotOfficeXmlFileException). Deleting and creating a new one.", noxmlEx)
                    try {
                        if (spreadsheetFile.exists()) {
                            spreadsheetFile.delete()
                        }
                        createNewSpreadsheet()
                    } catch (delEx: Exception) {
                        Log.e("LocalFileStorageService", "Failed to delete invalid OOXML spreadsheet file '${spreadsheetFile.absolutePath}' or create a new one.", delEx)
                        throw RuntimeException("Invalid OOXML spreadsheet encountered, failed to delete it and/or create a new one.", delEx)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("LocalFileStorageService", "Failed to initialize spreadsheet due to IOException", e)
            throw e // Rethrow critical IOExceptions
        } catch (e: SecurityException) {
            Log.e("LocalFileStorageService", "Failed to initialize spreadsheet due to SecurityException", e)
            throw e // Rethrow critical SecurityExceptions
        } catch (e: Exception) {
            Log.e("LocalFileStorageService", "An unexpected error occurred during spreadsheet initialization", e)
            throw RuntimeException("Failed to initialize spreadsheet due to an unexpected error", e)
        }
    }

    private fun createNewSpreadsheet() {
        XSSFWorkbook().use { workbook ->
            createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
            createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
            createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
            createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
            createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)
            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
            Log.i("LocalFileStorageService", "Successfully created a new spreadsheet file at '${spreadsheetFile.absolutePath}'.")
        }
    }

    private fun createSheetWithHeader(workbook: XSSFWorkbook, sheetName: String, headers: List<String>) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }
    }

    companion object {
        private const val CASES_SHEET_NAME = "Cases"
        private const val EVIDENCE_SHEET_NAME = "Evidence"
        private const val ALLEGATIONS_SHEET_NAME = "Allegations"
        private const val TRANSCRIPT_EDITS_SHEET_NAME = "TranscriptEdits"
        private const val EXHIBITS_SHEET_NAME = "Exhibits"

        private val CASES_HEADER = listOf("ID", "Name", "Plaintiffs", "Defendants", "Court", "FolderID", "LastModified", "IsArchived")
        private val EVIDENCE_HEADER = listOf("EvidenceID", "CaseID", "Type", "Content", "FormattedContent", "MediaUri", "Timestamp", "SourceDocument", "DocumentDate", "AllegationID", "Category", "Tags", "Commentary", "LinkedEvidenceIDs", "ParentVideoID", "Entities", "FileSize", "FileHash", "IsDuplicate")
        private val ALLEGATIONS_HEADER = listOf("AllegationID", "CaseID", "Text")
        private val TRANSCRIPT_EDITS_HEADER = listOf("EditID", "EvidenceID", "Timestamp", "Reason", "NewContent")
        private val EXHIBITS_HEADER = listOf("ExhibitID", "CaseID", "Name", "Description", "EvidenceIDs")
    }

    private suspend fun <T> readFromSpreadsheet(block: (XSSFWorkbook) -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                Log.w("LocalFileStorageService", "Spreadsheet file does not exist or is empty for read. Initializing with a new workbook.")
                 try {
                    initializeSpreadsheet() 
                } catch (e: Exception) {
                    Log.e("LocalFileStorageService", "Critical failure to re-initialize spreadsheet during read attempt.", e)
                    return@withContext Result.Error(IOException("Spreadsheet not available and re-initialization failed.", e))
                }
                if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                     Log.e("LocalFileStorageService", "Spreadsheet still not available after re-initialization attempt during read.")
                     return@withContext Result.Error(IOException("Spreadsheet not available after re-initialization."))
                }
            }
            FileInputStream(spreadsheetFile).use { fis ->
                val workbook = XSSFWorkbook(fis)
                val result = block(workbook)
                workbook.close() 
                Result.Success(result)
            }
        } catch (zipEx: ZipException) {
            Log.e("LocalFileStorageService", "Corrupted spreadsheet file encountered during read.", zipEx)
            Result.Error(zipEx) 
        } catch (e: Exception) {
            Log.e("LocalFileStorageService", "Error reading from spreadsheet", e)
            Result.Error(e)
        }
    }

    private suspend fun <T> writeToSpreadsheet(block: (XSSFWorkbook) -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                Log.w("LocalFileStorageService", "Spreadsheet file does not exist or is empty for write. Initializing with a new workbook.")
                 try {
                    initializeSpreadsheet() 
                } catch (e: Exception) {
                    Log.e("LocalFileStorageService", "Critical failure to re-initialize spreadsheet during write attempt.", e)
                    return@withContext Result.Error(IOException("Spreadsheet not available and re-initialization failed.", e))
                }
                 if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                     Log.e("LocalFileStorageService", "Spreadsheet still not available after re-initialization attempt during write.")
                     return@withContext Result.Error(IOException("Spreadsheet not available after re-initialization."))
                }
            }
            val workbook = FileInputStream(spreadsheetFile).use { fis -> XSSFWorkbook(fis) }
            val result = block(workbook)
            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
            workbook.close()
            Result.Success(result)
        } catch (zipEx: ZipException) {
            Log.e("LocalFileStorageService", "Corrupted spreadsheet file encountered during write.", zipEx)
            Result.Error(zipEx) 
        } catch (e: Exception) {
            Log.e("LocalFileStorageService", "Error writing to spreadsheet", e)
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
            val cell = row.getCell(idColumn)
            if (cell != null && cell.cellType == CellType.NUMERIC && cell.numericCellValue.toInt() == id) {
                return row
            } else if (cell != null && cell.cellType == CellType.STRING && cell.stringCellValue.toIntOrNull() == id) {
                 return row 
            }
        }
        return null
    }

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
                    Log.w("LocalFileStorageService", "Could not get numeric value from formula cell: ${e.message}")
                    null
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
            val oldDir = File(oldLocation.toUri().path!!)
            val newDir = File(newLocation.toUri().path!!)
            if (oldDir.exists() && oldDir.isDirectory) {
                oldDir.copyRecursively(newDir, overwrite = true)
                oldDir.deleteRecursively()
            }
        }
    }

    override suspend fun getAllCases(): Result<List<Case>> = readFromSpreadsheet { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: return@readFromSpreadsheet emptyList()
        (1..sheet.lastRowNum).mapNotNull { i -> 
            val row = sheet.getRow(i) ?: return@mapNotNull null
            val spreadsheetId = row.getCell(CASES_HEADER.indexOf("ID"))?.stringCellValue ?: ""
            if (spreadsheetId.isBlank()) return@mapNotNull null 

            Case(
                id = spreadsheetId.hashCode(), 
                spreadsheetId = spreadsheetId,
                name = row.getCell(CASES_HEADER.indexOf("Name"))?.stringCellValue ?: "",
                plaintiffs = row.getCell(CASES_HEADER.indexOf("Plaintiffs"))?.stringCellValue ?: "",
                defendants = row.getCell(CASES_HEADER.indexOf("Defendants"))?.stringCellValue ?: "",
                court = row.getCell(CASES_HEADER.indexOf("Court"))?.stringCellValue ?: "",
                folderId = row.getCell(CASES_HEADER.indexOf("FolderID"))?.stringCellValue,
                lastModifiedTime = getLongCellValueSafe(row.getCell(CASES_HEADER.indexOf("LastModified"))) ?: 0L,
                isArchived = getBooleanCellValueSafe(row.getCell(CASES_HEADER.indexOf("IsArchived"))) ?: false
            )
        }
    }

    override suspend fun getExhibitsForCase(caseSpreadsheetId: String): Result<List<Exhibit>> = readFromSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: return@readFromSpreadsheet emptyList()
        (1..sheet.lastRowNum).mapNotNull { i -> 
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(EXHIBITS_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) return@mapNotNull null

            val idCell = row.getCell(EXHIBITS_HEADER.indexOf("ExhibitID"))
            val exhibitId = getIntCellValueSafe(idCell) ?: run {
                Log.w("LocalFileStorageService", "Exhibit row ${row.rowNum} has invalid or missing ExhibitID.")
                return@mapNotNull null 
            }
            
            val evidenceIdsCell = row.getCell(EXHIBITS_HEADER.indexOf("EvidenceIDs"))
            val evidenceIds = (evidenceIdsCell?.stringCellValue ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }

            Exhibit(
                id = exhibitId,
                caseId = caseSpreadsheetId.hashCode().toLong(),
                name = row.getCell(EXHIBITS_HEADER.indexOf("Name"))?.stringCellValue ?: "",
                description = row.getCell(EXHIBITS_HEADER.indexOf("Description"))?.stringCellValue ?: "",
                evidenceIds = evidenceIds
            )
        }
    }

    override suspend fun addExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Exhibit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: workbook.createSheet(EXHIBITS_SHEET_NAME).also {
            it.createRow(0).apply { EXHIBITS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val lastId = (1..sheet.lastRowNum)
            .mapNotNull { i -> getIntCellValueSafe(sheet.getRow(i)?.getCell(EXHIBITS_HEADER.indexOf("ExhibitID"))) }
            .maxOrNull() ?: 0
        val newExhibit = exhibit.copy(id = lastId + 1)
        
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(EXHIBITS_HEADER.indexOf("ExhibitID")).setCellValue(newExhibit.id.toDouble())
            createCell(EXHIBITS_HEADER.indexOf("CaseID")).setCellValue(caseSpreadsheetId)
            createCell(EXHIBITS_HEADER.indexOf("Name")).setCellValue(newExhibit.name)
            createCell(EXHIBITS_HEADER.indexOf("Description")).setCellValue(newExhibit.description)
            createCell(EXHIBITS_HEADER.indexOf("EvidenceIDs")).setCellValue(newExhibit.evidenceIds.joinToString(","))
        }
        newExhibit
    }

    override suspend fun updateExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Unit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: throw IOException("Exhibits sheet not found.")
        val row = findRowById(sheet, exhibit.id, EXHIBITS_HEADER.indexOf("ExhibitID")) ?: throw IOException("Exhibit with id ${exhibit.id} not found for case $caseSpreadsheetId.")
        
        if (row.getCell(EXHIBITS_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) {
            throw IOException("Exhibit with id ${exhibit.id} does not belong to case $caseSpreadsheetId.")
        }

        (row.getCell(EXHIBITS_HEADER.indexOf("Name")) ?: row.createCell(EXHIBITS_HEADER.indexOf("Name"))).setCellValue(exhibit.name)
        (row.getCell(EXHIBITS_HEADER.indexOf("Description")) ?: row.createCell(EXHIBITS_HEADER.indexOf("Description"))).setCellValue(exhibit.description)
        (row.getCell(EXHIBITS_HEADER.indexOf("EvidenceIDs")) ?: row.createCell(EXHIBITS_HEADER.indexOf("EvidenceIDs"))).setCellValue(exhibit.evidenceIds.joinToString(","))
        Unit 
    }

    override suspend fun deleteExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Unit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: throw IOException("Exhibits sheet not found.")
        val row = findRowById(sheet, exhibit.id, EXHIBITS_HEADER.indexOf("ExhibitID")) ?: throw IOException("Exhibit with id ${exhibit.id} not found for case $caseSpreadsheetId.")

        if (row.getCell(EXHIBITS_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) {
            throw IOException("Exhibit with id ${exhibit.id} does not belong to case $caseSpreadsheetId. Cannot delete.")
        }
        
        val rowIndex = row.rowNum
        sheet.removeRow(row)
        if (rowIndex < sheet.lastRowNum) { 
            sheet.shiftRows(rowIndex + 1, sheet.lastRowNum, -1)
        }
        Unit 
    }

    override suspend fun createCase(case: Case): Result<Case> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: workbook.createSheet(CASES_SHEET_NAME).also {
            it.createRow(0).apply { CASES_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val newId = UUID.randomUUID().toString() 
        val newCase = case.copy(spreadsheetId = newId, lastModifiedTime = System.currentTimeMillis())
        
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(CASES_HEADER.indexOf("ID")).setCellValue(newCase.spreadsheetId)
            createCell(CASES_HEADER.indexOf("Name")).setCellValue(newCase.name)
            createCell(CASES_HEADER.indexOf("Plaintiffs")).setCellValue(newCase.plaintiffs)
            createCell(CASES_HEADER.indexOf("Defendants")).setCellValue(newCase.defendants)
            createCell(CASES_HEADER.indexOf("Court")).setCellValue(newCase.court)
            createCell(CASES_HEADER.indexOf("FolderID")).setCellValue(newCase.folderId ?: "") 
            createCell(CASES_HEADER.indexOf("LastModified")).setCellValue(newCase.lastModifiedTime!!.toDouble()) 
            createCell(CASES_HEADER.indexOf("IsArchived")).setCellValue(newCase.isArchived)
        }
        newCase
    }

    override suspend fun updateCase(case: Case): Result<Unit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: throw IOException("Cases sheet not found.")
        val row = findRowById(sheet, case.spreadsheetId, CASES_HEADER.indexOf("ID")) ?: throw IOException("Case with id ${case.spreadsheetId} not found.")
        
        (row.getCell(CASES_HEADER.indexOf("Name")) ?: row.createCell(CASES_HEADER.indexOf("Name"))).setCellValue(case.name)
        (row.getCell(CASES_HEADER.indexOf("Plaintiffs")) ?: row.createCell(CASES_HEADER.indexOf("Plaintiffs"))).setCellValue(case.plaintiffs)
        (row.getCell(CASES_HEADER.indexOf("Defendants")) ?: row.createCell(CASES_HEADER.indexOf("Defendants"))).setCellValue(case.defendants)
        (row.getCell(CASES_HEADER.indexOf("Court")) ?: row.createCell(CASES_HEADER.indexOf("Court"))).setCellValue(case.court)
        (row.getCell(CASES_HEADER.indexOf("FolderID")) ?: row.createCell(CASES_HEADER.indexOf("FolderID"))).setCellValue(case.folderId ?: "")
        (row.getCell(CASES_HEADER.indexOf("LastModified")) ?: row.createCell(CASES_HEADER.indexOf("LastModified"))).setCellValue(System.currentTimeMillis().toDouble()) 
        (row.getCell(CASES_HEADER.indexOf("IsArchived")) ?: row.createCell(CASES_HEADER.indexOf("IsArchived"))).setCellValue(case.isArchived)
        Unit
    }

    override suspend fun deleteCase(case: Case): Result<Unit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: throw IOException("Cases sheet not found.")
        val row = findRowById(sheet, case.spreadsheetId, CASES_HEADER.indexOf("ID")) ?: throw IOException("Case with id ${case.spreadsheetId} not found.")
        
        val rowIndex = row.rowNum
        sheet.removeRow(row)
        if (rowIndex < sheet.lastRowNum) {
            sheet.shiftRows(rowIndex + 1, sheet.lastRowNum, -1)
        }
        Unit
    }

    override suspend fun getEvidenceForCase(caseSpreadsheetId: String): Result<List<Evidence>> = readFromSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: return@readFromSpreadsheet emptyList()
        val editsSheet = workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME)

        val allEdits = editsSheet?.let {
            (1..it.lastRowNum).mapNotNull { j -> 
                val editRow = it.getRow(j) ?: return@mapNotNull null
                val evidenceId = getIntCellValueSafe(editRow.getCell(TRANSCRIPT_EDITS_HEADER.indexOf("EvidenceID")))
                if (evidenceId != null) {
                    evidenceId to com.hereliesaz.lexorcist.model.TranscriptEdit(
                        timestamp = getLongCellValueSafe(editRow.getCell(TRANSCRIPT_EDITS_HEADER.indexOf("Timestamp"))) ?: 0L,
                        reason = editRow.getCell(TRANSCRIPT_EDITS_HEADER.indexOf("Reason"))?.stringCellValue ?: "",
                        content = editRow.getCell(TRANSCRIPT_EDITS_HEADER.indexOf("NewContent"))?.stringCellValue ?: ""
                    )
                } else {
                    null
                }
            }
        }?.groupBy({ it.first }, { it.second }) ?: emptyMap()

        (1..sheet.lastRowNum).mapNotNull { i -> 
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(EVIDENCE_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) return@mapNotNull null

            val idCell = row.getCell(EVIDENCE_HEADER.indexOf("EvidenceID"))
            val evidenceId = getIntCellValueSafe(idCell) ?: run {
                 Log.w("LocalFileStorageService", "Evidence row ${row.rowNum} has invalid or missing EvidenceID.")
                 return@mapNotNull null
            }

            val timestamp = getLongCellValueSafe(row.getCell(EVIDENCE_HEADER.indexOf("Timestamp"))) ?: 0L
            val documentDate = getLongCellValueSafe(row.getCell(EVIDENCE_HEADER.indexOf("DocumentDate"))) ?: 0L
            
            val allegationIdCell = row.getCell(EVIDENCE_HEADER.indexOf("AllegationID"))
            val allegationIdString = when (allegationIdCell?.cellType) {
                CellType.NUMERIC -> allegationIdCell.numericCellValue.toInt().toString()
                CellType.STRING -> allegationIdCell.stringCellValue
                else -> null
            }

            val tags = (row.getCell(EVIDENCE_HEADER.indexOf("Tags"))?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }
            val linkedIds = (row.getCell(EVIDENCE_HEADER.indexOf("LinkedEvidenceIDs"))?.stringCellValue ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }

            val entitiesJson = row.getCell(EVIDENCE_HEADER.indexOf("Entities"))?.stringCellValue ?: "{}"
            val entities: Map<String, List<String>> = try {
                gson.fromJson(entitiesJson, object : TypeToken<Map<String, List<String>>>() {}.type)
            } catch (e: Exception) {
                Log.e("LocalFileStorageService", "Failed to parse entities JSON: $entitiesJson", e)
                emptyMap()
            }
            
            val transcriptEdits = allEdits[evidenceId] ?: emptyList()

            Evidence(
                id = evidenceId,
                caseId = caseSpreadsheetId.hashCode().toLong(),
                spreadsheetId = caseSpreadsheetId,
                type = row.getCell(EVIDENCE_HEADER.indexOf("Type"))?.stringCellValue ?: "",
                content = row.getCell(EVIDENCE_HEADER.indexOf("Content"))?.stringCellValue ?: "",
                formattedContent = row.getCell(EVIDENCE_HEADER.indexOf("FormattedContent"))?.stringCellValue,
                mediaUri = row.getCell(EVIDENCE_HEADER.indexOf("MediaUri"))?.stringCellValue,
                timestamp = timestamp,
                sourceDocument = row.getCell(EVIDENCE_HEADER.indexOf("SourceDocument"))?.stringCellValue ?: "",
                documentDate = documentDate,
                allegationId = allegationIdString,
                allegationElementName = null,
                category = row.getCell(EVIDENCE_HEADER.indexOf("Category"))?.stringCellValue ?: "",
                tags = tags,
                commentary = row.getCell(EVIDENCE_HEADER.indexOf("Commentary"))?.stringCellValue,
                linkedEvidenceIds = linkedIds,
                parentVideoId = row.getCell(EVIDENCE_HEADER.indexOf("ParentVideoID"))?.stringCellValue,
                entities = entities,
                transcriptEdits = transcriptEdits,
                fileSize = getLongCellValueSafe(row.getCell(EVIDENCE_HEADER.indexOf("FileSize"))) ?: 0L,
                fileHash = row.getCell(EVIDENCE_HEADER.indexOf("FileHash"))?.stringCellValue,
                isDuplicate = getBooleanCellValueSafe(row.getCell(EVIDENCE_HEADER.indexOf("IsDuplicate"))) ?: false
            )
        }
    }

    override suspend fun addEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Evidence> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: workbook.createSheet(EVIDENCE_SHEET_NAME).also {
            it.createRow(0).apply { EVIDENCE_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val lastId = (1..sheet.lastRowNum)
            .mapNotNull { i -> getIntCellValueSafe(sheet.getRow(i)?.getCell(EVIDENCE_HEADER.indexOf("EvidenceID"))) }
            .maxOrNull() ?: 0
        val newEvidence = evidence.copy(id = lastId + 1)
        
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(EVIDENCE_HEADER.indexOf("EvidenceID")).setCellValue(newEvidence.id.toDouble())
            createCell(EVIDENCE_HEADER.indexOf("CaseID")).setCellValue(caseSpreadsheetId)
            createCell(EVIDENCE_HEADER.indexOf("Type")).setCellValue(newEvidence.type)
            createCell(EVIDENCE_HEADER.indexOf("Content")).setCellValue(newEvidence.content)
            createCell(EVIDENCE_HEADER.indexOf("FormattedContent")).setCellValue(newEvidence.formattedContent)
            createCell(EVIDENCE_HEADER.indexOf("MediaUri")).setCellValue(newEvidence.mediaUri)
            createCell(EVIDENCE_HEADER.indexOf("Timestamp")).setCellValue(newEvidence.timestamp.toDouble())
            createCell(EVIDENCE_HEADER.indexOf("SourceDocument")).setCellValue(newEvidence.sourceDocument)
            createCell(EVIDENCE_HEADER.indexOf("DocumentDate")).setCellValue(newEvidence.documentDate.toDouble())
            val allegationCell = createCell(EVIDENCE_HEADER.indexOf("AllegationID"))
            newEvidence.allegationId?.let {
                allegationCell.setCellValue(it) 
            } ?: allegationCell.setBlank()
            createCell(EVIDENCE_HEADER.indexOf("Category")).setCellValue(newEvidence.category)
            createCell(EVIDENCE_HEADER.indexOf("Tags")).setCellValue(newEvidence.tags.joinToString(","))
            createCell(EVIDENCE_HEADER.indexOf("Commentary")).setCellValue(newEvidence.commentary ?: "")
            createCell(EVIDENCE_HEADER.indexOf("LinkedEvidenceIDs")).setCellValue(newEvidence.linkedEvidenceIds.joinToString(","))
            createCell(EVIDENCE_HEADER.indexOf("ParentVideoID")).setCellValue(newEvidence.parentVideoId ?: "")
            createCell(EVIDENCE_HEADER.indexOf("Entities")).setCellValue(gson.toJson(newEvidence.entities))
            createCell(EVIDENCE_HEADER.indexOf("FileSize")).setCellValue(newEvidence.fileSize.toDouble())
            createCell(EVIDENCE_HEADER.indexOf("FileHash")).setCellValue(newEvidence.fileHash ?: "")
            createCell(EVIDENCE_HEADER.indexOf("IsDuplicate")).setCellValue(newEvidence.isDuplicate)
        }
        newEvidence
    }

    override suspend fun addEvidenceList(caseSpreadsheetId: String, evidenceList: List<Evidence>): Result<List<Evidence>> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: workbook.createSheet(EVIDENCE_SHEET_NAME).also {
            it.createRow(0).apply { EVIDENCE_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        var lastId = (1..sheet.lastRowNum)
            .mapNotNull { i -> getIntCellValueSafe(sheet.getRow(i)?.getCell(EVIDENCE_HEADER.indexOf("EvidenceID"))) }
            .maxOrNull() ?: 0
        
        val addedEvidenceList = mutableListOf<Evidence>()

        evidenceList.forEach { evidence ->
            lastId++
            val newEvidence = evidence.copy(id = lastId)
            sheet.createRow(sheet.physicalNumberOfRows).apply {
                createCell(EVIDENCE_HEADER.indexOf("EvidenceID")).setCellValue(newEvidence.id.toDouble())
                createCell(EVIDENCE_HEADER.indexOf("CaseID")).setCellValue(caseSpreadsheetId)
                createCell(EVIDENCE_HEADER.indexOf("Type")).setCellValue(newEvidence.type)
                createCell(EVIDENCE_HEADER.indexOf("Content")).setCellValue(newEvidence.content)
                createCell(EVIDENCE_HEADER.indexOf("FormattedContent")).setCellValue(newEvidence.formattedContent)
                createCell(EVIDENCE_HEADER.indexOf("MediaUri")).setCellValue(newEvidence.mediaUri)
                createCell(EVIDENCE_HEADER.indexOf("Timestamp")).setCellValue(newEvidence.timestamp.toDouble())
                createCell(EVIDENCE_HEADER.indexOf("SourceDocument")).setCellValue(newEvidence.sourceDocument)
                createCell(EVIDENCE_HEADER.indexOf("DocumentDate")).setCellValue(newEvidence.documentDate.toDouble())
                val allegationCell = createCell(EVIDENCE_HEADER.indexOf("AllegationID"))
                newEvidence.allegationId?.let {
                    allegationCell.setCellValue(it)
                } ?: allegationCell.setBlank()
                createCell(EVIDENCE_HEADER.indexOf("Category")).setCellValue(newEvidence.category)
                createCell(EVIDENCE_HEADER.indexOf("Tags")).setCellValue(newEvidence.tags.joinToString(","))
                createCell(EVIDENCE_HEADER.indexOf("Commentary")).setCellValue(newEvidence.commentary ?: "")
                createCell(EVIDENCE_HEADER.indexOf("LinkedEvidenceIDs")).setCellValue(newEvidence.linkedEvidenceIds.joinToString(","))
                createCell(EVIDENCE_HEADER.indexOf("ParentVideoID")).setCellValue(newEvidence.parentVideoId ?: "")
                createCell(EVIDENCE_HEADER.indexOf("Entities")).setCellValue(gson.toJson(newEvidence.entities))
                createCell(EVIDENCE_HEADER.indexOf("FileSize")).setCellValue(newEvidence.fileSize.toDouble())
                createCell(EVIDENCE_HEADER.indexOf("FileHash")).setCellValue(newEvidence.fileHash ?: "")
                createCell(EVIDENCE_HEADER.indexOf("IsDuplicate")).setCellValue(newEvidence.isDuplicate)
            }
            addedEvidenceList.add(newEvidence)
        }
        addedEvidenceList
    }

    override suspend fun updateEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val row = findRowById(sheet, evidence.id, EVIDENCE_HEADER.indexOf("EvidenceID")) ?: throw IOException("Evidence with id ${evidence.id} not found for case $caseSpreadsheetId.")

        if (row.getCell(EVIDENCE_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) {
             throw IOException("Evidence id ${evidence.id} does not belong to case $caseSpreadsheetId")
        }

        (row.getCell(EVIDENCE_HEADER.indexOf("Type")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Type"))).setCellValue(evidence.type)
        (row.getCell(EVIDENCE_HEADER.indexOf("Content")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Content"))).setCellValue(evidence.content)
        (row.getCell(EVIDENCE_HEADER.indexOf("FormattedContent")) ?: row.createCell(EVIDENCE_HEADER.indexOf("FormattedContent"))).setCellValue(evidence.formattedContent)
        (row.getCell(EVIDENCE_HEADER.indexOf("MediaUri")) ?: row.createCell(EVIDENCE_HEADER.indexOf("MediaUri"))).setCellValue(evidence.mediaUri)
        (row.getCell(EVIDENCE_HEADER.indexOf("Timestamp")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Timestamp"))).setCellValue(evidence.timestamp.toDouble())
        (row.getCell(EVIDENCE_HEADER.indexOf("SourceDocument")) ?: row.createCell(EVIDENCE_HEADER.indexOf("SourceDocument"))).setCellValue(evidence.sourceDocument)
        (row.getCell(EVIDENCE_HEADER.indexOf("DocumentDate")) ?: row.createCell(EVIDENCE_HEADER.indexOf("DocumentDate"))).setCellValue(evidence.documentDate.toDouble())
        val allegationCell = row.getCell(EVIDENCE_HEADER.indexOf("AllegationID")) ?: row.createCell(EVIDENCE_HEADER.indexOf("AllegationID"))
        evidence.allegationId?.let {
            allegationCell.setCellValue(it)
        } ?: allegationCell.setBlank()
        (row.getCell(EVIDENCE_HEADER.indexOf("Category")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Category"))).setCellValue(evidence.category)
        (row.getCell(EVIDENCE_HEADER.indexOf("Tags")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Tags"))).setCellValue(evidence.tags.joinToString(","))
        (row.getCell(EVIDENCE_HEADER.indexOf("Commentary")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Commentary"))).setCellValue(evidence.commentary ?: "")
        (row.getCell(EVIDENCE_HEADER.indexOf("LinkedEvidenceIDs")) ?: row.createCell(EVIDENCE_HEADER.indexOf("LinkedEvidenceIDs"))).setCellValue(evidence.linkedEvidenceIds.joinToString(","))
        (row.getCell(EVIDENCE_HEADER.indexOf("ParentVideoID")) ?: row.createCell(EVIDENCE_HEADER.indexOf("ParentVideoID"))).setCellValue(evidence.parentVideoId ?: "")
        (row.getCell(EVIDENCE_HEADER.indexOf("Entities")) ?: row.createCell(EVIDENCE_HEADER.indexOf("Entities"))).setCellValue(gson.toJson(evidence.entities))
        (row.getCell(EVIDENCE_HEADER.indexOf("FileSize")) ?: row.createCell(EVIDENCE_HEADER.indexOf("FileSize"))).setCellValue(evidence.fileSize.toDouble())
        (row.getCell(EVIDENCE_HEADER.indexOf("FileHash")) ?: row.createCell(EVIDENCE_HEADER.indexOf("FileHash"))).setCellValue(evidence.fileHash ?: "")
        (row.getCell(EVIDENCE_HEADER.indexOf("IsDuplicate")) ?: row.createCell(EVIDENCE_HEADER.indexOf("IsDuplicate"))).setCellValue(evidence.isDuplicate)
        Unit
    }

    override suspend fun deleteEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val row = findRowById(sheet, evidence.id, EVIDENCE_HEADER.indexOf("EvidenceID")) ?: throw IOException("Evidence with id ${evidence.id} not found for case $caseSpreadsheetId.")
        
        if (row.getCell(EVIDENCE_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) {
             throw IOException("Evidence id ${evidence.id} does not belong to case $caseSpreadsheetId. Cannot delete.")
        }

        val rowIndex = row.rowNum
        sheet.removeRow(row)
        if (rowIndex < sheet.lastRowNum) {
            sheet.shiftRows(rowIndex + 1, sheet.lastRowNum, -1)
        }
        Unit
    }

    // Helper function to get display name
    private fun getDisplayName(context: Context, uri: Uri): String {
        var displayName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            displayName = cursor.getString(displayNameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("LocalFileStorageService", "Error getting display name for content URI $uri: ${e.message}")
            }
        }
        if (displayName == null) {
            displayName = uri.lastPathSegment
        }
        return displayName ?: "file_${System.currentTimeMillis()}"
    }

    // Sanitize file name
    private fun sanitizeFileName(name: String): String {
        return name.replace(kotlin.text.Regex("[\\/:*?"<>|]"), "_")
    }

    override suspend fun uploadFile(caseSpreadsheetId: String, fileUri: Uri, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val caseDir = File(storageDir, caseSpreadsheetId).apply { if (!exists()) mkdirs() }
            val rawDir = File(caseDir, "raw").apply { if (!exists()) mkdirs() }

            val rawFileName = getDisplayName(context, fileUri)
            val baseName = rawFileName.substringBeforeLast('.')
            val originalExtension = rawFileName.substringAfterLast('.', "")

            val sanitizedBaseName = sanitizeFileName(baseName)

            val mimeTypeSubPart = mimeType.substringAfter('/', "")
            
            val finalExtension = if (mimeTypeSubPart.isNotEmpty() && mimeTypeSubPart != "octet-stream") {
                mimeTypeSubPart // Use extension from MIME type if valid and not generic
            } else if (originalExtension.isNotEmpty()) {
                originalExtension // Fallback to original extension
            } else {
                "dat" // Default if no other extension found
            }

            val finalFileName = if (sanitizedBaseName.endsWith(".$finalExtension", ignoreCase = true)) {
                 sanitizedBaseName // Avoids double extension like .opus.opus if already present
            } else {
                 "$sanitizedBaseName.$finalExtension"
            }
            
            val destinationFile = File(rawDir, finalFileName)
            Log.d("LocalFileStorageService", "Uploading file: URI=$fileUri, MimeType=$mimeType, DestFile=$destinationFile")


            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (mimeType.startsWith("video/")) {
                // Get Case details for the worker
                val casesResult = getAllCases() // This might be inefficient, consider a direct getCaseById
                if (casesResult is Result.Success) {
                    val caseDetails = casesResult.data.find { it.spreadsheetId == caseSpreadsheetId }
                    if (caseDetails != null) {
                        val workData = Data.Builder()
                            .putString(VideoProcessingWorker.KEY_VIDEO_URI, destinationFile.toUri().toString())
                            .putInt(VideoProcessingWorker.KEY_CASE_ID, caseDetails.id) // case.id is Int (spreadsheetId.hashCode())
                            .putString(VideoProcessingWorker.KEY_CASE_NAME, caseDetails.name)
                            .putString(VideoProcessingWorker.KEY_SPREADSHEET_ID, caseSpreadsheetId)
                            .build()

                        val videoProcessingRequest = OneTimeWorkRequestBuilder<VideoProcessingWorker>()
                            .setInputData(workData)
                            .build()
                        
                        workManager.enqueue(videoProcessingRequest)
                        Log.i("LocalFileStorageService", "Enqueued video processing for ${destinationFile.name} (Case ID: ${caseDetails.id})")
                    } else {
                        Log.e("LocalFileStorageService", "Could not find case details for $caseSpreadsheetId to enqueue video processing.")
                    }
                } else if (casesResult is Result.Error) {
                     Log.e("LocalFileStorageService", "Failed to retrieve case list to enqueue video processing: ${casesResult.exception.message}")
                }
            }

            Result.Success(destinationFile.absolutePath)
        } catch (e: Exception) {
            Log.e("LocalFileStorageService", "Error uploading file for case $caseSpreadsheetId, URI $fileUri", e)
            Result.Error(e)
        }
    }

    override suspend fun getAllegationsForCase(caseSpreadsheetId: String): Result<List<Allegation>> = readFromSpreadsheet { workbook ->
        val sheet = workbook.getSheet(ALLEGATIONS_SHEET_NAME) ?: return@readFromSpreadsheet emptyList()
        (1..sheet.lastRowNum).mapNotNull { i -> 
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(ALLEGATIONS_HEADER.indexOf("CaseID"))?.stringCellValue != caseSpreadsheetId) return@mapNotNull null

            val allegationIdCell = row.getCell(ALLEGATIONS_HEADER.indexOf("AllegationID"))
            val allegationId = getIntCellValueSafe(allegationIdCell) ?: run {
                Log.w("LocalFileStorageService", "Allegation row ${row.rowNum} has invalid or missing AllegationID.")
                return@mapNotNull null
            }
            
            val textFromSheet = row.getCell(ALLEGATIONS_HEADER.indexOf("Text"))?.stringCellValue
            val masterAllegation = AllegationProvider.getAllegationById(allegationId)

            Allegation( 
                id = allegationId,
                spreadsheetId = caseSpreadsheetId,
                text = textFromSheet ?: masterAllegation?.allegationName ?: "" // Changed from .name to .allegationName
            )
        }
    }

    override suspend fun addAllegation(caseSpreadsheetId: String, allegation: Allegation): Result<Allegation> = writeToSpreadsheet { workbook ->
        val sheet = workbook.getSheet(ALLEGATIONS_SHEET_NAME) ?: workbook.createSheet(ALLEGATIONS_SHEET_NAME).also {
            it.createRow(0).apply { ALLEGATIONS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val newAllegationId = if (allegation.id != 0 && findRowById(sheet, allegation.id, ALLEGATIONS_HEADER.indexOf("AllegationID")) == null) {
            allegation.id 
        } else {
             (1..sheet.lastRowNum)
                .mapNotNull { i -> getIntCellValueSafe(sheet.getRow(i)?.getCell(ALLEGATIONS_HEADER.indexOf("AllegationID"))) }
                .maxOrNull()?.plus(1) ?: 1 
        }

        val newAllegation = allegation.copy(id = newAllegationId)
        
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(ALLEGATIONS_HEADER.indexOf("AllegationID")).setCellValue(newAllegation.id.toDouble())
            createCell(ALLEGATIONS_HEADER.indexOf("CaseID")).setCellValue(caseSpreadsheetId)
            createCell(ALLEGATIONS_HEADER.indexOf("Text")).setCellValue(newAllegation.text) 
        }
        newAllegation
    }

    override suspend fun updateTranscript(evidence: Evidence, newTranscript: String, reason: String): Result<Unit> = writeToSpreadsheet { workbook ->
        val evidenceSheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val evidenceRow = findRowById(sheet = evidenceSheet, id = evidence.id, idColumn = EVIDENCE_HEADER.indexOf("EvidenceID")) ?: throw IOException("Evidence with id ${evidence.id} not found.")

        (evidenceRow.getCell(EVIDENCE_HEADER.indexOf("Content")) ?: evidenceRow.createCell(EVIDENCE_HEADER.indexOf("Content"))).setCellValue(newTranscript)
        (evidenceRow.getCell(EVIDENCE_HEADER.indexOf("FormattedContent")) ?: evidenceRow.createCell(EVIDENCE_HEADER.indexOf("FormattedContent"))).setCellValue("```\n$newTranscript\n```")

        val editsSheet = workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME) ?: workbook.createSheet(TRANSCRIPT_EDITS_SHEET_NAME).also {
            it.createRow(0).apply { TRANSCRIPT_EDITS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val lastEditId = (1..editsSheet.lastRowNum)
            .mapNotNull { i -> getIntCellValueSafe(editsSheet.getRow(i)?.getCell(TRANSCRIPT_EDITS_HEADER.indexOf("EditID"))) }
            .maxOrNull() ?: 0
        val newEditId = lastEditId + 1

        editsSheet.createRow(editsSheet.physicalNumberOfRows).apply {
            createCell(TRANSCRIPT_EDITS_HEADER.indexOf("EditID")).setCellValue(newEditId.toDouble())
            createCell(TRANSCRIPT_EDITS_HEADER.indexOf("EvidenceID")).setCellValue(evidence.id.toDouble())
            createCell(TRANSCRIPT_EDITS_HEADER.indexOf("Timestamp")).setCellValue(System.currentTimeMillis().toDouble())
            createCell(TRANSCRIPT_EDITS_HEADER.indexOf("Reason")).setCellValue(reason)
            createCell(TRANSCRIPT_EDITS_HEADER.indexOf("NewContent")).setCellValue(newTranscript)
        }
        Unit
    }

    override suspend fun synchronize(): Result<Unit> {
        val selectedProviderName = settingsManager.getSelectedCloudProvider()
        val cloudStorageProvider = when (selectedProviderName) {
            "GoogleDrive" -> googleDriveProvider
            "Dropbox" -> dropboxProvider
            "OneDrive" -> oneDriveProvider
            else -> {
                Log.i("LocalFileStorageService", "No cloud provider selected for synchronization.")
                null
            }
        }

        return if (cloudStorageProvider != null) {
            Log.i("LocalFileStorageService", "Starting synchronization with $selectedProviderName.")
            syncManager.synchronize(cloudStorageProvider, this) 
        } else {
            Result.Success(Unit) 
        }
    }
}
