package com.hereliesaz.lexorcist.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
    @Named("oneDrive") private val oneDriveProvider: CloudStorageProvider
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

    private val spreadsheetFile: File by lazy {
        File(storageDir, "lexorcist_data.xlsx").also {
            if (!it.exists()) {
                initializeSpreadsheet()
            }
        }
    }

    init {
        // Ensure the spreadsheet is initialized when the service is created.
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            val workbook = if (spreadsheetFile.exists() && spreadsheetFile.length() > 0) {
                FileInputStream(spreadsheetFile).use { XSSFWorkbook(it) }
            } else {
                XSSFWorkbook()
            }

            var modified = false

            if (workbook.getSheet(CASES_SHEET_NAME) == null) {
                workbook.createSheet(CASES_SHEET_NAME).createRow(0).apply {
                    CASES_HEADER.forEachIndexed { index, header -> createCell(index).setCellValue(header) }
                }
                modified = true
            }

            if (workbook.getSheet(EVIDENCE_SHEET_NAME) == null) {
                workbook.createSheet(EVIDENCE_SHEET_NAME).createRow(0).apply {
                    EVIDENCE_HEADER.forEachIndexed { index, header -> createCell(index).setCellValue(header) }
                }
                modified = true
            }

            if (workbook.getSheet(ALLEGATIONS_SHEET_NAME) == null) {
                workbook.createSheet(ALLEGATIONS_SHEET_NAME).createRow(0).apply {
                    ALLEGATIONS_HEADER.forEachIndexed { index, header -> createCell(index).setCellValue(header) }
                }
                modified = true
            }

            if (workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME) == null) {
                workbook.createSheet(TRANSCRIPT_EDITS_SHEET_NAME).createRow(0).apply {
                    TRANSCRIPT_EDITS_HEADER.forEachIndexed { index, header -> createCell(index).setCellValue(header) }
                }
                modified = true
            }

            if (workbook.getSheet(EXHIBITS_SHEET_NAME) == null) {
                workbook.createSheet(EXHIBITS_SHEET_NAME).createRow(0).apply {
                    EXHIBITS_HEADER.forEachIndexed { index, header -> createCell(index).setCellValue(header) }
                }
                modified = true
            }

            if (modified) {
                FileOutputStream(spreadsheetFile).use { workbook.write(it) }
            }
            workbook.close()
        } catch (e: Exception) {
            // Log this exception, as it's critical for debugging file system issues.
            // Consider using a more robust logging framework if available.
            e.printStackTrace()
        }
    }

    init {
        // Ensure the spreadsheet is initialized when the service is created.
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                // If file doesn't exist or is empty, create a new one with all sheets.
                XSSFWorkbook().use { workbook ->
                    createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
                    createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
                    createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
                    createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
                    createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)

                    FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                }
            } else {
                // If file exists, open it and check for missing sheets.
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

                        if (modified) {
                            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log this exception, as it's critical for debugging file system issues.
            e.printStackTrace()
        }
    }

    private fun createSheetWithHeader(workbook: XSSFWorkbook, sheetName: String, headers: List<String>) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }
    }

    init {
        // Ensure the spreadsheet is initialized when the service is created.
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                // If file doesn't exist or is empty, create a new one with all sheets.
                XSSFWorkbook().use { workbook ->
                    createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
                    createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
                    createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
                    createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
                    createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)

                    FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                }
            } else {
                // If file exists, open it and check for missing sheets.
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

                        if (modified) {
                            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log this exception, as it's critical for debugging file system issues.
            e.printStackTrace()
        }
    }

    private fun createSheetWithHeader(workbook: XSSFWorkbook, sheetName: String, headers: List<String>) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }
    }

    init {
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                XSSFWorkbook().use { workbook ->
                    createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
                    createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
                    createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
                    createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
                    createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)

                    FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                }
            } else {
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

                        if (modified) {
                            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createSheetWithHeader(workbook: XSSFWorkbook, sheetName: String, headers: List<String>) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }
    }

    init {
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                XSSFWorkbook().use { workbook ->
                    createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
                    createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
                    createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
                    createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
                    createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)

                    FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                }
            } else {
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

                        if (modified) {
                            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createSheetWithHeader(workbook: XSSFWorkbook, sheetName: String, headers: List<String>) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }
    }

    init {
        initializeSpreadsheet()
    }

    private fun initializeSpreadsheet() {
        try {
            if (!spreadsheetFile.exists() || spreadsheetFile.length() == 0L) {
                XSSFWorkbook().use { workbook ->
                    createSheetWithHeader(workbook, CASES_SHEET_NAME, CASES_HEADER)
                    createSheetWithHeader(workbook, EVIDENCE_SHEET_NAME, EVIDENCE_HEADER)
                    createSheetWithHeader(workbook, ALLEGATIONS_SHEET_NAME, ALLEGATIONS_HEADER)
                    createSheetWithHeader(workbook, TRANSCRIPT_EDITS_SHEET_NAME, TRANSCRIPT_EDITS_HEADER)
                    createSheetWithHeader(workbook, EXHIBITS_SHEET_NAME, EXHIBITS_HEADER)

                    FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                }
            } else {
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

                        if (modified) {
                            FileOutputStream(spreadsheetFile).use { fos -> workbook.write(fos) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        private val EVIDENCE_HEADER = listOf("EvidenceID", "CaseID", "Type", "Content", "FormattedContent", "MediaUri", "Timestamp", "SourceDocument", "DocumentDate", "AllegationID", "Category", "Tags", "Commentary", "LinkedEvidenceIDs", "ParentVideoID", "Entities")
        private val ALLEGATIONS_HEADER = listOf("AllegationID", "CaseID", "Text")
        private val TRANSCRIPT_EDITS_HEADER = listOf("EditID", "EvidenceID", "Timestamp", "Reason", "NewContent")
        private val EXHIBITS_HEADER = listOf("ExhibitID", "CaseID", "Name", "Description", "EvidenceIDs")
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
            if (getIntCellValueSafe(row.getCell(idColumn)) == id) {
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

    override suspend fun getAllCases(): Result<List<Case>> = execute { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: return@execute emptyList()
        (1..sheet.lastRowNum).mapNotNull { i ->
            val row = sheet.getRow(i) ?: return@mapNotNull null
            val spreadsheetId = row.getCell(0)?.stringCellValue ?: ""
            Case(
                id = spreadsheetId.hashCode(),
                spreadsheetId = spreadsheetId,
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

    override suspend fun getExhibitsForCase(caseSpreadsheetId: String): Result<List<Exhibit>> = execute { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: return@execute emptyList()
        (1..sheet.lastRowNum).mapNotNull { i ->
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(1)?.stringCellValue != caseSpreadsheetId) return@mapNotNull null

            val idCell = row.getCell(0)
            val exhibitId = getIntCellValueSafe(idCell) ?: 0
            val evidenceIdsCell = row.getCell(4)
            val evidenceIds = (evidenceIdsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }

            Exhibit(
                id = exhibitId,
                caseId = caseSpreadsheetId.hashCode().toLong(),
                name = row.getCell(2)?.stringCellValue ?: "",
                description = row.getCell(3)?.stringCellValue ?: "",
                evidenceIds = evidenceIds
            )
        }
    }

    override suspend fun addExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Exhibit> = execute { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: workbook.createSheet(EXHIBITS_SHEET_NAME).also {
            it.createRow(0).apply { EXHIBITS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val lastId = (1..sheet.lastRowNum).mapNotNull { i ->
            getIntCellValueSafe(sheet.getRow(i)?.getCell(0))
        }.maxOrNull() ?: 0
        val newExhibit = exhibit.copy(id = lastId + 1)
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue(newExhibit.id.toDouble())
            createCell(1).setCellValue(caseSpreadsheetId)
            createCell(2).setCellValue(newExhibit.name)
            createCell(3).setCellValue(newExhibit.description)
            createCell(4).setCellValue(newExhibit.evidenceIds.joinToString(","))
        }
        newExhibit
    }

    override suspend fun updateExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: throw IOException("Exhibits sheet not found.")
        val row = findRowById(sheet, exhibit.id, 0) ?: throw IOException("Exhibit with id ${exhibit.id} not found.")
        (row.getCell(2) ?: row.createCell(2)).setCellValue(exhibit.name)
        (row.getCell(3) ?: row.createCell(3)).setCellValue(exhibit.description)
        (row.getCell(4) ?: row.createCell(4)).setCellValue(exhibit.evidenceIds.joinToString(","))
    }

    override suspend fun deleteExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(EXHIBITS_SHEET_NAME) ?: throw IOException("Exhibits sheet not found.")
        val row = findRowById(sheet, exhibit.id, 0) ?: throw IOException("Exhibit with id ${exhibit.id} not found.")
        sheet.removeRow(row)
        if (row.rowNum < sheet.lastRowNum) {
            sheet.shiftRows(row.rowNum + 1, sheet.lastRowNum, -1)
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
        (row.getCell(1) ?: row.createCell(1)).setCellValue(case.name)
        (row.getCell(2) ?: row.createCell(2)).setCellValue(case.plaintiffs)
        (row.getCell(3) ?: row.createCell(3)).setCellValue(case.defendants)
        (row.getCell(4) ?: row.createCell(4)).setCellValue(case.court)
        (row.getCell(5) ?: row.createCell(5)).setCellValue(case.folderId)
        (row.getCell(6) ?: row.createCell(6)).setCellValue(System.currentTimeMillis().toDouble())
        (row.getCell(7) ?: row.createCell(7)).setCellValue(case.isArchived)
    }

    override suspend fun deleteCase(case: Case): Result<Unit> = execute { workbook ->
        val sheet = workbook.getSheet(CASES_SHEET_NAME) ?: throw IOException("Cases sheet not found.")
        val row = findRowById(sheet, case.spreadsheetId, 0) ?: throw IOException("Case with id ${case.spreadsheetId} not found.")
        sheet.removeRow(row)
        if (row.rowNum < sheet.lastRowNum) {
            sheet.shiftRows(row.rowNum + 1, sheet.lastRowNum, -1)
        }
    }

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

            val timestampCell = row.getCell(6)
            val timestamp = getLongCellValueSafe(timestampCell) ?: 0L

            val documentDateCell = row.getCell(8)
            val documentDate = getLongCellValueSafe(documentDateCell) ?: 0L

            val allegationIdCell = row.getCell(9)
            val allegationIdInt = getIntCellValueSafe(allegationIdCell)

            val tagsCell = row.getCell(11)
            val tags = (tagsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }

            val linkedIdsCell = row.getCell(13)
            val linkedIds = (linkedIdsCell?.stringCellValue ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }

            val entitiesCell = row.getCell(15)
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
                allegationId = allegationIdInt?.toString(),
                allegationElementName = null,
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
        val lastId = (1..sheet.lastRowNum).mapNotNull { i ->
            getIntCellValueSafe(sheet.getRow(i)?.getCell(0))
        }.maxOrNull() ?: 0
        val newEvidence = evidence.copy(id = lastId + 1)
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
            newEvidence.allegationId?.let {
                try {
                    createCell(9).setCellValue(it.toDouble())
                } catch (e: NumberFormatException) {
                    createCell(9).setCellValue(it)
                }
            } ?: createCell(9).setBlank()
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
        (row.getCell(2) ?: row.createCell(2)).setCellValue(evidence.type)
        (row.getCell(3) ?: row.createCell(3)).setCellValue(evidence.content)
        (row.getCell(4) ?: row.createCell(4)).setCellValue(evidence.formattedContent)
        (row.getCell(5) ?: row.createCell(5)).setCellValue(evidence.mediaUri)
        (row.getCell(6) ?: row.createCell(6)).setCellValue(evidence.timestamp.toDouble())
        (row.getCell(7) ?: row.createCell(7)).setCellValue(evidence.sourceDocument)
        (row.getCell(8) ?: row.createCell(8)).setCellValue(evidence.documentDate.toDouble())
        val allegationCell = row.getCell(9) ?: row.createCell(9)
        evidence.allegationId?.let {
            try {
                 allegationCell.setCellValue(it.toDouble())
            } catch (e: NumberFormatException) {
                allegationCell.setCellValue(it)
            }
        } ?: allegationCell.setBlank()
        (row.getCell(10) ?: row.createCell(10)).setCellValue(evidence.category)
        (row.getCell(11) ?: row.createCell(11)).setCellValue(evidence.tags.joinToString(","))
        (row.getCell(12) ?: row.createCell(12)).setCellValue(evidence.commentary ?: "")
        (row.getCell(13) ?: row.createCell(13)).setCellValue(evidence.linkedEvidenceIds.joinToString(","))
        (row.getCell(14) ?: row.createCell(14)).setCellValue(evidence.parentVideoId ?: "")
        (row.getCell(15) ?: row.createCell(15)).setCellValue(gson.toJson(evidence.entities))
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

    override suspend fun getAllegationsForCase(caseSpreadsheetId: String): Result<List<Allegation>> = execute { workbook ->
        val sheet = workbook.getSheet(ALLEGATIONS_SHEET_NAME) ?: return@execute emptyList()
        (1..sheet.lastRowNum).mapNotNull { i ->
            val row = sheet.getRow(i) ?: return@mapNotNull null
            if (row.getCell(1)?.stringCellValue != caseSpreadsheetId) return@mapNotNull null
            val allegationId = getIntCellValueSafe(row.getCell(0)) ?: 0
            val masterAllegation = AllegationProvider.getAllegationById(allegationId)
            masterAllegation?.copy(
                spreadsheetId = caseSpreadsheetId,
                text = row.getCell(2)?.stringCellValue ?: masterAllegation.text
            ) ?: Allegation(
                id = allegationId,
                spreadsheetId = caseSpreadsheetId,
                text = row.getCell(2)?.stringCellValue ?: ""
            )
        }
    }

    override suspend fun addAllegation(caseSpreadsheetId: String, allegation: Allegation): Result<Allegation> = execute { workbook ->
        val sheet = workbook.getSheet(ALLEGATIONS_SHEET_NAME) ?: workbook.createSheet(ALLEGATIONS_SHEET_NAME).also {
            it.createRow(0).apply { ALLEGATIONS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        val lastId = (1..sheet.lastRowNum).mapNotNull { i ->
            getIntCellValueSafe(sheet.getRow(i)?.getCell(0))
        }.maxOrNull() ?: 0
        val newAllegation = allegation.copy(id = lastId + 1)
        sheet.createRow(sheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue(newAllegation.id.toDouble())
            createCell(1).setCellValue(caseSpreadsheetId)
            createCell(2).setCellValue(newAllegation.text)
        }
        newAllegation
    }

    override suspend fun updateTranscript(evidence: Evidence, newTranscript: String, reason: String): Result<Unit> = execute { workbook ->
        val evidenceSheet = workbook.getSheet(EVIDENCE_SHEET_NAME) ?: throw IOException("Evidence sheet not found.")
        val evidenceRow = findRowById(evidenceSheet, evidence.id, 0) ?: throw IOException("Evidence with id ${evidence.id} not found.")

        evidenceRow.getCell(3)?.setCellValue(newTranscript)

        val editsSheet = workbook.getSheet(TRANSCRIPT_EDITS_SHEET_NAME) ?: workbook.createSheet(TRANSCRIPT_EDITS_SHEET_NAME).also {
            it.createRow(0).apply { TRANSCRIPT_EDITS_HEADER.forEachIndexed { index, s -> createCell(index).setCellValue(s) } }
        }
        editsSheet.createRow(editsSheet.physicalNumberOfRows).apply {
            createCell(0).setCellValue((editsSheet.physicalNumberOfRows).toDouble())
            createCell(1).setCellValue(evidence.id.toDouble())
            createCell(2).setCellValue(System.currentTimeMillis().toDouble())
            createCell(3).setCellValue(reason)
            createCell(4).setCellValue(newTranscript)
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
            Result.Success(Unit)
        }
    }
}