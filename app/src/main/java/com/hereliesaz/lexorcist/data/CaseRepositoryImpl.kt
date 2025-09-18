package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.utils.ErrorReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val storageService: StorageService,
    private val settingsManager: SettingsManager, // Keep if used, remove if not
    private val errorReporter: ErrorReporter,
    private val caseSheetParser: CaseSheetParser,
    private val googleApiService: GoogleApiService
) : CaseRepository {

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    override val cases: Flow<List<Case>> = _cases.asStateFlow()

    private val _selectedCase = MutableStateFlow<Case?>(null)
    override val selectedCase: Flow<Case?> = _selectedCase.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    // Assuming Allegation handling might also benefit from similar caching if it comes from StorageService

    // Call refreshCases() from ViewModel init or appropriate lifecycle event
    override suspend fun refreshCases() {
        when (val result = storageService.getAllCases()) { // Assumes getAllCases returns Result<List<Case>>
            is Result.Success -> {
                _cases.value = result.data.sortedByDescending { it.lastModifiedTime ?: 0L }
            }
            is Result.Error -> {
                errorReporter.reportError(result.exception)
                // Optionally, inform UI about the error
            }
            is Result.UserRecoverableError -> {
                errorReporter.reportError(result.exception)
                // Optionally, inform UI about the error
            }
        }
    }

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return _cases.value.find { it.spreadsheetId == spreadsheetId }
    }

    override suspend fun selectCase(case: Case?) {
        _selectedCase.value = case
        // If selecting a case should also load its specific details like allegations:
        // case?.spreadsheetId?.let { refreshAllegations(0, it) } // Consider caseId usage
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
    ): Result<Case> { // Changed to return Result<Case>
        val caseDetails = Case(
            name = caseName,
            spreadsheetId = "", // Should be set by storageService or be part of its return
            plaintiffs = plaintiffs,
            defendants = defendants,
            court = court
            // Initialize other fields as necessary
        )

        // Assuming storageService.createCase now returns Result<Case>
        return when (val creationResult = storageService.createCase(caseDetails)) {
            is Result.Success -> {
                val createdCase = creationResult.data // The Case object confirmed by storage
                _cases.update {
                    (it + createdCase).sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                Result.Success(createdCase)
            }
            is Result.Error -> {
                errorReporter.reportError(creationResult.exception)
                Result.Error(creationResult.exception) // Propagate specific error
            }
            is Result.UserRecoverableError -> {
                errorReporter.reportError(creationResult.exception)
                // It's generally better to map UserRecoverableError to a specific type or handle it before this point
                // For now, propagating as an error for simplicity in the repository layer.
                Result.Error(creationResult.exception) // Or map to a specific error type
            }
        }
    }

    override suspend fun archiveCase(case: Case): Result<Case> {
        val archivedDetails = case.copy(isArchived = true)
        // Assuming storageService.updateCase returns Result<Case>
        return when (val updateResult = storageService.updateCase(archivedDetails)) {
            is Result.Success -> {
                val updatedCase = updateResult.data
                _cases.update {
                    it.map { c -> if (c.spreadsheetId == updatedCase.spreadsheetId) updatedCase else c }
                        .sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                Result.Success(updatedCase)
            }
            is Result.Error -> Result.Error(updateResult.exception)
            is Result.UserRecoverableError -> Result.Error(updateResult.exception) // Or map
        }
    }

    override suspend fun deleteCase(case: Case): Result<Unit> {
        return when (val deleteResult = storageService.deleteCase(case)) {
            is Result.Success -> {
                _cases.update {
                    it.filterNot { c -> c.spreadsheetId == case.spreadsheetId }
                        .sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(deleteResult.exception)
            is Result.UserRecoverableError -> Result.Error(deleteResult.exception) // Or map
        }
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val sheetData = googleApiService.readSpreadsheet(spreadsheetId)
        if (sheetData.isNullOrEmpty()) {
            return null
        }
        val parsedData = caseSheetParser.parseCaseFromData(spreadsheetId, sheetData)
        if (parsedData != null) {
            val (newCase, evidenceList) = parsedData
            // Assuming storageService.createCase returns Result<Case>
            val createResult = storageService.createCase(newCase)
            if (createResult is Result.Success) {
                val createdCase = createResult.data
                // TODO: Handle evidenceList - this might involve another storageService call and cache update
                // evidenceList.forEach { storageService.addEvidence(createdCase.spreadsheetId, it) }
                _cases.update {
                    (it + createdCase).sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                return createdCase
            } else if (createResult is Result.Error) {
                errorReporter.reportError(createResult.exception)
            } else if (createResult is Result.UserRecoverableError) {
                errorReporter.reportError(createResult.exception)
            }
        }
        return null
    }

    override suspend fun synchronize() {
        // Perform synchronization logic using storageService
        // This might involve fetching remote changes and merging them
        // For now, assume it's complex and might necessitate a full refresh after completion
        storageService.synchronize() // Assuming this handles the core sync
        refreshCases() // After sync, refresh the entire cache from the (potentially updated) spreadsheet
    }

    override suspend fun clearCache() {
        _cases.value = emptyList()
        _selectedCase.value = null
        _allegations.value = emptyList()
        // Any other local caches should be cleared here
    }

    // --- Allegation Methods --- (Example: could follow similar caching pattern)
    override fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> {
        // If allegations are fetched from storageService and cached:
        // return _allegations.asStateFlow().map { list -> list.filter { it.spreadsheetId == spreadsheetId } }
        return _allegations.asStateFlow() // Simpler: assumes refreshAllegations populates correctly for the selected case
    }

    override suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) {
        when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
            is Result.Success -> _allegations.value = result.data // Potentially filter or manage per caseId if _allegations is global
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        val allegationDetails = Allegation(spreadsheetId = spreadsheetId, text = allegationText)
        // Assuming storageService.addAllegation might return the created Allegation
        // storageService.addAllegation(spreadsheetId, allegationDetails)
        // Then update _allegations cache if successful, similar to cases
        refreshAllegations(0, spreadsheetId) // Fallback to refresh for now
    }

    // --- Other methods from CaseRepository interface ---
    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> {
        return emptyFlow() // Placeholder
    }

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        // Placeholder
    }

    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {
        // Placeholder
    }

    override suspend fun getEvidenceForCase(spreadsheetId: String): Result<List<Evidence>> {
        return storageService.getEvidenceForCase(spreadsheetId) // Assumed to be direct passthrough for now
    }

    override fun getHtmlTemplates(): Flow<List<DriveFile>> {
        return emptyFlow() // Placeholder
    }

    override suspend fun refreshHtmlTemplates() {
        // Placeholder
    }
}
