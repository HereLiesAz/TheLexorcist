package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.TranscriptionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    application: Application,
    private val transcriptionService: TranscriptionService, // Injected
    private val credentialHolder: CredentialHolder      // Injected
) : AndroidViewModel(application) {

    fun setCredential(credential: GoogleAccountCredential) {
        // Use the injected CredentialHolder
        this.credentialHolder.credential = credential
        // TranscriptionService is already injected and will use the updated CredentialHolder
    }

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState

    fun transcribeAudio(uri: Uri) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Loading
            val resultPair = transcriptionService.transcribeAudio(uri)
            val transcriptText = resultPair.first
            val message = resultPair.second

            if (message != null && message.startsWith("Error:")) {
                _transcriptionState.value = TranscriptionState.Error(message)
            } else {
                _transcriptionState.value = TranscriptionState.Success(transcriptText)
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
