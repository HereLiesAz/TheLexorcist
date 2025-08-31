package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface CaseDao {
    suspend fun insert(case: Case): Long // Changed to return Long (e.g., a row ID or count)
    fun getAllCases(): Flow<List<Case>>
    suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case?
    suspend fun getCaseById(id: Int): Case? // Added
    suspend fun update(case: Case)          // Added
    suspend fun delete(case: Case)
}
