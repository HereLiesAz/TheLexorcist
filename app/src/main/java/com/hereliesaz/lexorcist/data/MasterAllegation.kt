package com.hereliesaz.lexorcist.data

data class MasterAllegation(
    val id: String?, // Reverted to nullable
    val type: String?,
    val category: String?,
    val name: String, // Remains non-nullable, as 'name' is present in JSON
    val description: String?,
    val courtLevel: String?,
    val isSelected: Boolean = false,
)
