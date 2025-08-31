package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.Allegation
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import com.hereliesaz.lexorcist.SpreadsheetParser
import com.hereliesaz.lexorcist.util.CacheManager
import com.hereliesaz.lexorcist.util.Result
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val applicationContext: Context,
    private val caseDao: CaseDao,
    private var googleApiService: GoogleApiService?
) : CaseRepository {

    fun setGoogleApiService(service: GoogleApiService?) { // No override
        this.googleApiService = service
    }

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    private val cacheManager = CacheManager(applicationContext)
    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())

    // --- CaseRepository Interface Implementation ---

    override suspend fun createCase(case: Case): Int {
        return caseDao.insert(case).toInt()
    }

    override suspend fun getCase(id: Int): Case? {
        return caseDao.getCaseById(id)
    }

    override fun getAllCases(): Flow<List<Case>> { // Matches interface
        return caseDao.getAllCases()
    }

    override suspend fun updateCase(case: Case) {
        caseDao.update(case)
    }

    override suspend fun deleteCase(case: Case) {
        googleApiService?.deleteCaseFromRegistry(case)
        val folderIdToDelete = case.folderId ?: case.spreadsheetId
        googleApiService?.deleteFolder(folderIdToDelete)
        caseDao.delete(case)
    }

    // --- Other Public Methods (Not part of CaseRepository interface) ---

    suspend fun refreshCasesFromRemote() { // No override
        try {
            val currentGoogleApiService = googleApiService ?: return
            val appRootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return
            val registryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return
            val casesFromRegistry = currentGoogleApiService.getAllCasesFromRegistry(registryId)
            casesFromRegistry.forEach { caseDao.insert(it) } // Assumes onConflict strategy handles duplicates
        } catch (e: Exception) {
            Log.e("CaseRepoImpl", "Error refreshing cases from remote", e)
        }
    }

    suspend fun createNewCaseWithDetails( // No override
        caseName: String, exhibitSheetName: String, caseNumber: String, caseSection: String,
        caseJudge: String, plaintiffs: String, defendants: String, court: String
    ): Case? {
        val currentGoogleApiService = googleApiService ?: return null
        val rootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return null
        val caseRegistryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return null
        val caseFolderId = currentGoogleApiService.getOrCreateCaseFolder(caseName) ?: return null
        currentGoogleApiService.getOrCreateEvidenceFolder(caseName) // Ensure evidence folder exists

        return when (val result = currentGoogleApiService.createSpreadsheet(caseName, caseFolderId)) {
            is Result.Success -> {
                val caseSpreadsheetId = result.data ?: return null
                val scriptTemplate = applicationContext.resources.openRawResource(R.raw.apps_script_template)
                    .use { InputStreamReader(it).readText() }
                val scriptContent = scriptTemplate
                    .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
                    .replace("{{CASE_NUMBER}}", caseNumber)
                    .replace("{{CASE_SECTION}}", caseSection)
                    .replace("{{CASE_JUDGE}}", caseJudge)
                
                val scriptId = currentGoogleApiService.attachScript(caseSpreadsheetId, scriptContent, "")

                val newCase = Case(
                    name = caseName, spreadsheetId = caseSpreadsheetId, scriptId = scriptId,
                    folderId = caseFolderId, plaintiffs = plaintiffs, defendants = defendants, court = court
                )
                if (currentGoogleApiService.addCaseToRegistry(caseRegistryId, newCase)) {
                    caseDao.insert(newCase) // Save to local DB as well
                    newCase
                } else { 
                    Log.e("CaseRepoImpl", "Failed to add case to registry: ${newCase.name}")
                    // TODO: Consider cleanup of created Drive items if registry addition fails
                    null 
                }
            }
            is Result.Error -> {
                Log.e("CaseRepoImpl", "Error creating spreadsheet: ${result.error}")
                null
            }
        }
    }

    suspend fun archiveExistingCase(case: Case) { // No override
        val updatedCase = case.copy(isArchived = true)
        googleApiService?.updateCaseInRegistry(updatedCase)
        caseDao.update(updatedCase) // Update local DB
    }

    fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    suspend fun refreshSheetFilters(spreadsheetId: String) { // No override
        val allSheetData = googleApiService?.readSpreadsheet(spreadsheetId)
        val filterSheetData = allSheetData?.get("Filters")
        _sheetFilters.value = filterSheetData?.mapNotNull { row ->
            if (row.size >= 2) SheetFilter(row.getOrNull(0)?.toString() ?: "", row.getOrNull(1)?.toString() ?: "") else null
        } ?: emptyList()
    }

    suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) { // No override
        if (googleApiService?.addSheet(spreadsheetId, "Filters") != null) { // Ensure sheet exists
            if (googleApiService?.appendData(spreadsheetId, "Filters", listOf(listOf(name, value))) != null) {
                refreshSheetFilters(spreadsheetId)
            }
        }
    }

    fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> = _allegations.asStateFlow()

    suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) { // No override
        _allegations.value = googleApiService?.getAllegationsForCase(spreadsheetId, caseId) ?: emptyList()
    }

    suspend fun addAllegation(spreadsheetId: String, allegationText: String) { // No override
        if (googleApiService?.addAllegationToCase(spreadsheetId, allegationText) == true) {
            // Consider how to refresh; may need caseId to trigger specific refresh
        }
    }

    fun getHtmlTemplates(): Flow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    suspend fun refreshHtmlTemplates() { // No override
        try {
            _htmlTemplates.value = googleApiService?.listHtmlTemplatesInAppRootFolder() ?: emptyList()
        } catch (e: Exception) {
            Log.e("CaseRepoImpl", "Error refreshing HTML templates", e)
        }
    }

    suspend fun importSpreadsheetAndStore(spreadsheetId: String): Case? { // No override
        val currentGoogleApiService = googleApiService // Use a local val for smart cast and null safety
        if (currentGoogleApiService == null) {
            Log.e("CaseRepoImpl", "GoogleApiService is null, cannot import spreadsheet.")
            return null
        }

        val sheetsData = currentGoogleApiService.readSpreadsheet(spreadsheetId)
        if (sheetsData != null) {
            try {
                val schemaJson = applicationContext.resources.openRawResource(R.raw.spreadsheet_schema)
                    .bufferedReader().use { it.readText() }
                val schema = Gson().fromJson(schemaJson, SpreadsheetSchema::class.java)
                
                // currentGoogleApiService is guaranteed non-null here
                val spreadsheetParser = SpreadsheetParser(currentGoogleApiService, schema, caseDao) 
                
                val importedCase = spreadsheetParser.parseAndStore(sheetsData)
                // if (importedCase != null && !wasSavedByParser) { caseDao.insert(importedCase) }
                return importedCase
            } catch (e: Exception) {
                Log.e("CaseRepoImpl", "Error parsing spreadsheet $spreadsheetId: ${e.message}", e)
            }
        }
        return null
    }
}
