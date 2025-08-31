package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    // This ViewModel is being refactored.
    // Its functionality is being moved to more specific ViewModels
    // like AuthViewModel, CaseViewModel, and EvidenceViewModel.
}
