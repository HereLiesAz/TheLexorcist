package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.Allegation // Assuming this is the correct import
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.SpreadsheetParser // Assuming this is the correct import
import com.hereliesaz.lexorcist.util.CacheManager
import com.hereliesaz.lexorcist.util.Result // Assuming this is the correct import for Result.Success/Error
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context, // Changed to @ApplicationContext
    private val caseDao: CaseDao,
    private val googleApiService: GoogleApiService? // Changed to nullable
) : CaseRepository {

    private val tag = "CaseRepositoryImpl"

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    private val cacheManager = CacheManager(context) // Use injected context

     suspend fun createCase(case: Case): Int { // This was the original method in the interface definition
        return caseDao.insert(case).toInt()
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
    ): Case? {
        val currentGoogleApiService = googleApiService ?: run {
            Log.e(tag, "GoogleApiService not available, cannot create case.")
            return null
        }
        val rootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return null
        val caseRegistryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return null
        val caseFolderId = currentGoogleApiService.getOrCreateCaseFolder(caseName) ?: return null
        currentGoogleApiService.getOrCreateEvidenceFolder(caseName) // Ensure evidence folder exists

        return when (val result = currentGoogleApiService.createSpreadsheet(caseName, caseFolderId)) {
            is Result.Success -> {
                val caseSpreadsheetId = result.data ?: return null
                val scriptTemplate = context.resources.openRawResource(R.raw.apps_script_template)
                    .use { InputStreamReader(it).readText() }
                val scriptContent = scriptTemplate
                    .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
                    .replace("{{CASE_NUMBER}}", caseNumber)
                    .replace("{{CASE_SECTION}}", caseSection)
                    .replace("{{CASE_JUDGE}}", caseJudge)

                val scriptId = currentGoogleApiService.attachScript(caseSpreadsheetId, scriptContent, "")

                val newCase = Case( // Assuming Case constructor matches these fields
                    name = caseName,
                    spreadsheetId = caseSpreadsheetId,
                    scriptId = scriptId, // Assuming Case has scriptId
                    folderId = caseFolderId, // Assuming Case has folderId
                    plaintiffs = plaintiffs, // Assuming Case has plaintiffs
                    defendants = defendants, // Assuming Case has defendants
                    court = court // Assuming Case has court
                )
                if (currentGoogleApiService.addCaseToRegistry(caseRegistryId, newCase)) {
                    caseDao.insert(newCase) // Save to local DAO as well
                    newCase
                } else {
                    Log.e(tag, "Failed to add case to registry: ${newCase.name}")
                    null
                }
            }
            is Result.Error -> {
                Log.e(tag, "Error creating spreadsheet: ${result.error}")
                null
            }
        }
    }


    override suspend fun getCase(id: Int): Case? { // Assuming CaseRepository interface has this
        return caseDao.getCaseById(id)
    }

    override fun getAllCases(): Flow<List<Case>> {
        return caseDao.getAllCases()
    }

    override suspend fun updateCase(case: Case) { // Assuming CaseRepository interface has this
        caseDao.update(case)
    }

    override suspend fun deleteCase(case: Case) {
        googleApiService?.deleteCaseFromRegistry(case)
        val folderIdToDelete = case.folderId ?: case.spreadsheetId // Prefer folderId if available
        googleApiService?.deleteFolder(folderIdToDelete)
        caseDao.delete(case)
    }
    
    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return caseDao.getCaseBySpreadsheetId(spreadsheetId)
    }
    
    suspend fun refreshCasesFromRemote() { // Not in interface, but kept from original
        val currentGoogleApiService = googleApiService ?: return
        try {
            val appRootFolderId = currentGoogleApiService.getOrCreateAppRootFolder() ?: return
            val registryId = currentGoogleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return
            val casesFromRegistry = currentGoogleApiService.getAllCasesFromRegistry(registryId)
            casesFromRegistry.forEach { caseDao.insert(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing cases from remote", e)
        }
    }

    override suspend fun archiveCase(case: Case) { // Added to match previous impl, ensure in interface
        val updatedCase = case.copy(isArchived = true) // Assuming Case has isArchived
        googleApiService?.updateCaseInRegistry(updatedCase) // Assuming this method exists and takes updatedCase
        caseDao.update(updatedCase)
    }


    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        val sheetData = googleApiService?.readSpreadsheet(spreadsheetId)?.get("Filters")
        _sheetFilters.value = sheetData?.mapNotNull { row ->
            if (row.size >= 2) SheetFilter(row.getOrNull(0)?.toString() ?: "", row.getOrNull(1)?.toString() ?: "") else null
        } ?: emptyList()
    }

    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {
        googleApiService?.addSheet(spreadsheetId, "Filters") // Ensure sheet exists
        if (googleApiService?.appendData(spreadsheetId, "Filters", listOf(listOf(name, value))) != null) {
            refreshSheetFilters(spreadsheetId)
        }
    }

    override fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> = _allegations.asStateFlow() // Assuming CaseRepository interface defines this

    override suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) { // Assuming CaseRepository interface defines this
        _allegations.value = googleApiService?.getAllegationsForCase(spreadsheetId, caseId) ?: emptyList()
    }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) { // Assuming CaseRepository interface defines this
        googleApiService?.addAllegationToCase(spreadsheetId, allegationText)
    }

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    override fun getHtmlTemplates(): Flow<List<DriveFile>> = _htmlTemplates.asStateFlow() // Assuming CaseRepository interface defines this

    override suspend fun refreshHtmlTemplates() { // Assuming CaseRepository interface defines this
        _htmlTemplates.value = googleApiService?.listHtmlTemplatesInAppRootFolder() ?: emptyList()
    }
    
    override suspend fun importSpreadsheet(spreadsheetId: String): Case? { // Added to match previous impl, ensure in interface
        val currentGoogleApiService = googleApiService ?: return null
        val sheetsData = currentGoogleApiService.readSpreadsheet(spreadsheetId)
        if (sheetsData != null) {
            try {
                val schemaJson = context.resources.openRawResource(R.raw.spreadsheet_schema)
                    .bufferedReader().use { it.readText() }
                val schema = com.google.gson.Gson().fromJson(schemaJson, com.hereliesaz.lexorcist.model.SpreadsheetSchema::class.java)
                
                val spreadsheetParser = SpreadsheetParser(currentGoogleApiService, schema, caseDao) 
                
                val importedCase = spreadsheetParser.parseAndStore(sheetsData)
                if (importedCase != null) {
                }
                return importedCase
            } catch (e: Exception) {
                Log.e(tag, "Error parsing spreadsheet $spreadsheetId: ${e.message}", e)
            }
        }
        return null
    }
}
