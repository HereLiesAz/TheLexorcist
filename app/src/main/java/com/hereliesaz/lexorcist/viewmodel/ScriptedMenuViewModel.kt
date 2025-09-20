package com.hereliesaz.lexorcist.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import com.hereliesaz.lexorcist.service.ScriptedMenuStateService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptedMenuViewModel @Inject constructor(
    private val scriptedMenuStateService: ScriptedMenuStateService
) : ViewModel() {
    val menuItems = mutableStateListOf<ScriptedMenuItem>()

    init {
        // Example initial item - you might want to manage this dynamically
        // or ensure scripts always explicitly add/update items.
        menuItems.add(ScriptedMenuItem("scripted_item_1", "Scripted Item", false) { /* Default/placeholder action */ })

        viewModelScope.launch {
            scriptedMenuStateService.menuItemUpdates.collectLatest { update ->
                val index = menuItems.indexOfFirst { it.id == update.id }
                if (index != -1) {
                    var currentItem = menuItems[index]
                    update.label?.let { currentItem = currentItem.copy(text = it) }
                    update.isVisible?.let { currentItem = currentItem.copy(isVisible = it) }
                    // If you add onClickActionId handling in ScriptedMenuStateService and ScriptedMenuItem:
                    // update.onClickActionId?.let { /* Update onClick logic based on actionId */ }
                    menuItems[index] = currentItem
                } else {
                    // Optionally, handle cases where an update is for a non-existent ID,
                    // e.g., by adding it if all details are present.
                    // For now, we assume scripts will primarily update existing items or menuItems list is pre-populated.
                    if (update.label != null && update.isVisible != null) {
                         menuItems.add(ScriptedMenuItem(update.id, update.label, update.isVisible) { /* default action */ })
                    }
                }
            }
        }
    }

    // These functions can still be used for direct manipulation if needed,
    // but script-driven changes will now come via ScriptedMenuStateService.
    fun addMenuItem(id: String, text: String, onClick: () -> Unit) {
        if (menuItems.none { it.id == id }) {
            menuItems.add(ScriptedMenuItem(id, text, true, onClick))
        }
    }

    fun removeMenuItem(id: String) {
        menuItems.removeAll { it.id == id }
    }

    // These direct setters might conflict or be overridden by updates from the service.
    // Consider if they are still needed or if all state changes should flow via the service.
    fun setMenuItemVisibility(id: String, isVisible: Boolean) {
        val index = menuItems.indexOfFirst { it.id == id }
        if (index != -1) {
            menuItems[index] = menuItems[index].copy(isVisible = isVisible)
        }
    }

    fun setMenuItemLabel(id: String, label: String) {
        val index = menuItems.indexOfFirst { it.id == id }
        if (index != -1) {
            menuItems[index] = menuItems[index].copy(text = label)
        }
    }
}
