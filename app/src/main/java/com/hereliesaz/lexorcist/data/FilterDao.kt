package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface FilterDao {
    fun getFiltersForCase(spreadsheetId: String): Flow<List<Filter>>

    suspend fun insert(filter: Filter)
}
