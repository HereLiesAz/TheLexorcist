package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val content: String, // The actual template content (e.g., HTML)
    val author: String, // User who shared it
    val court: String,
    val rating: Double = 0.0,
    val numRatings: Int = 0,
) : Parcelable
