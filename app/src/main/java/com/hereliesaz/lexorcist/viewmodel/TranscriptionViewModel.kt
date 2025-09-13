package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.TranscriptionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TranscriptionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var transcriptionService: TranscriptionService? = null

    fun setCredential(credential: GoogleAccountCredential) {
        val credentialHolder = CredentialHolder()
        credentialHolder.credential = credential
        transcriptionService = TranscriptionService(getApplication(), credentialHolder)
    }

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState

    fun transcribeAudio(uri: Uri) {
        viewModelScope.launch {
            if (transcriptionService == null) {
                _transcriptionState.value = TranscriptionState.Error("Transcription service not initialized.")
                return@launch
            }
            _transcriptionState.value = TranscriptionState.Loading
            val transcript = transcriptionService!!.transcribeAudio(uri)
            if (transcript.startsWith("Error:")) {
                _transcriptionState.value = TranscriptionState.Error(transcript)
            } else {
                _transcriptionState.value = TranscriptionState.Success(transcript)
            }
        }
    }
}

sealed class TranscriptionState {
    object Idle : TranscriptionState()

    object Loading : TranscriptionState()

    data class Success(
        val transcript: String,
    ) : TranscriptionState()

    data class Error(
        val message: String,
    ) : TranscriptionState()
}
