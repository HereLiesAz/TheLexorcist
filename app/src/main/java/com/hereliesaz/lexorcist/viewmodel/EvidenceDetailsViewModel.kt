package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.data.EvidenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.launch

@HiltViewModel
class EvidenceDetailsViewModel
    @Inject
    constructor(
        application: Application,
        private val evidenceRepository: EvidenceRepository,
    ) : AndroidViewModel(application) {
        fun removeEvidence(evidence: Evidence) {
            viewModelScope.launch {
                evidenceRepository.deleteEvidence(evidence)
            }
        }
    }
