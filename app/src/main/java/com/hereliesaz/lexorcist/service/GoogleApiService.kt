package com.hereliesaz.lexorcist.service

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.model.File
import com.google.api.services.script.Script
import com.google.api.services.script.model.Content
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.services.sheets.v4.model.SpreadsheetProperties

class GoogleApiService(credential: GoogleAccountCredential) {

    private val scriptService: Script = Script.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("Lexorcist")
        .build()
    private val sheetsService: Sheets = Sheets.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("Lexorcist")
        .build()

    fun getScript(scriptId: String): String? {
        val project = scriptService.projects().get(scriptId).execute()
        val scriptFile = project.files.find { it.type == "SERVER_JS" }
        return scriptFile?.source
    }

    fun updateScript(scriptId: String, scriptContent: String) {
        val newFile = File().setSource(scriptContent).setName("Code")
        val content = Content().setFiles(listOf(newFile))
        scriptService.projects().updateContent(scriptId, content).execute()
    }

    fun createSpreadsheet(title: String): String? {
        val spreadsheet = Spreadsheet().setProperties(
            SpreadsheetProperties().setTitle(title)
        )
        val createdSpreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute()
        return createdSpreadsheet.spreadsheetId
    }

    fun writeData(spreadsheetId: String, data: List<List<Any>>) {
        val valueRange = ValueRange().setValues(data)
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, "A1", valueRange)
            .setValueInputOption("RAW")
            .execute()
    }
}
