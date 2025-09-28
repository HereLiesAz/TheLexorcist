package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.service.GlobalLoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        application: Application,
        private val evidenceRepository: EvidenceRepository,
        private val caseRepository: CaseRepository,
        private val credentialHolder: CredentialHolder, // Changed to CredentialHolder
    ) : AndroidViewModel(application) {
        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _importContact = MutableStateFlow<String?>(null)
        val importContact: StateFlow<String?> = _importContact.asStateFlow()

        private val _importStartDate = MutableStateFlow<Long?>(null)
        val importStartDate: StateFlow<Long?> = _importStartDate.asStateFlow()

        private val _importEndDate = MutableStateFlow<Long?>(null)
        val importEndDate: StateFlow<Long?> = _importEndDate.asStateFlow()

        private val _shouldImportSms = MutableStateFlow(false)
        val shouldImportSms: StateFlow<Boolean> = _shouldImportSms.asStateFlow()

        private val _shouldImportCalls = MutableStateFlow(false)
        val shouldImportCalls: StateFlow<Boolean> = _shouldImportCalls.asStateFlow()

        fun setImportFilters(contact: String?, startDate: Long?, endDate: Long?, importSms: Boolean, importCalls: Boolean) {
            _importContact.value = contact
            _importStartDate.value = startDate
            _importEndDate.value = endDate
            _shouldImportSms.value = importSms
            _shouldImportCalls.value = importCalls
        }

        fun showLoading() {
            _isLoading.value = true
        }

        fun hideLoading() {
            _isLoading.value = false
        }
    }
