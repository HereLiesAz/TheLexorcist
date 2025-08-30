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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val applicationContext: Context,
    private val caseDao: CaseDao,
    private val googleApiService: GoogleApiService
    private val googleApiService: GoogleApiService?
) : CaseRepository {

    override fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    private val cacheManager = CacheManager(applicationContext)

    override fun getCases(): Flow<List<Case>> = _cases.asStateFlow()

    override suspend fun getCaseById(id: Int): Case? {
        return _cases.value.find { it.id == id }
    }

    override suspend fun refreshCases() {
        try {
            val appRootFolderId = googleApiService?.getOrCreateAppRootFolder() ?: return
            val registryId = googleApiService?.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return
            val cases = googleApiService?.getAllCasesFromRegistry(registryId) ?: emptyList()
            _cases.value = cases
            cacheManager.saveCases(cases)
        } catch (e: Exception) {
            _cases.value = cacheManager.loadCases() ?: emptyList()
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
        googleApiService.getOrCreateEvidenceFolder(caseName)

        when (val result = googleApiService.createSpreadsheet(caseName, caseFolderId)) {
            is com.hereliesaz.lexorcist.util.Result.Success -> {
                val caseSpreadsheetId = result.data ?: return
                val scriptTemplate = applicationContext.resources.openRawResource(R.raw.apps_script_template).use { InputStreamReader(it).readText() }
        val scriptContent = scriptTemplate
            .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
            .replace("{{CASE_NUMBER}}", caseNumber)
            .replace("{{CASE_SECTION}}", caseSection)
            .replace("{{CASE_JUDGE}}", caseJudge)

        val scriptId = googleApiService.attachScript(caseSpreadsheetId, scriptContent, "")
        googleApiService.attachScript(caseSpreadsheetId, scriptContent, "")

        val newCase = Case(
            name = caseName,
            spreadsheetId = caseSpreadsheetId,
            generatedPdfId = null,
            sourceHtmlSnapshotId = null
        )
        if (googleApiService.addCaseToRegistry(caseRegistryId, newCase) == true) {
            refreshCases()
        }
            }
            is com.hereliesaz.lexorcist.util.Result.Error -> {
                // Here we should probably expose the error to the ViewModel
            }
        }
    }

    override suspend fun archiveCase(case: Case) {
        // googleApiService?.updateCaseInRegistry(case.copy(isArchived = true))
        // TODO: Implement updateCaseInRegistry in GoogleApiService
        // googleApiService?.updateCaseInRegistry(case.copy(isArchived = true))
        refreshCases()
    }

    override suspend fun deleteCase(case: Case) {
        // googleApiService?.deleteCaseFromRegistry(case)
        // googleApiService?.deleteFolder(case.spreadsheetId)
        // TODO: Implement deleteCaseFromRegistry and deleteFolder in GoogleApiService
        // googleApiService?.deleteCaseFromRegistry(case)
        // googleApiService?.deleteFolder(case.spreadsheetId)
        refreshCases()
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
}
