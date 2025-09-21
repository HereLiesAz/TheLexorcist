package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val content: String, // The actual template content (e.g., HTML)
    val authorName: String = "", // Manually entered name of the author
    val authorEmail: String = "", // Email of the user who shared it (or manually entered)
    val court: String? = null, // Changed to nullable
    val rating: Double = 0.0,
    val numRatings: Int = 0,
) : Parcelable
