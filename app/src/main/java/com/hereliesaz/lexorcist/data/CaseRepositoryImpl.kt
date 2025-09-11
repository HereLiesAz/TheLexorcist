package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.service.GoogleApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

@Singleton
class CaseRepositoryImpl
    @Inject
    constructor(
        private val credentialHolder: com.hereliesaz.lexorcist.auth.CredentialHolder,
        private val settingsManager: SettingsManager,
        private val errorReporter: com.hereliesaz.lexorcist.utils.ErrorReporter,
    ) : CaseRepository {
        private val _cases = kotlinx.coroutines.flow.MutableStateFlow<List<Case>>(emptyList())

    /**
     * A flow of the list of cases.
     * This is the implementation of the [CaseRepository.cases] property.
     */
    override val cases: Flow<List<Case>> = _cases.asStateFlow()

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? = _cases.value.find { it.spreadsheetId == spreadsheetId }

        override suspend fun refreshCases() {
            android.util.Log.d("CaseRepositoryImpl", "refreshCases called")
            val googleApiService = credentialHolder.googleApiService
            try {
                googleApiService?.let { service ->
                    // Null check
                    val appRootFolderId = service.getOrCreateAppRootFolder()
                    val registryId = service.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
                    val cases = service.getAllCasesFromRegistry(registryId)
                    android.util.Log.d("CaseRepositoryImpl", "Found ${cases.size} cases in registry")
                    _cases.value = cases
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
            court: String,
        ) {
            android.util.Log.d("CaseRepositoryImpl", "createCase called with name: $caseName")
            val googleApiService = credentialHolder.googleApiService
            try {
                googleApiService?.let { service ->
                    // Null check
                    android.util.Log.d("CaseRepositoryImpl", "GoogleApiService is not null")
                    val appRootFolderId = service.getOrCreateAppRootFolder()
                    val caseFolderId =
                        service.getOrCreateCaseFolder(caseName) ?: run {
                            errorReporter.reportError(Exception("Failed to create or get case folder in createCase"))
                            return
                        }
                    val spreadsheetResult = service.createSpreadsheet(caseName, caseFolderId)

                if (spreadsheetResult is com.hereliesaz.lexorcist.utils.Result.Success) {
                    val spreadsheetId =
                        spreadsheetResult.data ?: run {
                            errorReporter.reportError(Exception("Spreadsheet ID is null after creation in createCase"))
                            return
                        }
                    android.util.Log.d("CaseRepositoryImpl", "Spreadsheet created with id: $spreadsheetId")
                    val newCase =
                        Case(
                            // ID will be assigned by registry
                            id = 0,
                            name = caseName,
                            spreadsheetId = spreadsheetId,
                            folderId = caseFolderId,
                            plaintiffs = plaintiffs,
                            defendants = defendants,
                            court = court,
                            lastModifiedTime = System.currentTimeMillis(),
                        )
                    val registryId = service.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
                    service.addCaseToRegistry(registryId, newCase)
                    refreshCases() // This will also need googleApiService
                } else {
                    val error = (spreadsheetResult as com.hereliesaz.lexorcist.utils.Result.Error).exception
                    errorReporter.reportError(error ?: Exception("Unknown error creating spreadsheet in createCase"))
                    android.util.Log.e("CaseRepositoryImpl", "Error creating spreadsheet: $error")
                }
            } ?: errorReporter.reportError(Exception("GoogleApiService not available in createCase"))
        } catch (e: java.io.IOException) {
            errorReporter.reportError(e)
            android.util.Log.e("CaseRepositoryImpl", "IOException in createCase: $e")
        }
    }

        override suspend fun archiveCase(case: Case) {
            credentialHolder.googleApiService?.let {
                val archivedCase = case.copy(isArchived = true)
                if (it.updateCaseInRegistry(archivedCase)) {
                    refreshCases()
                }
            }
        }

        override suspend fun deleteCase(case: Case) {
            credentialHolder.googleApiService?.let {
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

    override suspend fun addSheetFilter(
        spreadsheetId: String,
        name: String,
        value: String,
    ) {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override fun getAllegations(
        caseId: Int,
        spreadsheetId: String,
    ): Flow<List<Allegation>> {
        // TODO: Implement actual logic (with null safety for googleApiService)
        return emptyFlow()
    }

    override suspend fun refreshAllegations(
        caseId: Int,
        spreadsheetId: String,
    ) {
        // TODO: Implement actual logic (with null safety for googleApiService)
    }

    override suspend fun addAllegation(
        spreadsheetId: String,
        allegationText: String,
    ) {
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
