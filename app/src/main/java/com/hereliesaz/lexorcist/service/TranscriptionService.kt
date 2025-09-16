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
    ) {
        suspend fun transcribeAudio(uri: Uri): String {
            try {
                val credential =
                    credentialHolder.credential
                        ?: return "Error: Could not retrieve access token."
                val accessToken = credential.token
                val googleCredentials = GoogleCredentials.create(AccessToken(accessToken, null))
                val speechSettings =
                    SpeechSettings
                        .newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
                        .build()

                SpeechClient.create(speechSettings).use { speechClient ->
                    val audioBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (audioBytes == null) {
                        return "Error: Could not read audio file."
                    }

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

                    val response = speechClient.recognize(request)
                    val results = response.resultsList

                    if (results.isNotEmpty()) {
                        return results[0].alternativesList[0].transcript
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return "Error during transcription: ${e.message}"
            }
            return "No transcription result."
        }
    }
