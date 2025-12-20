package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * Represents an edit to a transcript.
 *
 * Marked as [Immutable] to ensure stability in Jetpack Compose,
 * preventing unnecessary recompositions when used in lists within [Evidence].
 */
@Immutable
@Parcelize
data class TranscriptEdit(
    val timestamp: Long,
    val reason: String,
    val content: String
) : Parcelable
