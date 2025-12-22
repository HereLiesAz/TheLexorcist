package com.hereliesaz.lexorcist.model

import androidx.compose.runtime.Immutable
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem

@Immutable
data class DisplayExhibit(
    val catalogItem: ExhibitCatalogItem,
    val caseExhibit: Exhibit?
)
