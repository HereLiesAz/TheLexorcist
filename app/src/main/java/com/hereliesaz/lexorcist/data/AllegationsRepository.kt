package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.StateFlow

interface AllegationsRepository {
    // For case-specific allegations
    suspend fun getAllegations(caseId: String): List<Allegation>
}
