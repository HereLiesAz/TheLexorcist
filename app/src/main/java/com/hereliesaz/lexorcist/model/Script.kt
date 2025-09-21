package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Script(
    val id: String,
    val name: String,
    val description: String,
    val content: String, // The actual script code
    val authorName: String = "", // Manually entered name of the author
    val authorEmail: String = "", // Email of the user who shared it (or manually entered)
    val court: String? = null, // Standardized position
    val rating: Double = 0.0,
    val numRatings: Int = 0
) : Parcelable
