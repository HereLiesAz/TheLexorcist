package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.utils.ErrorReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

@Singleton
class CaseRepositoryImpl
@Inject
constructor(
    private val storageService: StorageService, // Injected StorageService
    private val settingsManager: SettingsManager,
    private val errorReporter: ErrorReporter,
) : CaseRepository {
    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    private val _selectedCase = MutableStateFlow<Case?>(null)

    override val cases: Flow<List<Case>> = _cases.asStateFlow()
    override val selectedCase: Flow<Case?> = _selectedCase.asStateFlow()

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? =
        _cases.value.find { it.spreadsheetId == spreadsheetId }

    override suspend fun selectCase(case: Case?) {
        _selectedCase.value = case
    }

    override suspend fun refreshCases() {
        android.util.Log.d("CaseRepositoryImpl", "refreshCases called")
        when (val result = storageService.getAllCases()) {
            is Result.Success -> {
                android.util.Log.d("CaseRepositoryImpl", "Found ${result.data.size} cases")
                _cases.value = result.data
            }
            is Result.Error -> {
                errorReporter.reportError(result.exception)
                _cases.value = emptyList() // Clear cases on error
            }
            is Result.UserRecoverableError -> {
                // Not applicable for local storage, but handle for completeness
                errorReporter.reportError(result.exception)
            }
        }
    }

    override suspend fun createCase(
        caseName: String,
        exhibitSheetName: String, // This is not used in local storage model
        caseNumber: String, // This is not part of Case data class, ignored for now
        caseSection: String, // This is not part of Case data class, ignored for now
        caseJudge: String, // This is not part of Case data class, ignored for now
        plaintiffs: String,
        defendants: String,
        court: String,
    ): Result<Unit> {
        android.util.Log.d("CaseRepositoryImpl", "createCase called with name: $caseName")

        // Create a Case object from the parameters.
        // id, spreadsheetId, folderId, lastModifiedTime will be set by the storage service.
        val newCase = Case(
            id = 0,
            name = caseName,
            spreadsheetId = "", // To be filled by storage service
            folderId = null, // Not relevant for local file storage
            plaintiffs = plaintiffs,
            defendants = defendants,
            court = court,
            lastModifiedTime = 0,
            isArchived = false
        )

        return when (val result = storageService.createCase(newCase)) {
            is Result.Success -> {
                android.util.Log.d("CaseRepositoryImpl", "Case created successfully with id: ${result.data.spreadsheetId}")
                refreshCases() // Refresh the list to include the new case
                Result.Success(Unit)
            }
            is Result.Error -> {
                errorReporter.reportError(result.exception)
                android.util.Log.e("CaseRepositoryImpl", "Error creating case: ${result.exception}")
                Result.Error(result.exception)
            }
            is Result.UserRecoverableError -> {
                // Not applicable for local storage, but handle for completeness
                errorReporter.reportError(result.exception)
                result
            }
        }
    }

    override suspend fun archiveCase(case: Case) {
        val archivedCase = case.copy(isArchived = true)
        when (val result = storageService.updateCase(archivedCase)) {
            is Result.Success -> refreshCases()
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override suspend fun deleteCase(case: Case) {
        when (val result = storageService.deleteCase(case)) {
            is Result.Success -> refreshCases()
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> {
        // TODO: Implement actual logic
        return emptyFlow()
    }

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        // TODO: Implement actual logic
    }

    override suspend fun addSheetFilter(
        spreadsheetId: String,
        name: String,
        value: String,
    ) {
        // TODO: Implement actual logic
    }

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())

    override fun getAllegations(
        caseId: Int,
        spreadsheetId: String,
    ): Flow<List<Allegation>> {
        return _allegations.asStateFlow()
    }

    override suspend fun refreshAllegations(
        caseId: Int,
        spreadsheetId: String,
    ) {
        when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
            is Result.Success -> _allegations.value = result.data
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override suspend fun addAllegation(
        spreadsheetId: String,
        allegationText: String,
    ) {
        val allegation = Allegation(spreadsheetId = spreadsheetId, text = allegationText)
        storageService.addAllegation(spreadsheetId, allegation)
        refreshAllegations(0, spreadsheetId) // caseId is not used in refreshAllegations
    }

    override suspend fun getEvidenceForCase(spreadsheetId: String): Result<List<Evidence>> {
        return storageService.getEvidenceForCase(spreadsheetId)
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

    override suspend fun clearCache() {
        _cases.value = emptyList()
    }
}
