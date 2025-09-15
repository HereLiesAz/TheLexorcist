package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranscriptEdit(
    val timestamp: Long,
    val reason: String,
    val content: String
) : Parcelable
