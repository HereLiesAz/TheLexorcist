package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.data.CaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CaseViewModel @Inject constructor(
    application: Application,
    private val caseRepository: CaseRepository
) : AndroidViewModel(application) {
    // ... all the other code from the original file
}
