package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.SheetFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow // Added import
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.utils.Result

interface CaseRepository {
    /**
     * A flow of the list of cases.
     * This flow will emit a new list of cases whenever the data changes.
     */
    val cases: Flow<List<Case>>
    val selectedCase: StateFlow<Case?> // Changed Flow to StateFlow

    /**
     * A flow of allegations for the currently selected case.
     */
    val selectedCaseAllegations: Flow<List<Allegation>>

    /**
     * A flow of evidence for the currently selected case, wrapped in a Result to handle loading/error states.
     */
    val selectedCaseEvidence: Flow<Result<List<Evidence>>>

    suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case?

    suspend fun selectCase(case: Case?)

    suspend fun refreshCases()

    /**
     * Refreshes the details (like allegations and evidence) for the currently selected case.
     */
    suspend fun refreshSelectedCaseDetails()

    suspend fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
        plaintiffs: String,
        defendants: String,
        court: String,
    ): Result<Case> // Changed to return Result<Case>

    suspend fun archiveCase(case: Case): Result<Case> // Changed to return Result<Case>

    suspend fun deleteCase(case: Case): Result<Unit> // Kept as Result<Unit> as per Impl

    fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>>

    suspend fun refreshSheetFilters(spreadsheetId: String)

    suspend fun addSheetFilter(
        spreadsheetId: String,
        name: String,
        value: String,
    )

    suspend fun addAllegation(
        spreadsheetId: String,
        allegationText: String,
    )

    fun getHtmlTemplates(): Flow<List<DriveFile>>

    suspend fun refreshHtmlTemplates()

    suspend fun importSpreadsheet(spreadsheetId: String): Case?

    suspend fun updateCase(case: Case): Result<Unit>

    suspend fun clearCache()

    suspend fun synchronize()
}
