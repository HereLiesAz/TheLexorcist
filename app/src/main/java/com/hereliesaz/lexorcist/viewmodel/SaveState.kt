package com.hereliesaz.lexorcist.viewmodel

sealed class SaveState {
    data object NONE : SaveState()
    data object SAVING : SaveState()
    data object SUCCESS : SaveState()
    data class FAILURE(val message: String) : SaveState()
}
