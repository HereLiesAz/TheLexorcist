package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class Exhibit(
    val id: Int = 0,
    val caseId: Long,
    val name: String,
    val description: String,
    val evidenceIds: List<Int>
) : Parcelable
