package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.data.EvidenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EvidenceDetailsViewModel
    @Inject
    constructor(
        application: Application,
        private val evidenceRepository: EvidenceRepository,
    ) : AndroidViewModel(application) {
        // ... all the other code from the original file
    }
