package com.hereliesaz.lexorcist.model

import kotlinx.serialization.Serializable

@Serializable
data class UiComponentModel(
    val id: String,
    val type: String,
    val properties: Map<String, String>,
    val onClick: String? = null
)
