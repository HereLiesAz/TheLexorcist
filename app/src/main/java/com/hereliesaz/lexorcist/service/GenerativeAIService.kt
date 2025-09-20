package com.hereliesaz.lexorcist.service

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel // Specific import
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service for interacting with cloud-based generative AI models.
 * This service provides capabilities for generating text content based on prompts using the Google AI SDK.
 *
 * It is provided as a Singleton by Hilt to ensure a single instance manages the connection
 * to the generative model service.
 *
 * @param context The application context, injected by Hilt. Currently unused but kept for potential future needs like resource access or API key providers.
 */
@Singleton
class GenerativeAIService @Inject constructor(
    @ApplicationContext private val context: Context // Context might be used later for API key provider
) {

    private val generativeModel: GenerativeModel

    init {
        // Initialize the Gemini Pro model using the Google AI SDK.
        // IMPORTANT: Replace "YOUR_API_KEY_PLACEHOLDER" with your actual Gemini API key.
        // You should store your API key securely, e.g., in local.properties or a backend, and not hardcode it.
        generativeModel = GenerativeModel(
            modelName = "gemini-pro", 
            apiKey = "YOUR_API_KEY_PLACEHOLDER"
            // Optional: add safetySettings = listOf(...), generationConfig = generationConfig { ... } 
        )
    }

    /**
     * Generates text content from the remote generative model based on a given prompt.
     * This is a suspending function and must be called from a coroutine.
     *
     * @param prompt The text prompt to send to the model.
     * @return The generated text content as a String, or an error message if the call fails.
     */
    suspend fun generateContent(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No response text found."
        } catch (e: Exception) {
            // A simple error handling mechanism for now. In a production app, this should
            // use a more structured error reporting system.
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
