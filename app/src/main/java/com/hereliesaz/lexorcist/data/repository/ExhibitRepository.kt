package com.hereliesaz.lexorcist.data.repository

import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import kotlinx.coroutines.flow.Flow

interface ExhibitRepository {
    fun getExhibitCatalog(): Flow<List<ExhibitCatalogItem>>
}