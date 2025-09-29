package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface CaseAllegationSelectionRepository {
    fun getSelectedAllegations(spreadsheetId: String): Flow<List<SelectedAllegation>>

    suspend fun updateSelectedAllegations(
        spreadsheetId: String,
        allegations: List<SelectedAllegation>,
    )
}
