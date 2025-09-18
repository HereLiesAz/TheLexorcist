package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.TranscriptionService
import com.hereliesaz.lexorcist.utils.Result // Import Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    application: Application,
    private val transcriptionService: TranscriptionService,
    private val credentialHolder: CredentialHolder
) : AndroidViewModel(application) {

    // Credential setting might be useful if credentials are obtained/updated after ViewModel creation.
    fun setCredential(credential: GoogleAccountCredential) {
        this.credentialHolder.credential = credential
    }

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState

    fun transcribeAudio(uri: Uri) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Loading
            // Call the suspend function that returns Result<String>
            when (val result = transcriptionService.transcribeAudio(uri)) {
                is Result.Success -> {
                    _transcriptionState.value = TranscriptionState.Success(result.data)
                }
                is Result.Error -> {
                    _transcriptionState.value = TranscriptionState.Error(result.exception.message ?: "Unknown transcription error")
                }
                is Result.UserRecoverableError -> {
                    // For TranscriptionViewModel, we might just show an error.
                    // If we needed to propagate the intent, we'd need a mechanism here.
                    _transcriptionState.value = TranscriptionState.Error(result.exception.message ?: "User recoverable transcription error")
                }
                is Result.Loading -> {
                    // This state should ideally be handled by observing transcriptionService.processingState if it's a long operation.
                    // If transcribeAudio directly returns Loading, it means it didn't complete.
                    // For now, we'll keep it in Loading or revert to an error if this is unexpected.
                    _transcriptionState.value = TranscriptionState.Loading // Or Error("Transcription did not complete")
                }
            }
        }
    }
}

sealed class TranscriptionState {
    object Idle : TranscriptionState()
    object Loading : TranscriptionState()
    data class Success(val transcript: String) : TranscriptionState()
    data class Error(val message: String) : TranscriptionState()
}
