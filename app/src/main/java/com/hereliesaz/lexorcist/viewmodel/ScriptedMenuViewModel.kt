package com.hereliesaz.lexorcist.viewmodel

import androidx.compose.runtime.mutableStateListOf
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptedMenuViewModel @Inject constructor() {
    val menuItems = mutableStateListOf<ScriptedMenuItem>()

    fun addMenuItem(id: String, text: String, onClick: () -> Unit) {
        menuItems.add(ScriptedMenuItem(id, text, true, onClick))
    }

    fun removeMenuItem(id: String) {
        menuItems.removeAll { it.id == id }
    }

    fun setMenuItemVisibility(id: String, isVisible: Boolean) {
        val index = menuItems.indexOfFirst { it.id == id }
        if (index != -1) {
            menuItems[index] = menuItems[index].copy(isVisible = isVisible)
        }
    }
}
