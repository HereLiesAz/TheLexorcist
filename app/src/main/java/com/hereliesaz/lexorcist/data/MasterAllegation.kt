package com.hereliesaz.lexorcist.data

data class MasterAllegation(
    val id: String?,
    val type: String?,
    val category: String?,
    val name: String?,
    val description: String?,
    val courtLevel: String?,
    val isSelected: Boolean = false,
)
