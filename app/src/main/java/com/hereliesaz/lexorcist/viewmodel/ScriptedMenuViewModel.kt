package com.hereliesaz.lexorcist.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel // ADDED
import com.hereliesaz.lexorcist.model.ScriptedMenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScriptedMenuViewModel @Inject constructor() : ViewModel() { // EXTEND ViewModel
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

    fun setMenuItemLabel(id: String, label: String) {
        val index = menuItems.indexOfFirst { it.id == id }
        if (index != -1) {
            menuItems[index] = menuItems[index].copy(text = label)
        }
    }

}
