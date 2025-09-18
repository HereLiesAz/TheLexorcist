package com.hereliesaz.lexorcist.model

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class InProgress(val progress: Float) : ProcessingState()
    data class Completed(val result: String) : ProcessingState()
    data class Failure(val error: String) : ProcessingState()
}
