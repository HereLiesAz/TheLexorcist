package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import javax.inject.Inject

class AllegationsRepositoryImpl @Inject constructor(
    private val googleApiService: GoogleApiService?
) : AllegationsRepository {

    override suspend fun getAllegations(caseId: String): List<Allegation> {
        // TODO: Implement logic to get allegations from the Google Sheet
        return emptyList()
    }
}
