package com.hereliesaz.lexorcist.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectedAllegation(
    val id: String,
    val name: String
) : Parcelable