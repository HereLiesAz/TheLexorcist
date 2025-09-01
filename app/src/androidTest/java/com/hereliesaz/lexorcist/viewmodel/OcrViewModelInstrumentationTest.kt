package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.di.TestAppModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltAndroidTest
@UninstallModules(TestAppModule::class)
class OcrViewModelInstrumentationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var evidenceRepository: EvidenceRepository

    private lateinit var ocrViewModel: OcrViewModel
    private lateinit var application: Application

    @Before
    fun setup() {
        hiltRule.inject()
        application = ApplicationProvider.getApplicationContext()
        ocrViewModel = OcrViewModel(application, evidenceRepository, settingsManager, scriptRunner)
    }

    @Test
    fun performOcrOnUri_addsEvidenceWithCorrectDetails() = runTest {
        // Given
        val uri: Uri = Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/1000000033")
        val context: Context = application.applicationContext
        val caseId = 123
        val parentVideoId = "video-456"
        val expectedTimestamp = 1672531200000L
        val expectedDocumentDate = 1672521200000L

        // When
        ocrViewModel.performOcrOnUri(uri, context, caseId, parentVideoId)

        // Then
        verify {
            evidenceRepository.addEvidence(withArg { evidence ->
                org.junit.Assert.assertEquals(uri.toString(), evidence.sourceDocument)
                org.junit.Assert.assertEquals(caseId.toLong(), evidence.caseId)
                org.junit.Assert.assertEquals(parentVideoId, evidence.parentVideoId)
                // Timestamps will be checked in the unit test where we can control the clock
            })
        }
    }
}
