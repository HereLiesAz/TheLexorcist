package com.hereliesaz.lexorcist.data

import com.google.gson.annotations.SerializedName

data class ExhibitCatalogItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("exhibit_type")
    val type: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("applicable_allegation_ids")
    val applicableAllegationIds: List<Int>
)