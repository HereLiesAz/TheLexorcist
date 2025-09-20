package com.hereliesaz.lexorcist.service

import android.content.Context
import com.google.firebase.ai.client.generativeai.FirebaseGenerativeAI
import com.google.firebase.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerativeAIService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val generativeModel: GenerativeModel

    init {
        // For now, we'll use the Gemini Pro model. This can be configured later.
        // Authentication will be handled automatically by the SDK,
        // preferring ADC if available, then falling back to the API key
        // from the google-services.json file.
        generativeModel = FirebaseGenerativeAI.getInstance().generativeModel("gemini-pro")
    }

    suspend fun generateContent(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No response text found."
        } catch (e: Exception) {
            // In a real app, we'd have more robust error handling
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
