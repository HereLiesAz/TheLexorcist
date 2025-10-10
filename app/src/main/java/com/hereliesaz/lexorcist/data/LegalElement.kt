package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LegalElement(
    val id: String,
    val allegationId: String,
    val name: String,
    val description: String
) : Parcelable
