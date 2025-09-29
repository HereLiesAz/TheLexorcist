package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MasterAllegation(
    val id: String,
    val type: String?,
    val category: String?,
    val name: String,
    val description: String?,
    val courtLevel: String?,
    val elements: List<String>?,
    val relevantEvidence: RelevantEvidence?,
    val isSelected: Boolean = false
) : Parcelable
