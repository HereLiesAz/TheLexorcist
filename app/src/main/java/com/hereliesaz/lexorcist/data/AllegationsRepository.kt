package com.hereliesaz.lexorcist.data

interface AllegationsRepository {
    // For case-specific allegations
    suspend fun getAllegations(caseId: String): List<Allegation>
}
