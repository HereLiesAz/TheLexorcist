package com.hereliesaz.lexorcist.data.repository

import com.hereliesaz.lexorcist.model.SelectionState
import kotlinx.coroutines.flow.Flow

interface SelectionRepository {
    val selectionState: Flow<SelectionState>

    suspend fun selectCase(caseId: String?)

    suspend fun selectExhibit(exhibitId: String?)
}