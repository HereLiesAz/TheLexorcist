package com.hereliesaz.lexorcist.model

import com.google.gson.annotations.SerializedName

data class SpreadsheetSchema(
    @SerializedName("caseInfoSheet") val caseInfoSheet: CaseInfoSheet,
    @SerializedName("allegationsSheet") val allegationsSheet: AllegationsSheet,
    @SerializedName("evidenceSheet") val evidenceSheet: EvidenceSheet,
)

data class CaseInfoSheet(
    @SerializedName("name") val name: String,
    @SerializedName("caseNameLabel") val caseNameLabel: String,
    @SerializedName("caseNameColumn") val caseNameColumn: Int,
)

data class AllegationsSheet(
    @SerializedName("name") val name: String,
    @SerializedName("allegationColumn") val allegationColumn: Int,
)

data class EvidenceSheet(
    @SerializedName("name") val name: String,
    @SerializedName("contentColumn") val contentColumn: Int,
    @SerializedName("tagsColumn") val tagsColumn: Int,
)
