package com.hereliesaz.lexorcist.service

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service for interacting with the cloud-based Google AI developer API (Gemini).
 * This service is configured to use the free-tier backend, not the paid Vertex AI backend.
 */
@Singleton
class GenerativeAIService @Inject constructor() {

    // Initialize the generative model using the Google AI backend.
    // The model name "gemini-2.5-flash" is used as per the latest documentation.
    private val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash")

    /**
     * Generates text content from the remote generative model based on a given prompt.
     *
     * @param prompt The text prompt to send to the model.
     * @return The generated text as a String, or an error message if the call fails.
     */
    suspend fun generateContent(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No response text found."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
