package com.hereliesaz.lexorcist.service

import android.content.Context
import android.content.ContentResolver
import android.content.res.AssetManager // Added for mocking AssetManager
import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result // Your Result class
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After // Added for cleaning up static mock
import org.junit.Rule // Added for TemporaryFolder
import org.junit.Test
import org.junit.rules.TemporaryFolder // Added for TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic // Added for static mocking
import org.mockito.Mockito // Added for Mockito.mockStatic and Mockito.RETURNS_DEEP_STUBS
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TranscriptionServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockLogService: LogService

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var mockParsedUri: Uri

    @Mock
    private lateinit var mockInputStream: InputStream

    @Mock
    private lateinit var mockAssetManager: AssetManager

    private lateinit var voskTranscriptionService: VoskTranscriptionService
    private lateinit var testAudioUriString: String
    private lateinit var mockedStaticUri: MockedStatic<Uri>
    private lateinit var testAppFilesDir: File

    @Before
    fun setUp() {
        testAudioUriString = "content://com.example.provider/audio.wav"

        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testAudioUriString) }.thenReturn(mockParsedUri)

        whenever(mockParsedUri.toString()).thenReturn(testAudioUriString)

        testAppFilesDir = tempFolder.newFolder("appfiles")
        whenever(mockContext.filesDir).thenReturn(testAppFilesDir)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContext.assets).thenReturn(mockAssetManager)

        // This mock is intended to make copyModelFromAssets() in VoskTranscriptionService return null,
        // forcing it to attempt the download path.
        whenever(mockAssetManager.list(any())).thenReturn(emptyArray<String>())
        // To ensure open() also fails if list() wasn't enough (e.g. if an empty dir was "found")
        whenever(mockAssetManager.open(any())).thenThrow(IOException("Mock AssetManager open failed"))

        voskTranscriptionService = VoskTranscriptionService(mockContext, mockLogService)
    }

    @After
    fun tearDown() {
        mockedStaticUri.close()
    }

    @Test
    fun `transcribeAudio when contentResolver throws FileNotFoundException should return Error`() = runTest {
        val expectedExceptionMessage = "File not found for URI: $testAudioUriString"
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri)))
            .thenThrow(FileNotFoundException("Mock FNF Exception"))

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            assertTrue("Expected exception to be IOException, but was ${result.exception::class.java}", result.exception is IOException)
            assertEquals("Exception message mismatch", expectedExceptionMessage, result.exception.message)
            assertTrue("Expected cause to be FileNotFoundException", result.exception.cause is FileNotFoundException)
        }
    }

    @Test
    fun `transcribeAudio when contentResolver returns null InputStream should return Error`() = runTest {
        val expectedExceptionMessage = "Failed to open audio stream from URI: $testAudioUriString"
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri))).thenReturn(null)

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            assertTrue("Expected exception to be IOException, but was ${result.exception::class.java}", result.exception is IOException)
            assertEquals("Exception message mismatch", expectedExceptionMessage, result.exception.message)
        }
    }
}
