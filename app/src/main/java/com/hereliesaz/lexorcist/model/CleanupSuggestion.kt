package com.hereliesaz.lexorcist.model

import com.hereliesaz.lexorcist.data.Evidence

sealed class CleanupSuggestion {
    data class DuplicateGroup(val evidence: List<Evidence>) : CleanupSuggestion()
    data class ImageSeriesGroup(val evidence: List<Evidence>) : CleanupSuggestion()
}