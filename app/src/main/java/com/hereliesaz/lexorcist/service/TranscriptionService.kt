package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.protobuf.ByteString
import com.hereliesaz.lexorcist.auth.CredentialHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val credentialHolder: CredentialHolder,
        private val logService: LogService,
    ) {
        suspend fun transcribeAudio(uri: Uri): Pair<String, String?> {
            logService.addLog("Starting audio transcription...")
            try {
                logService.addLog("Getting credentials...")
                val credential =
                    credentialHolder.credential
                        ?: return Pair("", "Error: Could not retrieve access token.")
                val accessToken = credential.token
                val googleCredentials = GoogleCredentials.create(AccessToken(accessToken, null))
                val speechSettings =
                    SpeechSettings
                        .newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
                        .build()
                logService.addLog("Creating SpeechClient...")
                SpeechClient.create(speechSettings).use { speechClient ->
                    logService.addLog("Reading audio file...")
                    val audioBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (audioBytes == null) {
                        logService.addLog("Error: Could not read audio file.", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                        return Pair("", "Error: Could not read audio file.")
                    }
                    logService.addLog("Audio file read successfully. Size: ${audioBytes.size} bytes.")

                    val audio =
                        RecognitionAudio.newBuilder()
                            .setContent(ByteString.copyFrom(audioBytes))
                            .build()

                    val config =
                        RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED)
                            .setSampleRateHertz(16000)
                            .setLanguageCode("en-US")
                            .build()

                    val request =
                        RecognizeRequest
                            .newBuilder()
                            .setConfig(config)
                            .setAudio(audio)
                            .build()

                    logService.addLog("Sending transcription request...")
                    val response = speechClient.recognize(request)
                    val results = response.resultsList
                    logService.addLog("Transcription request complete.")

                    if (results.isNotEmpty()) {
                        val transcript = results[0].alternativesList[0].transcript
                        logService.addLog("Transcription successful. Found ${transcript.length} characters.")
                        return Pair(transcript, null)
                    }
                }
            } catch (e: Exception) {
                logService.addLog("Error during transcription: ${e.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                e.printStackTrace()
                return Pair("", "Error during transcription: ${e.message}")
            }
            logService.addLog("No transcription result.", com.hereliesaz.lexorcist.model.LogLevel.DEBUG)
            return Pair("", "No transcription result.")
        }
    }
