package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow

interface MasterAllegationRepository {
    fun getMasterAllegations(): Flow<List<MasterAllegation>>
}
