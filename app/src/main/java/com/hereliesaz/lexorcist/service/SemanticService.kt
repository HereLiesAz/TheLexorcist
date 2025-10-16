package com.hereliesaz.lexorcist.service

import android.content.Context
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject

class SemanticService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private var textEmbedder: TextEmbedder? = null

    private suspend fun initialize() {
        if (textEmbedder == null) {
            withContext(dispatcherProvider.io) {
                val modelsDir = File(context.filesDir, "models")
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs()
                }
                val modelFile = File(modelsDir, "universal_sentence_encoder.tflite")
                if (!modelFile.exists()) {
                    val modelUrl = "https://tfhub.dev/google/lite-model/universal-sentence-encoder-lite/2/default/1?lite-format=tflite"
                    URL(modelUrl).openStream().use { input ->
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                textEmbedder = TextEmbedder.createFromFile(context, modelFile.absolutePath)
            }
        }
    }

    suspend fun calculateSimilarity(text1: String, text2: String): Float {
        initialize()
        val embeddings1 = textEmbedder?.embed(text1)
        val embeddings2 = textEmbedder?.embed(text2)

        if (embeddings1 != null && embeddings2 != null) {
            return TextEmbedder.cosineSimilarity(embeddings1.embeddingResult().embeddings()[0], embeddings2.embeddingResult().embeddings()[0]).toFloat()
        }
        return 0.0f
    }
}
