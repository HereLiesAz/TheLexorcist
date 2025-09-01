package com.hereliesaz.lexorcist.data

import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.SheetFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val settingsManager: SettingsManager,
    private val errorReporter: com.hereliesaz.lexorcist.utils.ErrorReporter
) : CaseRepository {

    private val _cases = kotlinx.coroutines.flow.MutableStateFlow<List<Case>>(emptyList())

    override fun getAllCases(): Flow<List<Case>> = _cases.asStateFlow()

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return _cases.value.find { it.spreadsheetId == spreadsheetId }
    }

    override suspend fun refreshCases() {
        try {
            val appRootFolderId = googleApiService.getOrCreateAppRootFolder()
            val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
            _cases.value = googleApiService.getAllCasesFromRegistry(registryId)
        } catch (e: java.io.IOException) {
            errorReporter.reportError(e)
        }
    }

    override suspend fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
        plaintiffs: String,
        defendants: String,
        court: String
    ) {
        try {
            val appRootFolderId = googleApiService.getOrCreateAppRootFolder()
            val caseFolderId = googleApiService.getOrCreateCaseFolder(caseName) ?: return
            val spreadsheetResult = googleApiService.createSpreadsheet(caseName, caseFolderId)

            if (spreadsheetResult is com.hereliesaz.lexorcist.utils.Result.Success) {
                val spreadsheetId = spreadsheetResult.data ?: return
                val newCase = Case(
                    id = 0, // ID will be assigned by registry
                    name = caseName,
                    spreadsheetId = spreadsheetId,
                    folderId = caseFolderId,
                    plaintiffs = plaintiffs,
                    defendants = defendants,
                    court = court,
                    lastModifiedTime = System.currentTimeMillis()
                )
                val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
                googleApiService.addCaseToRegistry(registryId, newCase)
                refreshCases()
            } else {
                // Handle error
            }
        } catch (e: java.io.IOException) {
            errorReporter.reportError(e)
        }
    }

    override suspend fun archiveCase(case: Case) {
        // TODO: Implement actual logic
    }

    override suspend fun deleteCase(case: Case) {
        // TODO: Implement actual logic
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> {
        // TODO: Implement actual logic
        return emptyFlow()
    }

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        // TODO: Implement actual logic
    }

    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {
        // TODO: Implement actual logic
    }

    override fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> {
        // TODO: Implement actual logic
        return emptyFlow()
    }

    override suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) {
        // TODO: Implement actual logic
    }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        // TODO: Implement actual logic
    }

    override fun getHtmlTemplates(): Flow<List<DriveFile>> {
        // TODO: Implement actual logic
        return emptyFlow()
    }

    override suspend fun refreshHtmlTemplates() {
        // TODO: Implement actual logic
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        // TODO: Implement actual logic
        return null
    }
}
