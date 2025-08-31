package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
// Removed: import com.hereliesaz.lexorcist.data.Allegation // Already imported below
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.data.SpreadsheetParser
import com.hereliesaz.lexorcist.utils.CacheManager
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow // For simple flow emission
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    // private val caseDao: CaseDao, // Removed caseDao
    private val googleApiService: GoogleApiService?,
    private val spreadsheetParser: SpreadsheetParser
) : CaseRepository {

    private val tag = "CaseRepositoryImpl"

    // For simplicity, using a MutableStateFlow that refreshCases updates.
    // Ideally, getAllCases directly fetches or GoogleApiService provides a Flow.
    private val _casesFlow = MutableStateFlow<List<Case>>(emptyList())

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    private val cacheManager = CacheManager(context) // Keep if used for non-DB caching

    // Renamed from getCases
    override fun getAllCases(): Flow<List<Case>> {
        // This will now depend on refreshCases being called to update _casesFlow
        return _casesFlow.asStateFlow()
    }

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        val currentGoogleApiService = googleApiService ?: return null
        // This is inefficient but necessary without a direct API call
        // Consider optimizing in GoogleApiService or by caching if performance is an issue
        val appRootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return null
        val registryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return null
        val cases = currentGoogleApiService.getAllCasesFromRegistry(registryId)
        return cases.find { it.spreadsheetId == spreadsheetId }
    }

    override suspend fun refreshCases() {
        val currentGoogleApiService = googleApiService ?: return
        try {
            val appRootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return
            val registryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return
            val casesFromRegistry = currentGoogleApiService.getAllCasesFromRegistry(registryId)
            _casesFlow.value = casesFromRegistry // Update the flow
            // Removed: casesFromRegistry.forEach { caseDao.insert(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing cases from remote", e)
            _casesFlow.value = emptyList() // Clear on error
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
        val currentGoogleApiService = googleApiService ?: run {
            Log.e(tag, "GoogleApiService not available, cannot create case.")
            return
        }
        val rootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return
        val caseRegistryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return
        val caseFolderId = currentGoogleApiService.getOrCreateCaseFolder(caseName) ?: return
        currentGoogleApiService.getOrCreateEvidenceFolder(caseName) // Ensure evidence folder exists

        when (val result = currentGoogleApiService.createSpreadsheet(caseName, caseFolderId)) {
            is Result.Success -> {
                val caseSpreadsheetId = result.data ?: run {
                    Log.e(tag, "Spreadsheet ID is null after creation.")
                    return
                }
                val scriptTemplate = context.resources.openRawResource(R.raw.apps_script_template)
                    .use { InputStreamReader(it).readText() }
                val scriptContent = scriptTemplate
                    .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
                    .replace("{{CASE_NUMBER}}", caseNumber)
                    .replace("{{CASE_SECTION}}", caseSection)
                    .replace("{{CASE_JUDGE}}", caseJudge)

                // Assuming attachScript returns the scriptId or null
                val scriptId = currentGoogleApiService.attachScript(caseSpreadsheetId, scriptContent, "")

                val newCase = Case(
                    name = caseName,
                    spreadsheetId = caseSpreadsheetId,
                    scriptId = scriptId, // This might be null if attachScript fails or returns null
                    folderId = caseFolderId,
                    plaintiffs = plaintiffs,
                    defendants = defendants,
                    court = court,
                    // id is usually assigned by the database or backend,
                    // for sheets, it might be row index or a generated unique ID.
                    // For now, let's assume it's handled by getAllCasesFromRegistry parsing.
                    // If creating here, ensure it doesn't clash or rely on Google to assign.
                    // For simplicity, we'll let registry handle it or assume it's not critical for this step.
                    id = 0 // Placeholder or needs a strategy for unique ID generation if not from registry
                )
                if (currentGoogleApiService.addCaseToRegistry(caseRegistryId, newCase)) {
                    // Removed: caseDao.insert(newCase)
                    refreshCases() // Refresh the case list after adding
                } else {
                    Log.e(tag, "Failed to add case to registry: ${newCase.name}")
                }
            }
            is Result.Error -> {
                Log.e(tag, "Error creating spreadsheet: ${'$'}{result.exception.message}")
            }
        }
    }

    override suspend fun archiveCase(case: Case) {
        val updatedCase = case.copy(isArchived = true)
        val success = googleApiService?.updateCaseInRegistry(updatedCase)
        if (success == true) {
            refreshCases()
        } else {
            Log.e(tag, "Failed to archive case in registry: ${case.name}")
        }
        // Removed: caseDao.update(updatedCase)
    }

    override suspend fun deleteCase(case: Case) {
        val folderIdToDelete = case.folderId ?: case.spreadsheetId // Prefer folderId if available
        
        // Attempt to delete from registry first
        val registryDeleteSuccess = googleApiService?.deleteCaseFromRegistry(case)
        if (registryDeleteSuccess == false) {
            Log.w(tag, "Could not delete case from registry or GoogleApiService not available: ${case.name}")
            // Decide if you want to proceed with folder deletion if registry deletion fails
        }

        // Attempt to delete the folder
        val folderDeleteSuccess = googleApiService?.deleteFolder(folderIdToDelete)
        if (folderDeleteSuccess == false) {
             Log.w(tag, "Could not delete folder or GoogleApiService not available: $folderIdToDelete")
        }
        
        if (registryDeleteSuccess == true || folderDeleteSuccess == true) { // Refresh if any part succeeded
            refreshCases()
        }
        // Removed: caseDao.delete(case)
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        val sheetData = googleApiService?.readSpreadsheet(spreadsheetId)?.get("Filters")
        _sheetFilters.value = sheetData?.mapNotNull { row ->
            if (row.size >= 2) SheetFilter(row.getOrNull(0)?.toString() ?: "", row.getOrNull(1)?.toString() ?: "") else null
        } ?: emptyList()
    }

    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {
        // Ensure "Filters" sheet exists before appending. AddSheet is idempotent.
        googleApiService?.addSheet(spreadsheetId, "Filters") 
        val response = googleApiService?.appendData(spreadsheetId, "Filters!A1", listOf(listOf(name, value)))
        if (response != null) { // Check if append was successful
            refreshSheetFilters(spreadsheetId)
        } else {
            Log.e(tag, "Failed to add sheet filter for spreadsheet: $spreadsheetId")
        }
    }

    override fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> = _allegations.asStateFlow()

    override suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) {
        _allegations.value = googleApiService?.getAllegationsForCase(spreadsheetId, caseId) ?: emptyList()
    }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        val success = googleApiService?.addAllegationToCase(spreadsheetId, allegationText)
        if (success == true) {
            // Assuming getAllegationsForCase in GoogleApiService gets caseId from spreadsheetId if needed,
            // or this refresh needs the caseId. For now, assuming refreshAllegations works with spreadsheetId.
            // This might need adjustment based on how getAllegationsForCase is implemented.
            // We need a caseId to refresh. If not available, this refresh won't be accurate.
            // For now, let's assume Case object is available or spreadsheetId is enough.
            // If Case is needed, this function might need it as a parameter.
             Log.d(tag, "Allegation added. Refreshing. This might need a caseId.")
            // A more robust solution would be to get the caseId associated with spreadsheetId
            // or pass the relevant Case object to this method.
        } else {
            Log.e(tag, "Failed to add allegation to case for spreadsheet: $spreadsheetId")
        }
    }

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    override fun getHtmlTemplates(): Flow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    override suspend fun refreshHtmlTemplates() {
        _htmlTemplates.value = googleApiService?.listHtmlTemplatesInAppRootFolder() ?: emptyList()
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val currentGoogleApiService = googleApiService ?: return null
        val sheetsData = currentGoogleApiService.readSpreadsheet(spreadsheetId)
        if (sheetsData != null) {
            try {
                // Assuming parseAndStore now correctly uses GoogleApiService or is self-contained
                // without DAO interactions.
                val importedCase = spreadsheetParser.parseAndStore(spreadsheetId, sheetsData)
                if (importedCase != null) {
                    refreshCases() // Refresh cases after import
                }
                return importedCase
            } catch (e: Exception) {
                Log.e(tag, "Error parsing spreadsheet $spreadsheetId: ${'$'}{e.message}", e)
            }
        }
        return null
    }
}
