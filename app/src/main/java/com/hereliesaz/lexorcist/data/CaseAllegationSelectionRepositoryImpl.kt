package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.GoogleApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseAllegationSelectionRepositoryImpl
    @Inject
    constructor(
        private val credentialHolder: CredentialHolder,
    ) : CaseAllegationSelectionRepository {
        private val googleApiService: GoogleApiService?
            get() = credentialHolder.googleApiService

        override fun getSelectedAllegations(spreadsheetId: String): Flow<List<String>> =
            flow {
                val result = googleApiService?.getSelectedAllegations(spreadsheetId) ?: emptyList()
                emit(result)
            }

        override suspend fun updateSelectedAllegations(
            spreadsheetId: String,
            allegations: List<String>,
        ) {
            googleApiService?.updateSelectedAllegations(spreadsheetId, allegations)
        }
    }
