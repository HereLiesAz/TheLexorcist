package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel
@Inject
constructor(
    private val application: Application,
    private val evidenceRepository: EvidenceRepository,
) : AndroidViewModel(application) {

    fun performOcrOnUri(uri: Uri, context: Context, caseId: Long, parentVideoId: String?) {
        viewModelScope.launch {
            val evidence = Evidence(
                caseId = caseId,
                parentVideoId = parentVideoId,
                spreadsheetId = "",
                type = "image",
                content = "",
                formattedContent = null,
                mediaUri = uri.toString(),
                timestamp = System.currentTimeMillis(),
                sourceDocument = "",
                documentDate = 0L,
                allegationId = null,
                category = "",
                tags = emptyList()
            )
            evidenceRepository.addEvidence(evidence)
        }
    }
}
