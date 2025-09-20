package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // ADDED
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import com.hereliesaz.lexorcist.service.ScriptRunner // ADDED
import com.hereliesaz.lexorcist.service.ScriptRunnerUiAction // ADDED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest // ADDED
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch // ADDED
import javax.inject.Inject

@HiltViewModel
class ScriptedMenuViewModel @Inject constructor(
    private val scriptRunner: ScriptRunner // ADDED
) : ViewModel() {

    private val _menuItems = MutableStateFlow<List<ScriptedMenuItem>>(emptyList())
    val menuItems = _menuItems.asStateFlow()

    private val _navigationActions = MutableSharedFlow<String>()
    val navigationActions = _navigationActions.asSharedFlow()

    // ADDED: init block to collect actions from ScriptRunner
    init {
        viewModelScope.launch {
            scriptRunner.uiActions.collectLatest { action ->
                when (action) {
                    is ScriptRunnerUiAction.AddOrUpdate -> {
                        addOrUpdateMenuItem(action.id, action.label, action.isVisible, action.onClickAction)
                    }
                    is ScriptRunnerUiAction.Remove -> {
                        removeMenuItem(action.id)
                    }
                    ScriptRunnerUiAction.ClearAll -> {
                        clearAllMenuItems()
                    }
                }
            }
        }
    }

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
                mutableList[index] = newItem
            } else {
                mutableList.add(newItem)
            }
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
