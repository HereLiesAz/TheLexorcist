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
    // Changed to nullable
    private val googleApiService: GoogleApiService?,
    private val settingsManager: SettingsManager,
    private val errorReporter: com.hereliesaz.lexorcist.utils.ErrorReporter
) : CaseRepository {

    private val _cases = kotlinx.coroutines.flow.MutableStateFlow<List<Case>>(emptyList())
    /**
     * A flow of the list of cases.
     * This is the implementation of the [CaseRepository.cases] property.
     */
    override val cases: Flow<List<Case>> = _cases.asStateFlow()

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return _cases.value.find { it.spreadsheetId == spreadsheetId }
    }

    override suspend fun refreshCases() {
        try {
            googleApiService?.let { service -> // Null check
                val appRootFolderId = service.getOrCreateAppRootFolder()
                val registryId = service.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
                _cases.value = service.getAllCasesFromRegistry(registryId)
            } ?: errorReporter.reportError(Exception("GoogleApiService not available in refreshCases"))
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
            googleApiService?.let { service -> // Null check
                val appRootFolderId = service.getOrCreateAppRootFolder()
                val caseFolderId = service.getOrCreateCaseFolder(caseName) ?: run {
                    errorReporter.reportError(Exception("Failed to create or get case folder in createCase"))
                    return
                }
                val spreadsheetResult = service.createSpreadsheet(caseName, caseFolderId)

                if (spreadsheetResult is com.hereliesaz.lexorcist.utils.Result.Success) {
                    val spreadsheetId = spreadsheetResult.data ?: run {
                        errorReporter.reportError(Exception("Spreadsheet ID is null after creation in createCase"))
                        return
                    }
                    val newCase = Case(
                        // ID will be assigned by registry
                        id = 0,
                        name = caseName,
                        spreadsheetId = spreadsheetId,
                        folderId = caseFolderId,
                        plaintiffs = plaintiffs,
                        defendants = defendants,
                        court = court,
                        lastModifiedTime = System.currentTimeMillis()
                    )
                    val registryId = service.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
                    service.addCaseToRegistry(registryId, newCase)
                    refreshCases() // This will also need googleApiService
                } else {
                    val error = (spreadsheetResult as com.hereliesaz.lexorcist.utils.Result.Error).exception
                    errorReporter.reportError(error ?: Exception("Unknown error creating spreadsheet in createCase"))
                }
            } ?: errorReporter.reportError(Exception("GoogleApiService not available in createCase"))
        } catch (e: java.io.IOException) {
            errorReporter.reportError(e)
        }
    }

    override suspend fun archiveCase(case: Case) {
        googleApiService?.let {
            val archivedCase = case.copy(isArchived = true)
            if (it.updateCaseInRegistry(archivedCase)) {
                refreshCases()
            }
        }
    }

    override suspend fun deleteCase(case: Case) {
        googleApiService?.let {
            if (it.deleteCaseFromRegistry(case)) {
                case.folderId?.let { folderId -> it.deleteFolder(folderId) }
                refreshCases()
            }
        }
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> {
        // TODO: Implement actual logic (with null safety for googleApiService)
        return emptyFlow()
    }

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> {
        // TODO: Implement actual logic (with null safety for googleApiService)
        return emptyFlow()
    }

    override suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override fun getHtmlTemplates(): Flow<List<DriveFile>> {
        // TODO: Implement actual logic (with null safety for googleApiService)
        return emptyFlow()
    }

    override suspend fun refreshHtmlTemplates() {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        // TODO: Implement actual logic (with null safety for googleApiService)
        return null
    }
}
