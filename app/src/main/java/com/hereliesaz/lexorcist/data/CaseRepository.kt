package com.hereliesaz.lexorcist.data

import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.model.SheetFilter
import kotlinx.coroutines.flow.Flow

interface CaseRepository {
    fun setGoogleApiService(googleApiService: com.hereliesaz.lexorcist.GoogleApiService?)
    fun getCases(): Flow<List<Case>>
    suspend fun getCaseById(id: Int): Case?
    suspend fun refreshCases()
    suspend fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
        plaintiffs: String,
        defendants: String,
        court: String
    )
    suspend fun archiveCase(case: Case)
    suspend fun deleteCase(case: Case)
    fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>>
    suspend fun refreshSheetFilters(spreadsheetId: String)
    suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String)
    fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>>
    suspend fun refreshAllegations(caseId: Int, spreadsheetId: String)
    suspend fun addAllegation(spreadsheetId: String, allegationText: String)
    fun getHtmlTemplates(): Flow<List<DriveFile>>
    suspend fun refreshHtmlTemplates()
    suspend fun importSpreadsheet(spreadsheetId: String): Case?
}
