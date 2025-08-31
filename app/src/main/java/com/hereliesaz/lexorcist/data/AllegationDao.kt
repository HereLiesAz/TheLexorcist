package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface AllegationDao {
    suspend fun insert(allegation: Allegation)

    fun getAllegationsForCase(spreadsheetId: String, caseId: Int): Flow<List<Allegation>>

    suspend fun delete(allegation: Allegation)
}
