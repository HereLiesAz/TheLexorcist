package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RelevantEvidence(
    val physical: List<String>? = emptyList(),
    val documentary: List<String>? = emptyList(),
    val testimonial: List<String>? = emptyList(),
    val digital: List<String>? = emptyList(),
    val demonstrative: List<String>? = emptyList()
) : Parcelable