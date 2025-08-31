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

    private suspend fun getRegistryId(): String? {
        val appRootFolderId = googleApiService.getOrCreateAppRootFolder()
        return appRootFolderId?.let {
            googleApiService.getOrCreateCaseRegistrySpreadsheetId(it)
        }
    }

    override suspend fun insert(case: Case) {
        getRegistryId()?.let {
            googleApiService.addCaseToRegistry(it, case)
        }
    }

    override fun getAllCases(): Flow<List<Case>> = flow {
        getRegistryId()?.let {
            emit(googleApiService.getAllCasesFromRegistry(it))
        }
    }

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        val registryId = getRegistryId() ?: return null
        val cases = googleApiService.getAllCasesFromRegistry(registryId)
        return cases.find { it.spreadsheetId == spreadsheetId }
    }

    override suspend fun delete(case: Case) {
        googleApiService.deleteCaseFromRegistry(case)
    }
}
