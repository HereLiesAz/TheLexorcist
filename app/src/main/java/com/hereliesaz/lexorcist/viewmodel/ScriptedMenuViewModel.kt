package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sealed class to represent the navigation or action events triggered by menu items.
sealed class ScriptedMenuEvent {
    data class NavigateToScreen(val screenJson: String) : ScriptedMenuEvent()
    data class ExecuteJs(val functionName: String) : ScriptedMenuEvent()
}

@HiltViewModel
class ScriptedMenuViewModel @Inject constructor() : ViewModel() {

    // Holds the current list of scripted menu items.
    private val _menuItems = MutableStateFlow<List<ScriptedMenuItem>>(emptyList())
    val menuItems = _menuItems.asStateFlow()

    // A flow for sending one-time events from the ViewModel to the UI.
    private val _events = MutableSharedFlow<ScriptedMenuEvent>()
    val events = _events.asSharedFlow()

    /**
     * Adds a new menu item or updates an existing one with the same ID.
     * This is intended to be called from the ScriptRunner.
     */
    fun upsertMenuItem(
        id: String,
        label: String,
        isVisible: Boolean,
        onClickFunction: String?,
        screenJson: String?
    ) {
        _menuItems.update { currentItems ->
            val newItem = ScriptedMenuItem(id, label, isVisible, onClickFunction, screenJson)
            val existingIndex = currentItems.indexOfFirst { it.id == id }

            if (existingIndex != -1) {
                // Replace existing item
                currentItems.toMutableList().apply { this[existingIndex] = newItem }
            } else {
                // Add new item
                currentItems + newItem
            }
        }
    }

    /**
     * Removes a menu item by its ID.
     * Intended to be called from the ScriptRunner.
     */
    fun removeMenuItem(id: String) {
        _menuItems.update { currentItems ->
            currentItems.filterNot { it.id == id }
        }
    }

    /**
     * Handles the click event for a menu item.
     * It determines the correct action (navigate or execute JS) and emits an event.
     */
    fun onMenuItemClicked(item: ScriptedMenuItem) {
        viewModelScope.launch {
            // Priority is given to opening a screen.
            if (item.screenJson != null) {
                _events.emit(ScriptedMenuEvent.NavigateToScreen(item.screenJson))
            } else if (item.onClickFunction != null) {
                _events.emit(ScriptedMenuEvent.ExecuteJs(item.onClickFunction))
            }
        }
    }
}
