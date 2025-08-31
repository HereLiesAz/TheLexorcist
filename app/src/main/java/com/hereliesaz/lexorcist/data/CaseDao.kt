package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface CaseDao {
    fun getAllCases(): Flow<List<Case>>
    suspend fun getCaseById(id: Int): Case?
    suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case?
    suspend fun insert(case: Case): Long
    suspend fun update(case: Case)
    suspend fun delete(case: Case)
}
