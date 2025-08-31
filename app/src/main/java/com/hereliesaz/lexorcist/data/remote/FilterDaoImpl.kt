package com.hereliesaz.lexorcist.data.remote

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.Filter
import com.hereliesaz.lexorcist.data.FilterDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FilterDaoImpl @Inject constructor(
    private val googleApiService: GoogleApiService
) : FilterDao {

    override fun getFiltersForCase(spreadsheetId: String): Flow<List<Filter>> = flow {
        emit(googleApiService.getFiltersForCase(spreadsheetId))
    }

    override suspend fun insert(filter: Filter) {
        googleApiService.addFilterToCase(filter.spreadsheetId, filter)
    }
}
