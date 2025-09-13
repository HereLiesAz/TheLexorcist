package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface CaseAllegationSelectionRepository {
    fun getSelectedAllegations(spreadsheetId: String): Flow<List<String>>

    suspend fun updateSelectedAllegations(
        spreadsheetId: String,
        allegations: List<String>,
    )
}
