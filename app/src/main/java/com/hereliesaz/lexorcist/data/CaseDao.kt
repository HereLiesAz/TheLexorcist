package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface CaseDao {
    suspend fun insert(case: Case)

    fun getAllCases(): Flow<List<Case>>

    suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case?

    suspend fun delete(case: Case)
}
