package com.hereliesaz.lexorcist.model

import com.google.gson.annotations.SerializedName

data class SelectionState(
    @SerializedName("selected_case_id")
    val selectedCaseId: String? = null,
    @SerializedName("selected_exhibit_id")
    val selectedExhibitId: String? = null
)