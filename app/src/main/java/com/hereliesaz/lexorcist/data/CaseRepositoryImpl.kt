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
    private val caseDao: CaseDao,
    private var googleApiService: GoogleApiService?
) : CaseRepository {

    override fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())

    override fun getCases(): Flow<List<Case>> = caseDao.getAllCases()

    override suspend fun refreshCases() {
        // Data is now loaded from Room, so this function can be left empty
        // or used to sync with a remote source in the future.
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
        val caseFolderId = googleApiService?.getOrCreateCaseFolder(caseName) ?: return
        googleApiService?.getOrCreateEvidenceFolder(caseName)
        val caseSpreadsheetId = googleApiService?.createSpreadsheet(caseName, caseFolderId) ?: return

        val newCase = Case(
            name = caseName,
            spreadsheetId = caseSpreadsheetId
        )
        caseDao.insert(newCase)
    }

    override suspend fun deleteCase(case: Case) {
        caseDao.deleteCaseById(case.id)
        // I will add the Google Drive deletion logic here later
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
