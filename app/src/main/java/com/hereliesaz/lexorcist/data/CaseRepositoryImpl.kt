package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.SheetFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader

class CaseRepositoryImpl(
    private val applicationContext: Context,
    private var googleApiService: GoogleApiService?
) : CaseRepository {

    fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())

    override fun getCases(): Flow<List<Case>> = _cases.asStateFlow()

    override suspend fun refreshCases() {
        val appRootFolderId = googleApiService?.getOrCreateAppRootFolder() ?: return
        val registryId = googleApiService?.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return
        _cases.value = googleApiService?.getAllCasesFromRegistry(registryId) ?: emptyList()
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
        selectedMasterHtmlTemplateId: String
    ) {
        val rootFolderId = googleApiService?.getOrCreateAppRootFolder() ?: return
        val caseRegistryId = googleApiService?.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return
        val caseFolderId = googleApiService?.getOrCreateCaseFolder(caseName) ?: return
        googleApiService?.getOrCreateEvidenceFolder(caseName)

        val htmlTemplateContent = googleApiService?.downloadFileAsString(selectedMasterHtmlTemplateId) ?: return

        var processedHtml = htmlTemplateContent
            .replace("{{CASE_NAME}}", caseName)
            .replace("{{CASE_NUMBER}}", caseNumber)
            .replace("{{CASE_SECTION}}", caseSection)
            .replace("{{JUDGE}}", caseJudge)
        processedHtml = processedHtml
            .replace("{{PLAINTIFFS}}", plaintiffs)
            .replace("{{DEFENDANTS}}", defendants)
            .replace("{{COURT}}", court)

        val snapshotHtmlFileName = "${caseName}_Snapshot.html"
        val uploadedHtmlSnapshot = googleApiService?.uploadStringAsFile(processedHtml, "text/html", snapshotHtmlFileName, caseFolderId)
        val snapshotHtmlId = uploadedHtmlSnapshot?.id

        // PDF generation logic is complex and involves UI components (WebView),
        // which should not be in a repository. This needs to be handled in the ViewModel.
        // For now, I will skip the PDF generation part.
        val generatedPdfId: String? = null

        val caseSpreadsheetId = googleApiService?.createSpreadsheet(caseName, caseFolderId) ?: return

        val scriptTemplate = applicationContext.resources.openRawResource(R.raw.apps_script_template).use { InputStreamReader(it).readText() }
        val scriptContent = scriptTemplate
            .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
            .replace("{{CASE_NUMBER}}", caseNumber)
            .replace("{{CASE_SECTION}}", caseSection)
            .replace("{{CASE_JUDGE}}", caseJudge)

        googleApiService?.attachScript(caseSpreadsheetId, scriptContent, generatedPdfId ?: "")

        val newCase = Case(
            name = caseName,
            spreadsheetId = caseSpreadsheetId,
            generatedPdfId = generatedPdfId,
            sourceHtmlSnapshotId = snapshotHtmlId,
            originalMasterHtmlTemplateId = selectedMasterHtmlTemplateId
        )
        if (googleApiService?.addCaseToRegistry(caseRegistryId, newCase) == true) {
            refreshCases()
        }
    }

    override fun getHtmlTemplates(): Flow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    override suspend fun refreshHtmlTemplates() {
        _htmlTemplates.value = googleApiService?.listHtmlTemplatesInAppRootFolder() ?: emptyList()
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
