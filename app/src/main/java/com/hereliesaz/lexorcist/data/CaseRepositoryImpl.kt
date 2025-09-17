package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.data.objectbox.CaseObjectBox
import com.hereliesaz.lexorcist.data.objectbox.CaseObjectBox_
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.utils.ErrorReporter
import io.objectbox.BoxStore
import io.objectbox.kotlin.asFlow // Specific import
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val storageService: StorageService,
    private val settingsManager: SettingsManager,
    private val errorReporter: ErrorReporter,
    private val caseSheetParser: CaseSheetParser,
    private val credentialHolder: com.hereliesaz.lexorcist.auth.CredentialHolder,
    private val boxStore: BoxStore
) : CaseRepository {
    private val caseBox = boxStore.boxFor(CaseObjectBox::class.java)
    private val query = caseBox.query().build() // Explicit query variable
    private val _selectedCase = MutableStateFlow<Case?>(null)

    override val cases: Flow<List<Case>> = query.asFlow().map { objectBoxCases -> // Extension function style
        objectBoxCases.map { it.toCase() }
    }
    override val selectedCase: Flow<Case?> = _selectedCase.asStateFlow()

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return caseBox.query(CaseObjectBox_.spreadsheetId.equal(spreadsheetId)).build().findFirst()?.toCase()
    }

    override suspend fun selectCase(case: Case?) {
        _selectedCase.value = case
    }

    override suspend fun refreshCases() {
        // No longer needed, ObjectBox flow updates automatically
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
    ): Result<Unit> {
        val newCase = Case(
            name = caseName,
            spreadsheetId = "", // will be set by storageService
            plaintiffs = plaintiffs,
            defendants = defendants,
            court = court
        )

        return when (val result = storageService.createCase(newCase)) {
            is Result.Success -> {
                val createdCase = result.data
                caseBox.put(createdCase.toCaseObjectBox())
                Result.Success(Unit)
            }
            is Result.Error -> {
                errorReporter.reportError(result.exception)
                Result.Error(result.exception)
            }
            is Result.UserRecoverableError -> {
                errorReporter.reportError(result.exception)
                result
            }
        }
    }

    override suspend fun archiveCase(case: Case) {
        val archivedCase = case.copy(isArchived = true)
        when (val result = storageService.updateCase(archivedCase)) {
            is Result.Success -> {
                val caseObjectBox = caseBox.query(CaseObjectBox_.spreadsheetId.equal(case.spreadsheetId)).build().findFirst()
                if (caseObjectBox != null) {
                    caseObjectBox.isArchived = true
                    caseBox.put(caseObjectBox)
                }
            }
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override suspend fun deleteCase(case: Case) {
        when (val result = storageService.deleteCase(case)) {
            is Result.Success -> {
                val caseObjectBox = caseBox.query(CaseObjectBox_.spreadsheetId.equal(case.spreadsheetId)).build().findFirst()
                if (caseObjectBox != null) {
                    caseBox.remove(caseObjectBox)
                }
            }
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> {
        // TODO: Implement actual logic
        return emptyFlow()
    }

    override suspend fun refreshSheetFilters(spreadsheetId: String) {
        // TODO: Implement actual logic
    }

    override suspend fun addSheetFilter(
        spreadsheetId: String,
        name: String,
        value: String,
    ) {
        // TODO: Implement actual logic
    }

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())

    override fun getAllegations(
        caseId: Int,
        spreadsheetId: String,
    ): Flow<List<Allegation>> {
        return _allegations.asStateFlow()
    }

    override suspend fun refreshAllegations(
        caseId: Int,
        spreadsheetId: String,
    ) {
        when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
            is Result.Success -> _allegations.value = result.data
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override suspend fun addAllegation(
        spreadsheetId: String,
        allegationText: String,
    ) {
        val allegation = Allegation(spreadsheetId = spreadsheetId, text = allegationText)
        storageService.addAllegation(spreadsheetId, allegation)
        refreshAllegations(0, spreadsheetId) // caseId is not used in refreshAllegations
    }

    override suspend fun getEvidenceForCase(spreadsheetId: String): Result<List<Evidence>> {
        return storageService.getEvidenceForCase(spreadsheetId)
    }

    override fun getHtmlTemplates(): Flow<List<DriveFile>> {
        // TODO: Implement actual logic
        return emptyFlow()
    }

    override suspend fun refreshHtmlTemplates() {
        // TODO: Implement actual logic
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val googleApiService = credentialHolder.googleApiService ?: return null
        val sheetData = googleApiService.readSpreadsheet(spreadsheetId)
        if (sheetData.isNullOrEmpty()) {
            return null
        }
        val parsedData = caseSheetParser.parseCaseFromData(spreadsheetId, sheetData)
        if (parsedData != null) {
            val (newCase, evidenceList) = parsedData
            storageService.createCase(newCase)
            evidenceList.forEach { storageService.addEvidence(newCase.spreadsheetId, it) }
            caseBox.put(newCase.toCaseObjectBox())
            return newCase
        }
        return null
    }

    override suspend fun clearCache() {
        caseBox.removeAll()
    }

    override suspend fun synchronize() {
        storageService.synchronize()
    }

    private fun CaseObjectBox.toCase(): Case {
        return Case(
            id = this.id.toInt(),
            name = this.name,
            spreadsheetId = this.spreadsheetId,
            scriptId = this.scriptId,
            generatedPdfId = this.generatedPdfId,
            sourceHtmlSnapshotId = this.sourceHtmlSnapshotId,
            originalMasterHtmlTemplateId = this.originalMasterHtmlTemplateId,
            folderId = this.folderId,
            plaintiffs = this.plaintiffs,
            defendants = this.defendants,
            court = this.court,
            isArchived = this.isArchived,
            lastModifiedTime = this.lastModifiedTime
        )
    }

    private fun Case.toCaseObjectBox(): CaseObjectBox {
        val case = this
        return CaseObjectBox(
            id = case.id.toLong(),
            name = case.name,
            spreadsheetId = case.spreadsheetId,
            scriptId = case.scriptId,
            generatedPdfId = case.generatedPdfId,
            sourceHtmlSnapshotId = case.sourceHtmlSnapshotId,
            originalMasterHtmlTemplateId = case.originalMasterHtmlTemplateId,
            folderId = case.folderId,
            plaintiffs = case.plaintiffs,
            defendants = case.defendants,
            court = case.court,
            isArchived = case.isArchived,
            lastModifiedTime = case.lastModifiedTime
        )
    }
}
