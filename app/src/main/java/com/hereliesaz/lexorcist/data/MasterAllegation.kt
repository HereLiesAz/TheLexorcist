package com.hereliesaz.lexorcist.data

data class MasterAllegation(
    val id: String, // Changed from String? to String
    val type: String?,
    val category: String?,
    val name: String, // Changed from String? to String
    val description: String?,
    val courtLevel: String?,
    val isSelected: Boolean = false,
)
