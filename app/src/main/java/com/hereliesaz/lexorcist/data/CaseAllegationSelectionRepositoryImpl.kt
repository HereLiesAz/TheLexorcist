package com.hereliesaz.lexorcist.data

// Removed: import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.GoogleApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseAllegationSelectionRepositoryImpl
    @Inject
    constructor(
        private val googleApiService: GoogleApiService, // Injected GoogleApiService directly
    ) : CaseAllegationSelectionRepository {
        // Removed the local googleApiService getter property

        override fun getSelectedAllegations(spreadsheetId: String): Flow<List<SelectedAllegation>> =
            flow {
                // Use the directly injected googleApiService
                val result = googleApiService.getSelectedAllegations(spreadsheetId) ?: emptyList()
                emit(result)
            }

        override suspend fun updateSelectedAllegations(
            spreadsheetId: String,
            allegations: List<SelectedAllegation>,
        ) {
            // Use the directly injected googleApiService
            googleApiService.updateSelectedAllegations(spreadsheetId, allegations)
        }
    }
