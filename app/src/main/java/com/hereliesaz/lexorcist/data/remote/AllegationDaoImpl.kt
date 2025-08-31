package com.hereliesaz.lexorcist.data.remote

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.AllegationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AllegationDaoImpl @Inject constructor(
    private val googleApiService: GoogleApiService
) : AllegationDao {

    override suspend fun insert(allegation: Allegation) {
        googleApiService.addAllegationToCase(allegation.spreadsheetId, allegation.text)
    }

    override fun getAllegationsForCase(spreadsheetId: String, caseId: Int): Flow<List<Allegation>> = flow {
        emit(googleApiService.getAllegationsForCase(spreadsheetId, caseId))
    }

    override suspend fun delete(allegation: Allegation) {
        googleApiService.deleteAllegation(allegation.spreadsheetId, allegation.id)
    }
}
