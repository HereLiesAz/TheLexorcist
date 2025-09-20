package com.hereliesaz.lexorcist.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MenuItemUpdate(
    val id: String,
    val label: String? = null,
    val isVisible: Boolean? = null,
    val onClickActionId: String? = null // Optional: For dynamic onClick actions if needed later
)

@Singleton
class ScriptedMenuStateService @Inject constructor() {

    private val _menuItemUpdates = MutableSharedFlow<MenuItemUpdate>(replay = 1) // Replay 1 to get last state on collect
    val menuItemUpdates: SharedFlow<MenuItemUpdate> = _menuItemUpdates.asSharedFlow()

    suspend fun updateLabel(itemId: String, label: String) {
        _menuItemUpdates.emit(MenuItemUpdate(id = itemId, label = label))
    }

    suspend fun updateVisibility(itemId: String, isVisible: Boolean) {
        _menuItemUpdates.emit(MenuItemUpdate(id = itemId, isVisible = isVisible))
    }

    // In case you want to set an action, e.g. from a script
    suspend fun setOnClickAction(itemId: String, actionId: String) {
        _menuItemUpdates.emit(MenuItemUpdate(id = itemId, onClickActionId = actionId))
    }
}
