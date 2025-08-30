package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.android.gms.tasks.Tasks
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
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
        ocrViewModel = OcrViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startImageReview with valid uri sets imageBitmapForReview`() = runTest {
        // Given
        val uri: Uri = mockk()
        val bitmap: Bitmap = mockk()
        mockkStatic(ImageDecoder::class)
        every { ImageDecoder.decodeBitmap(any<ImageDecoder.Source>()) } returns bitmap

        // When
        ocrViewModel.startImageReview(uri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNotNull(ocrViewModel.imageBitmapForReview.value)
    }

    @Test
    fun `confirmImageReview with valid bitmap adds evidence to uiEvidenceList`() = runTest {
        // Given
        val uri: Uri = mockk()
        val bitmap: Bitmap = mockk(relaxed = true)
        mockkStatic(ImageDecoder::class)
        every { ImageDecoder.decodeBitmap(any<ImageDecoder.Source>()) } returns bitmap

        val textRecognizer: TextRecognizer = mockk()
        val text: Text = mockk(relaxed = true)
        mockkStatic(TextRecognition::class)
        every { TextRecognition.getClient(any()) } returns textRecognizer
        every { textRecognizer.process(any<InputImage>()) } returns Tasks.forResult(text)

        ocrViewModel.startImageReview(uri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        ocrViewModel.confirmImageReview(context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNotNull(ocrViewModel.newlyCreatedEvidence.value)
        assertEquals(text.text, ocrViewModel.newlyCreatedEvidence.value?.content)
    }
}
