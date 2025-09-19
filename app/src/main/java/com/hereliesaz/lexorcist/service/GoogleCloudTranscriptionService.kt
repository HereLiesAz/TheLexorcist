package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeResponse // Specific import
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.protobuf.ByteString // Specific import
import com.hereliesaz.lexorcist.R // For R.raw access
import com.hereliesaz.lexorcist.model.LogLevel
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException // Specific import for clarity
import javax.inject.Inject
import javax.inject.Singleton

// **IMPORTANT**: Replace this with the actual name of your service account JSON file in `res/raw/`
private const val SERVICE_ACCOUNT_JSON_RESOURCE_NAME = "your_service_account_key" // e.g., "google_cloud_credentials"

@Singleton
class GoogleCloudTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService
) : TranscriptionService {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var speechClient: SpeechClient? = null

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    override val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    init {
        initializeSpeechClient()
    }

    private fun initializeSpeechClient() {
        try {
            // Attempt to get the resource ID for the service account JSON.
            // This makes the filename configurable via the constant above.
            val resourceId = context.resources.getIdentifier(
                SERVICE_ACCOUNT_JSON_RESOURCE_NAME,
                "raw",
                context.packageName
            )

            if (resourceId == 0) {
                val errorMsg = "Service account JSON file '$SERVICE_ACCOUNT_JSON_RESOURCE_NAME.json' not found in res/raw/. Please add it and configure the SERVICE_ACCOUNT_JSON_RESOURCE_NAME constant."
                logService.addLog(errorMsg, LogLevel.ERROR)
                _processingState.value = ProcessingState.Failure(errorMsg)
                speechClient = null
                return
            }

            context.resources.openRawResource(resourceId).use { stream ->
                val credentials = GoogleCredentials.fromStream(stream)
                val speechSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()
                speechClient = SpeechClient.create(speechSettings)
                logService.addLog("GoogleCloudTranscriptionService: SpeechClient initialized successfully.")
                _processingState.value = ProcessingState.Idle // Set to Idle after successful init
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to initialize Google SpeechClient: ${e.message}"
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            speechClient = null
            e.printStackTrace()
        }
    }

    override suspend fun start(uri: Uri) {
        logService.addLog("GoogleCloudTranscriptionService: start called for $uri.", LogLevel.INFO)
        if (speechClient == null) {
            logService.addLog("GoogleCloudTranscriptionService: SpeechClient was null, re-initializing...", LogLevel.INFO)
            initializeSpeechClient()
        }
    }

    override fun stop() {
        logService.addLog("GoogleCloudTranscriptionService: stop called.", LogLevel.INFO)
        // Consider closing/shutting down speechClient if appropriate for your app's lifecycle
        // speechClient?.shutdown()
        // speechClient?.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)
    }

    override suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val currentSpeechClient = speechClient
        if (currentSpeechClient == null) {
            val errorMsg = "SpeechClient not initialized. Attempting to re-initialize."
            logService.addLog(errorMsg, LogLevel.ERROR)
            initializeSpeechClient() // Attempt re-initialization
            if (speechClient == null) { // Check again after attempt
                 _processingState.value = ProcessingState.Failure("SpeechClient still not initialized after re-attempt.")
                return@withContext Result.Error(IllegalStateException("SpeechClient not initialized."))
            }
             // if re-initialization was successful, update currentSpeechClient
            // but for safety, it's better to return and let the user retry if it was null initially
             _processingState.value = ProcessingState.Failure(errorMsg) // Reflect the initial problem
            return@withContext Result.Error(IllegalStateException(errorMsg))
        }

        _processingState.value = ProcessingState.InProgress(0.0f)
        logService.addLog("GoogleCloudTranscriptionService: Transcribing audio from $uri")

        try {
            val audioBytes: ByteString
            context.contentResolver.openInputStream(uri).use { inputStream ->
                if (inputStream == null) {
                    val errorMsg = "Failed to open audio stream from URI: $uri"
                    logService.addLog(errorMsg, LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure(errorMsg)
                    return@withContext Result.Error(IOException(errorMsg))
                }
                audioBytes = ByteString.readFrom(inputStream)
            }
            _processingState.value = ProcessingState.InProgress(0.25f)

            val audio = recognitionAudio {
                this.content = audioBytes
            }
            val config = recognitionConfig {
                this.encoding =
                    RecognitionConfig.AudioEncoding.LINEAR16// Ensure this matches your audio
                this.sampleRateHertz = 16000// Ensure this matches your audio
                this.languageCode = "en-US"// Adjust as needed
                this.enableAutomaticPunctuation = true
            }
            _processingState.value = ProcessingState.InProgress(0.5f)

            logService.addLog("GoogleCloudTranscriptionService: Sending request to Google Cloud Speech API.")
            // Use the local, null-checked speechClient
            val response: RecognizeResponse = currentSpeechClient.recognize(config, audio)
            _processingState.value = ProcessingState.InProgress(0.75f)

            val transcription = StringBuilder()
            for (result in response.resultsList) {
                if (result.alternativesCount > 0) {
                    transcription.append(result.alternativesList[0].transcript).append("\n")
                }
            }
            _processingState.value = ProcessingState.InProgress(1.0f)

            val resultText = transcription.toString().trim()
            if (resultText.isNotBlank()) {
                logService.addLog("GoogleCloudTranscriptionService: Transcription successful. Text: $resultText")
                _processingState.value = ProcessingState.Completed(resultText)
                Result.Success(resultText)
            } else {
                logService.addLog("GoogleCloudTranscriptionService: Transcription returned no text.", LogLevel.WARNING)
                _processingState.value = ProcessingState.Completed("") // Or Failure
                Result.Success("") // Or an error indicating no transcription
            }

        } catch (e: Exception) {
            val errorMsg = "Google Cloud Speech transcription failed: ${e.message}"
            logService.addLog(errorMsg, LogLevel.ERROR)
            e.printStackTrace()
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(e)
        }
    }
}
