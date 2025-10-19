package com.hereliesaz.lexorcist.service

import android.content.Context
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private var textEmbedder: TextEmbedder? = null

    private suspend fun initialize() {
        if (textEmbedder == null) {
            withContext(dispatcherProvider.io()) {
                val modelFile = File(context.filesDir, "universal_sentence_encoder.tflite")
                if (!modelFile.exists()) {
                    context.assets.open("universal_sentence_encoder.tflite").use { input ->
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

    suspend fun getEmbedding(text: String): com.google.mediapipe.tasks.components.containers.Embedding? {
        initialize()
        val embeddings = textEmbedder?.embed(text)
        return embeddings?.embeddingResult()?.embeddings()?.get(0)
    }
}