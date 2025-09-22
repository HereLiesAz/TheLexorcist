package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.service.GlobalLoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel
@Inject
constructor(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val credentialHolder: CredentialHolder, // Changed to CredentialHolder
    globalLoadingState: GlobalLoadingState,
) : AndroidViewModel(application) {
    val isLoading: StateFlow<Boolean> =
        globalLoadingState.isLoading.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )
}
