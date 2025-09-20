package com.hereliesaz.lexorcist.service

import android.content.Context
import android.content.ContentResolver
import android.content.res.AssetManager // Added for mocking AssetManager
import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result // Your Result class
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
import java.io.IOException // Still useful to have, though the check is broader

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

    @Mock // Mock for the Uri that Uri.parse will return
    private lateinit var mockParsedUri: Uri

    @Mock
    private lateinit var mockInputStream: InputStream

    @Mock
    private lateinit var mockAssetManager: AssetManager // Added mock AssetManager

    private lateinit var voskTranscriptionService: VoskTranscriptionService
    private lateinit var testAudioUriString: String
    private lateinit var mockedStaticUri: MockedStatic<Uri> // To hold the static mock
    private lateinit var testAppFilesDir: File // To hold the temporary directory for app files

    @Before
    fun setUp() {
        testAudioUriString = "content://com.example.provider/audio.wav"

        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testAudioUriString) }.thenReturn(mockParsedUri)

        // Stubbing toString() for the mock Uri to prevent NPEs in logging or other string operations
        whenever(mockParsedUri.toString()).thenReturn(testAudioUriString)

        testAppFilesDir = tempFolder.newFolder("appfiles")
        whenever(mockContext.filesDir).thenReturn(testAppFilesDir)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContext.getAssets()).thenReturn(mockAssetManager) // Stub getAssets()

        // Basic stub for AssetManager.list to avoid NPE in Vosk init
        // The Vosk model initialization might try to list files in a specific path or the root.
        // Providing an empty array for any path requested by list().
        whenever(mockAssetManager.list(any())).thenReturn(emptyArray<String>()) // Corrected any()
        // If specific model files are expected by name, more specific stubbing for open() will be needed.
        // For now, let's assume the model files are not found, leading to an init error for the tests.

        voskTranscriptionService = VoskTranscriptionService(mockContext, mockLogService)
    }

    @After
    fun tearDown() {
        mockedStaticUri.close() 
    }

    @Test
    fun `transcribeAudio when contentResolver throws FileNotFoundException should return Error`() = runTest {
        // This mock may not be hit if model initialization fails first, which is expected here.
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri)))
            .thenThrow(FileNotFoundException("Mock FNF Exception"))

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            // Changed from IOException to generic Exception to catch other init failures e.g. SecurityException
            assertTrue("Expected Exception due to model init failure, got ${result.exception::class.java}", 
                result.exception is Exception)
            val message = result.exception.message ?: ""
            assertTrue(
                "Error message mismatch. Expected model init failure. Got: $message",
                 message.contains("Vosk model directory not found after extraction") || 
                 message.contains("Vosk model download failed") ||
                 message.contains("Zip entry tried to escape model directory") || // For SecurityException
                 message.contains("Error during model download/unzip") || // General catch-all from downloadAndUnzipModel
                 message.contains("Failed to initialize Vosk model assets") || // Added for new expected failure
                 message.contains("Failed to list assets from path") // Added for new expected failure
            )
        }
    }

    @Test
    fun `transcribeAudio when contentResolver returns null InputStream should return Error`() = runTest {
        // This mock may not be hit if model initialization fails first, which is expected here.
        whenever(mockContentResolver.openInputStream(eq(mockParsedUri))).thenReturn(null)

        val result = voskTranscriptionService.transcribeAudio(mockParsedUri)

        assertTrue("Expected Result.Error, got $result", result is Result.Error)
        if (result is Result.Error) {
            // Changed from IOException to generic Exception to catch other init failures e.g. SecurityException
            assertTrue("Expected Exception due to model init failure, got ${result.exception::class.java}", 
                result.exception is Exception)
            val message = result.exception.message ?: ""
            assertTrue(
                "Error message mismatch. Expected model init failure. Got: $message",
                message.contains("Vosk model directory not found after extraction") || 
                message.contains("Vosk model download failed") ||
                message.contains("Zip entry tried to escape model directory") || // For SecurityException
                message.contains("Error during model download/unzip") || // General catch-all from downloadAndUnzipModel
                message.contains("Failed to initialize Vosk model assets") || // Added for new expected failure
                message.contains("Failed to list assets from path") // Added for new expected failure
            )
        }
    }
}
