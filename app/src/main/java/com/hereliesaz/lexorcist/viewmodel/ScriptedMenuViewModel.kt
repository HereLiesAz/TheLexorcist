package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ScriptedMenuViewModel @Inject constructor() : ViewModel() {

    // Expose an immutable StateFlow of the menu items to the UI.
    private val _menuItems = MutableStateFlow<List<ScriptedMenuItem>>(emptyList())
    val menuItems = _menuItems.asStateFlow()

    // Use a SharedFlow to send one-off navigation events to the UI.
    private val _navigationActions = MutableSharedFlow<String>()
    val navigationActions = _navigationActions.asSharedFlow()

    /**
     * Adds a new menu item or updates an existing one with the same ID.
     * This provides a simple "upsert" functionality for scripts.
     */
    fun addOrUpdateMenuItem(id: String, text: String, isVisible: Boolean, onClickAction: String?) {
        _menuItems.update { currentList ->
            val mutableList = currentList.toMutableList()
            val index = mutableList.indexOfFirst { it.id == id }
            val newItem = ScriptedMenuItem(id, text, isVisible, onClickAction)

            if (index != -1) {
                // Replace the existing item at the found index.
                mutableList[index] = newItem
            } else {
                // Add the new item to the end of the list.
                mutableList.add(newItem)
            }
            // Return the updated list to trigger the StateFlow.
            mutableList
        }
    }

    /**
     * Removes a menu item from the list by its unique ID.
     */
    fun removeMenuItem(id: String) {
        _menuItems.update { currentItems ->
            currentItems.filterNot { it.id == id }
        }
    }

    /**
     * Clears all dynamically added menu items, resetting the state.
     */
    fun clearAllMenuItems() {
        _menuItems.update { emptyList() }
    }

    /**
     * Called by the UI when a menu item is clicked.
     * This function emits the associated action string to the navigation flow,
     * which the UI layer will observe to perform the navigation.
     */
    suspend fun onMenuItemClicked(action: String) {
        _navigationActions.emit(action)
    }
}
