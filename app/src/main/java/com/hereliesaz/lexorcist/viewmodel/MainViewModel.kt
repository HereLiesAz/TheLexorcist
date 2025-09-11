package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
        private val googleApiService: GoogleApiService?,
    ) : AndroidViewModel(application)

