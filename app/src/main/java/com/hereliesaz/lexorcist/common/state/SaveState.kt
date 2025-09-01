package com.hereliesaz.lexorcist.common.state

/**
 * Represents the state of a save operation.
 * This is a sealed class, so it can only be one of the defined states.
 * This is used to communicate the state of a long-running operation from a ViewModel to the UI.
 */
sealed class SaveState {
    /** The operation is idle and has not started yet. */
    object Idle : SaveState()
    /** The operation is in progress. */
    object Saving : SaveState()
    /** The operation has completed successfully. */
    object Success : SaveState()
    /** The operation has failed with an error. */
    data class Error(val message: String) : SaveState()
}
