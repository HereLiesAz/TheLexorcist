package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a menu item in the navigation rail that is controlled by a user script.
 *
 * @param id A unique identifier for the menu item.
 * @param label The text to display on the menu item.
 * @param isVisible Controls whether the menu item is shown or hidden.
 * @param onClickFunction The name of a Javascript function to be executed when the item is clicked.
 *                          This is used for simple actions that don't open a new screen.
 * @param screenJson A JSON string defining a dynamic UI to be displayed when the item is clicked.
 *                   If this is present, it takes precedence over [onClickFunction].
 */
@Parcelize
data class ScriptedMenuItem(
    val id: String,
    val label: String,
    val isVisible: Boolean = true,
    val onClickFunction: String? = null,
    val screenJson: String? = null
) : Parcelable
