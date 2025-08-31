package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Script(
    val id: String,
    val name: String,
    val description: String,
    val content: String, // The actual script code
    val author: String, // User who shared it
    val rating: Double = 0.0,
    val numRatings: Int = 0
) : Parcelable
