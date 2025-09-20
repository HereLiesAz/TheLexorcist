package com.hereliesaz.lexorcist.service

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service for interacting with cloud-based generative AI models through the Firebase SDK.
 * This service provides capabilities for generating text content based on prompts.
 *
 * It is provided as a Singleton by Hilt to ensure a single instance manages the connection
 * to the generative model service.
 *
 * @param context The application context, injected by Hilt. Currently unused but kept for potential future needs like resource access.
 */
@Singleton
class GenerativeAIService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val generativeModel: GenerativeModel

    init {
        // Initialize the Gemini Pro model. This could be made configurable in the future.
        // The Firebase SDK handles authentication automatically, preferring Application Default Credentials (ADC)
        // if available, and falling back to the API key in the google-services.json file.
        generativeModel = GenerativeAI.getInstance().generativeModel("gemini-pro")
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
