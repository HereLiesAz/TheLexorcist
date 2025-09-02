package com.hereliesaz.lexorcist.data

interface AllegationsRepository {
    suspend fun getAllegations(caseId: String): List<Allegation>
}
