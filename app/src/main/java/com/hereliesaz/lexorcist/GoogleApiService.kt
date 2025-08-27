package com.hereliesaz.lexorcist

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.Drive
import com.google.api.services.script.Script
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleApiService(
    private val credential: GoogleAccountCredential,
    private val sheetsService: Sheets,
    private val driveService: Drive,
    private val scriptService: Script
) {

    // ... (existing functions)

    suspend fun readSpreadsheet(spreadsheetId: String): Map<String, List<List<Any>>>? {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
                val sheetData = mutableMapOf<String, List<List<Any>>>()
                spreadsheet.sheets.forEach { sheet ->
                    val range = sheet.properties.title
                    val response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
                    sheetData[range] = response.getValues() ?: emptyList()
                }
                sheetData
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun createSpreadsheet(title: String, caseInfo: Map<String, String>): String? {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = Spreadsheet().setProperties(
                    SpreadsheetProperties().setTitle(title)
                )
                val createdSpreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute()
                val spreadsheetId = createdSpreadsheet.spreadsheetId

                // Now add the case info to a sheet
                val sheetName = "Case Info"
                val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle(sheetName))
                val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()

                val values = caseInfo.map { (key, value) ->
                    listOf(key, value)
                }
                val body = ValueRange().setValues(values)
                sheetsService.spreadsheets().values().update(spreadsheetId, "$sheetName!A1", body)
                    .setValueInputOption("RAW")
                    .execute()

                spreadsheetId
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun addSheet(spreadsheetId: String, sheetTitle: String) {
        withContext(Dispatchers.IO) {
            try {
                val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle(sheetTitle))
                val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun appendData(spreadsheetId: String, sheetName: String, values: List<List<Any>>) {
        withContext(Dispatchers.IO) {
            try {
                val body = ValueRange().setValues(values)
                sheetsService.spreadsheets().values().append(spreadsheetId, sheetName, body)
                    .setValueInputOption("RAW")
                    .execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun createMasterTemplate(): String? {
        // ... (implementation)
        return null
    }

    suspend fun attachScript(spreadsheetId: String, masterTemplateId: String) {
        // ... (implementation)
    }
}