package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.ExifUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import android.graphics.ImageDecoder

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class OcrViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var ocrViewModel: OcrViewModel
    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var scriptRunner: ScriptRunner
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        evidenceRepository = mockk(relaxed = true)
        settingsManager = mockk(relaxed = true)
        scriptRunner = mockk(relaxed = true)
        ocrViewModel = OcrViewModel(application, evidenceRepository, settingsManager, scriptRunner)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performOcrOnUri returns evidence with correct data`() = runTest {
        // Given
        val uri: Uri = mockk()
        val bitmap: Bitmap = mockk()
        mockkStatic(ImageDecoder::class)
        every { ImageDecoder.decodeBitmap(any<ImageDecoder.Source>()) } returns bitmap
        mockkStatic(ExifUtils::class)
        every { ExifUtils.getExifDate(any(), any()) } returns 0L
        val caseId = 1
        val parentVideoId = "video1"

        // When
        val evidence = ocrViewModel.performOcrOnUri(uri, context, caseId, parentVideoId)

        // Then
        assertEquals(caseId.toLong(), evidence.caseId)
        assertEquals(parentVideoId, evidence.parentVideoId)
        assertEquals(uri.toString(), evidence.sourceDocument)
    }
}
