package com.hereliesaz.lexorcist.service.nlp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service that encapsulates the logic for interacting with the Legal-BERT TFLite model.
 * It handles model loading, tokenization, and inference to provide text embeddings.
 *
 * This class is provided as a Singleton by Hilt, ensuring that the model and tokenizer
 * are only initialized once.
 *
 * @param context The application context, injected by Hilt, used for asset loading.
 */
@Singleton
class LegalBertService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private lateinit var interpreter: Interpreter
    private lateinit var tokenizer: LegalBertTokenizer

    private val maxSeqLength = 128 // Standard sequence length for BERT models.

    init {
        try {
            tokenizer = LegalBertTokenizer(context)
            interpreter = Interpreter(loadModelFile())
        } catch (e: IOException) {
            // If the model or vocab can't be loaded, the app cannot perform a core function.
            // Throwing an exception here will crash the app on startup, making the issue
            // immediately visible during development.
            throw IllegalStateException("Failed to initialize LegalBertService", e)
        }
    }

    /**
     * Generates a fixed-size embedding (vector representation) for a given text.
     * The embedding captures the semantic meaning of the text and can be used for
     * tasks like similarity comparison, classification, and clustering.
     *
     * @param text The input string to be embedded.
     * @return A 768-element FloatArray representing the text's semantic embedding,
     *         derived from the [CLS] token's output.
     */
    fun getEmbedding(text: String): FloatArray {
        // 1. Tokenize the input text using our custom tokenizer.
        val tokenized = tokenizer.tokenize(text, maxSeqLength)

        // 2. Prepare the three input buffers required by the BERT model.
        // The byte buffers are allocated with space for maxSeqLength * 4 bytes (size of an Int).
        // 2. Prepare inputs for the TFLite model.
        // BERT models typically require 3 inputs: input_ids, attention_mask, and token_type_ids.
        val inputIdsBuffer = ByteBuffer.allocateDirect(maxSeqLength * 4).apply {
            order(ByteOrder.nativeOrder())
            tokenized.inputIds.forEach { putInt(it.toInt()) }
            rewind()
        }

        val attentionMaskBuffer = ByteBuffer.allocateDirect(maxSeqLength * 4).apply {
            order(ByteOrder.nativeOrder())
            tokenized.attentionMask.forEach { putInt(it.toInt()) }
            rewind()
        }

        val tokenTypeIdsBuffer = ByteBuffer.allocateDirect(maxSeqLength * 4).apply {
            order(ByteOrder.nativeOrder())
            tokenized.tokenTypeIds.forEach { putInt(it.toInt()) }
            rewind()
        }

        val inputs = arrayOf(inputIdsBuffer, attentionMaskBuffer, tokenTypeIdsBuffer)
        val outputs = mutableMapOf<Int, Any>()

        // 3. Prepare the output buffer. For BERT, the primary output is the "last hidden state"
        // which contains the embedding for every token in the sequence.
        // The shape is [batch_size, sequence_length, hidden_size], which is [1, 128, 768] here.
        val outputEmbeddings = Array(1) { Array(maxSeqLength) { FloatArray(768) } }
        outputs[0] = outputEmbeddings

        // 4. Run inference using the TFLite interpreter.
        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        // 5. Return the embedding for the [CLS] token. The embedding of this special token
        // at the beginning of the sequence is conventionally used as the aggregate
        // representation of the entire sequence's meaning.
        return outputEmbeddings[0][0]
    }

    /**
     * Loads the TFLite model file from the assets folder into a ByteBuffer.
     * This is a standard procedure for preparing a model for the TFLite Interpreter.
     *
     * @return A ByteBuffer containing the model data.
     * @throws IOException if the model file cannot be found or read.
     */
    @Throws(IOException::class)
    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd("legal_bert.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
