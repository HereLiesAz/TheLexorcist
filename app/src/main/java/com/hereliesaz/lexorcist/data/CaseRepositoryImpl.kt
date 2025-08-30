package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.util.CacheManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader

class CaseRepositoryImpl(
    private val caseDao: CaseDao,
    private val applicationContext: Context
) : CaseRepository {

    private var googleApiService: GoogleApiService? = null

    fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    private val cacheManager = CacheManager(applicationContext)

    override fun getCases(): Flow<List<Case>> = caseDao.getAllCases()

    override suspend fun refreshCases() {
        try {
            val appRootFolderId = googleApiService?.getOrCreateAppRootFolder() ?: return
            val registryId = googleApiService?.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return
            val cases = googleApiService?.getAllCasesFromRegistry(registryId) ?: emptyList()
            cases.forEach { caseDao.insert(it) }
        } catch (e: Exception) {
            // No action on exception, local data will be served
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
        val rootFolderId = googleApiService?.getOrCreateAppRootFolder() ?: return
        val caseRegistryId = googleApiService?.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return
        val caseFolderId = googleApiService?.getOrCreateCaseFolder(caseName) ?: return
        googleApiService?.getOrCreateEvidenceFolder(caseName)

        val caseSpreadsheetId = googleApiService?.createSpreadsheet(caseName, caseFolderId) ?: return

        val scriptTemplate = applicationContext.resources.openRawResource(R.raw.apps_script_template).use { InputStreamReader(it).readText() }
        val scriptContent = scriptTemplate
            .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
            .replace("{{CASE_NUMBER}}", caseNumber)
            .replace("{{CASE_SECTION}}", caseSection)
            .replace("{{CASE_JUDGE}}", caseJudge)

        googleApiService?.attachScript(caseSpreadsheetId, scriptContent, "")

        val newCase = Case(
            name = caseName,
            spreadsheetId = caseSpreadsheetId,
            generatedPdfId = null,
            sourceHtmlSnapshotId = null
        )
        if (googleApiService?.addCaseToRegistry(caseRegistryId, newCase) == true) {
            refreshCases()
        }
    }

    override suspend fun archiveCase(case: Case) {
        val updatedCase = case.copy(isArchived = true)
        googleApiService?.updateCaseInRegistry(updatedCase)
        caseDao.insert(updatedCase)
    }

    override suspend fun deleteCase(case: Case) {
        googleApiService?.deleteCaseFromRegistry(case)
        googleApiService?.deleteFolder(case.spreadsheetId)
        caseDao.deleteCaseById(case.id)
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        val allSheetData = googleApiService?.readSpreadsheet(spreadsheetId)
        val filterSheetData = allSheetData?.get("Filters")
        _sheetFilters.value = filterSheetData?.mapNotNull {
            if (it.size >= 2) SheetFilter(it.getOrNull(0)?.toString() ?: "", it.getOrNull(1)?.toString() ?: "") else null
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
        if (googleApiService?.addAllegationToCase(spreadsheetId, allegationText) == true) {
            // This is tricky without the caseId. The calling ViewModel will need to trigger the refresh.
            // For now, the refresh is not called automatically after adding an allegation.
        }
    }

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    override fun getHtmlTemplates(): Flow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    override suspend fun refreshHtmlTemplates() {
        try {
            _htmlTemplates.value = googleApiService?.listHtmlTemplatesInAppRootFolder() ?: emptyList()
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val sheetsData = googleApiService?.readSpreadsheet(spreadsheetId)
        if (sheetsData != null) {
            try {
                val schemaJson = applicationContext.resources.openRawResource(R.raw.spreadsheet_schema).bufferedReader().use { it.readText() }
                val schema = com.google.gson.Gson().fromJson(schemaJson, com.hereliesaz.lexorcist.model.SpreadsheetSchema::class.java)

                val spreadsheetParser = com.hereliesaz.lexorcist.SpreadsheetParser(googleApiService!!, schema)
                val newCase = spreadsheetParser.parseAndStore(sheetsData)
                if (newCase != null) {
                    refreshCases()
                    return newCase
                }
            } catch (e: Exception) {
                // Log error
            }
        }
        return null
    }
}
