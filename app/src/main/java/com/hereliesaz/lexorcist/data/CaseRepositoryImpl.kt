package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.data.SpreadsheetParser
import com.hereliesaz.lexorcist.utils.CacheManager
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val caseDao: CaseDao,
    private val googleApiService: GoogleApiService?,
    private val spreadsheetParser: SpreadsheetParser
) : CaseRepository {

    private val tag = "CaseRepositoryImpl"

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    private val cacheManager = CacheManager(context)

    override fun getCases(): Flow<List<Case>> {
        return caseDao.getAllCases()
    }

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return caseDao.getCaseBySpreadsheetId(spreadsheetId)
    }

    override suspend fun refreshCases() {
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
        currentGoogleApiService.getOrCreateEvidenceFolder(caseName)

        when (val result = currentGoogleApiService.createSpreadsheet(caseName, caseFolderId)) {
            is Result.Success -> {
                val caseSpreadsheetId = result.data ?: return
                val scriptTemplate = context.resources.openRawResource(R.raw.apps_script_template)
                    .use { InputStreamReader(it).readText() }
                val scriptContent = scriptTemplate
                    .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
                    .replace("{{CASE_NUMBER}}", caseNumber)
                    .replace("{{CASE_SECTION}}", caseSection)
                    .replace("{{CASE_JUDGE}}", caseJudge)

                val scriptId = currentGoogleApiService.attachScript(caseSpreadsheetId, scriptContent, "")

                val newCase = Case(
                    name = caseName,
                    spreadsheetId = caseSpreadsheetId,
                    scriptId = scriptId,
                    folderId = caseFolderId,
                    plaintiffs = plaintiffs,
                    defendants = defendants,
                    court = court
                )
                if (currentGoogleApiService.addCaseToRegistry(caseRegistryId, newCase)) {
                    caseDao.insert(newCase)
                } else {
                    Log.e(tag, "Failed to add case to registry: ${newCase.name}")
                }
            }
            is Result.Error -> {
                Log.e(tag, "Error creating spreadsheet: ${result.exception.message}")
            }
        }
    }

    override suspend fun archiveCase(case: Case) {
        val updatedCase = case.copy(isArchived = true)
        googleApiService?.updateCaseInRegistry(updatedCase)
        caseDao.update(updatedCase)
    }

    override suspend fun deleteCase(case: Case) {
        googleApiService?.deleteCaseFromRegistry(case)
        val folderIdToDelete = case.folderId ?: case.spreadsheetId
        googleApiService?.deleteFolder(folderIdToDelete)
        caseDao.delete(case)
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        val sheetData = googleApiService?.readSpreadsheet(spreadsheetId)?.get("Filters")
        _sheetFilters.value = sheetData?.mapNotNull { row ->
            if (row.size >= 2) SheetFilter(row.getOrNull(0)?.toString() ?: "", row.getOrNull(1)?.toString() ?: "") else null
        } ?: emptyList()
    }

    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {
        googleApiService?.addSheet(spreadsheetId, "Filters")
        if (googleApiService?.appendData(spreadsheetId, "Filters", listOf(listOf(name, value))) != null) {
            refreshSheetFilters(spreadsheetId)
        }
    }

    override fun getAllegations(caseId: Int, spreadsheetId: String): Flow<List<Allegation>> = _allegations.asStateFlow()

    override suspend fun refreshAllegations(caseId: Int, spreadsheetId: String) {
        _allegations.value = googleApiService?.getAllegationsForCase(spreadsheetId, caseId) ?: emptyList()
    }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        googleApiService?.addAllegationToCase(spreadsheetId, allegationText)
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
                val importedCase = spreadsheetParser.parseAndStore(spreadsheetId, sheetsData)
                return importedCase
            } catch (e: Exception) {
                Log.e(tag, "Error parsing spreadsheet $spreadsheetId: ${e.message}", e)
            }
        }
        return null
    }
}
