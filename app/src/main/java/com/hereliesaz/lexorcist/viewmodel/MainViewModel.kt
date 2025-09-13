package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.service.GoogleApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        application: Application,
        private val evidenceRepository: EvidenceRepository,
        private val caseRepository: CaseRepository,
        private val credentialHolder: CredentialHolder, // Changed to CredentialHolder
    ) : AndroidViewModel(application) {
        // Access googleApiService via credentialHolder if needed, e.g.:
        // private val googleApiService: GoogleApiService? = credentialHolder.googleApiService
    }
