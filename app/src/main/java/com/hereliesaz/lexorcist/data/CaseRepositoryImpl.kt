package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.SheetFilter
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
                    val appRootFolderIdResult = service.getOrCreateAppRootFolder()
                    if (appRootFolderIdResult is com.hereliesaz.lexorcist.utils.Result.Success) {
                        val registryId = service.getOrCreateCaseRegistrySpreadsheetId(appRootFolderIdResult.data)
                        val cases = service.getAllCasesFromRegistry(registryId)
                        android.util.Log.d("CaseRepositoryImpl", "Found ${cases.size} cases in registry")
                        _cases.value = cases
                    } else if (appRootFolderIdResult is com.hereliesaz.lexorcist.utils.Result.Error) {
                        errorReporter.reportError(appRootFolderIdResult.exception)
                    }
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
        ): com.hereliesaz.lexorcist.utils.Result<Unit> {
            android.util.Log.d("CaseRepositoryImpl", "createCase called with name: $caseName")
            val googleApiService = credentialHolder.googleApiService
            if (googleApiService == null) {
                errorReporter.reportError(Exception("GoogleApiService not available in createCase"))
                return com.hereliesaz.lexorcist.utils.Result.Error(Exception("GoogleApiService not available"))
            }
            try {
                val appRootFolderIdResult = googleApiService.getOrCreateAppRootFolder()
                if (appRootFolderIdResult is com.hereliesaz.lexorcist.utils.Result.Success) {
                    val appRootFolderId = appRootFolderIdResult.data
                    val caseFolderId =
                        googleApiService.getOrCreateCaseFolder(caseName) ?: run {
                            errorReporter.reportError(Exception("Failed to create or get case folder in createCase"))
                            return com.hereliesaz.lexorcist.utils.Result.Error(Exception("Failed to create or get case folder"))
                        }
                    val spreadsheetResult = googleApiService.createSpreadsheet(caseName, caseFolderId)

                    return when (spreadsheetResult) {
                        is com.hereliesaz.lexorcist.utils.Result.Success -> {
                            val spreadsheetId =
                                spreadsheetResult.data ?: run {
                                    errorReporter.reportError(Exception("Spreadsheet ID is null after creation in createCase"))
                                    return com.hereliesaz.lexorcist.utils.Result.Error(Exception("Spreadsheet ID is null after creation"))
                                }
                            android.util.Log.d("CaseRepositoryImpl", "Spreadsheet created with id: $spreadsheetId")
                            val newCase =
                                Case(
                                    id = 0,
                                    name = caseName,
                                    spreadsheetId = spreadsheetId,
                                    folderId = caseFolderId,
                                    plaintiffs = plaintiffs,
                                    defendants = defendants,
                                    court = court,
                                    lastModifiedTime = System.currentTimeMillis(),
                                )
                            val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
                            googleApiService.addCaseToRegistry(registryId, newCase)
                            refreshCases()
                            com.hereliesaz.lexorcist.utils.Result.Success(Unit)
                        }
                        is com.hereliesaz.lexorcist.utils.Result.Error -> {
                            errorReporter.reportError(spreadsheetResult.exception)
                            android.util.Log.e("CaseRepositoryImpl", "Error creating spreadsheet: ${spreadsheetResult.exception}")
                            spreadsheetResult
                        }
                        is com.hereliesaz.lexorcist.utils.Result.UserRecoverableError -> {
                            spreadsheetResult
                        }
                    }
                } else {
                    return appRootFolderIdResult as com.hereliesaz.lexorcist.utils.Result.Error
                }
            } catch (e: java.io.IOException) {
                errorReporter.reportError(e)
                android.util.Log.e("CaseRepositoryImpl", "IOException in createCase: $e")
                return com.hereliesaz.lexorcist.utils.Result.Error(e)
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

        override suspend fun clearCache() {
            _cases.value = emptyList()
        }
    }

