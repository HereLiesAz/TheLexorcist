package com.hereliesaz.lexorcist.model

import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem

data class DisplayExhibit(
    val catalogItem: ExhibitCatalogItem,
    val caseExhibit: Exhibit? = null
)