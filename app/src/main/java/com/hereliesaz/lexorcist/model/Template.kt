package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val content: String,
    val authorName: String,
    val authorEmail: String,
    val court: String?,
    val rating: Double = 0.0,
    val numRatings: Int = 0
) : Parcelable
