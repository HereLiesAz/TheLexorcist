package com.hereliesaz.lexorcist.data.remote

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CaseDaoImpl @Inject constructor(
    private val googleApiService: GoogleApiService
) : CaseDao {

    override fun getAllCases(): Flow<List<Case>> = flow {
        val appRootFolderId = googleApiService.getOrCreateAppRootFolder() ?: return@flow
        val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return@flow
        emit(googleApiService.getAllCasesFromRegistry(registryId))
    }

    override suspend fun getCaseById(id: Int): Case? {
        // This is not possible with the current Google Sheets setup
        return null
    }

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        // This is not possible with the current Google Sheets setup
        return null
    }

    override suspend fun insert(case: Case): Long {
        val appRootFolderId = googleApiService.getOrCreateAppRootFolder() ?: return 0
        val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return 0
        return if (googleApiService.addCaseToRegistry(registryId, case)) 1 else 0
    }

    override suspend fun update(case: Case) {
        googleApiService.updateCaseInRegistry(case)
    }

    override suspend fun delete(case: Case) {
        googleApiService.deleteCaseFromRegistry(case)
    }
}
