package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1p1beta1.* // Or v1 depending on features needed
import com.hereliesaz.lexorcist.R // Assuming your service account JSON is in res/raw
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.model.LogLevel // Your custom LogLevel
import com.hereliesaz.lexorcist.utils.Result // Your custom Result class
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCloudTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService // Assuming you still want to use your LogService
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
            // IMPORTANT: Securely load your service account JSON.
            // Bundling directly in res/raw is okay for development,
            // but for production, consider more secure ways if possible.
            context.resources.openRawResource(R.raw.your_service_account_json_file).use { stream ->
                val credentials = GoogleCredentials.fromStream(stream)
                val speechSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()
                speechClient = SpeechClient.create(speechSettings)
                logService.addLog("GoogleCloudTranscriptionService: SpeechClient initialized successfully.")
                _processingState.value = ProcessingState.Idle
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to initialize Google SpeechClient: ${e.message}"
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            speechClient = null
        }
    }

    override suspend fun start(uri: Uri) {
        // For Google Cloud Speech, 'start' might be less relevant for batch transcription.
        // If you were doing streaming, this is where you'd set up the stream.
        // For now, let's treat it like a pre-step for transcribeAudio or make it a no-op.
        logService.addLog("GoogleCloudTranscriptionService: start called for $uri. For batch, transcribeAudio is primary.", LogLevel.INFO)
        if (speechClient == null) {
            initializeSpeechClient() // Try to re-initialize if it failed before
        }
    }

    override fun stop() {
        // If you implement streaming transcription, this is where you'd close the stream.
        logService.addLog("GoogleCloudTranscriptionService: stop called.", LogLevel.INFO)
    }

    override suspend fun transcribeAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        if (speechClient == null) {
            val errorMsg = "SpeechClient not initialized."
            logService.addLog(errorMsg, LogLevel.ERROR)
            _processingState.value = ProcessingState.Failure(errorMsg)
            return@withContext Result.Error(IllegalStateException(errorMsg))
        }

        _processingState.value = ProcessingState.InProgress(0.0f)
        logService.addLog("GoogleCloudTranscriptionService: Transcribing audio from $uri")

        try {
            val audioBytes: com.google.protobuf.ByteString
            context.contentResolver.openInputStream(uri).use { inputStream ->
                if (inputStream == null) {
                    val errorMsg = "Failed to open audio stream from URI: $uri"
                    logService.addLog(errorMsg, LogLevel.ERROR)
                    _processingState.value = ProcessingState.Failure(errorMsg)
                    return@withContext Result.Error(java.io.IOException(errorMsg))
                }
                audioBytes = com.google.protobuf.ByteString.readFrom(inputStream)
            }
            _processingState.value = ProcessingState.InProgress(0.25f) // Indicate progress

            val audio = RecognitionAudio.newBuilder().setContent(audioBytes).build()

            // Configure request to enable automatic punctuation and specify language
            // You'll need to determine sample rate and encoding from the audio file
            // or make it configurable. For simplicity, assuming 16000Hz and LINEAR16.
            val config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // Or OGG_OPUS, FLAC, etc.
                .setSampleRateHertz(16000) // Adjust as needed
                .setLanguageCode("en-US") // Adjust as needed
                .setEnableAutomaticPunctuation(true)
                // .setModel("telephony") // or "medical_dictation", "latest_long", etc.
                .build()
            _processingState.value = ProcessingState.InProgress(0.5f)

            logService.addLog("GoogleCloudTranscriptionService: Sending request to Google Cloud Speech API.")
            val response = speechClient!!.recognize(config, audio)
            _processingState.value = ProcessingState.InProgress(0.75f)

            val transcription = StringBuilder()
            for (result in response.resultsList) {
                // The first alternative is generally the best one.
                if (result.alternativesCount > 0) {
                    transcription.append(result.alternativesList[0].transcript).append("\n")
                }
            }
            _processingState.value = ProcessingState.InProgress(1.0f)

            if (transcription.isNotBlank()) {
                val resultText = transcription.toString().trim()
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
            e.printStackTrace() // For detailed debugging
            _processingState.value = ProcessingState.Failure(errorMsg)
            Result.Error(e)
        }
    }

    // Consider adding a method to explicitly shut down the speechClient when the service is destroyed
    // if it were tied to an Android lifecycle component, but as a Singleton,
    // it will live as long as the app. speechClient.shutdown() and speechClient.awaitTermination()
    // could be called in an @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY) if this were a ViewModel
    // or similar, or potentially in Application.onTerminate for a true singleton.
    // For now, relying on process termination to clean up.
}
