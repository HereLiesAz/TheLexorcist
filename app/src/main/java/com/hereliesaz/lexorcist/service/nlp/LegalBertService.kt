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

@Singleton
class LegalBertService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private lateinit var interpreter: Interpreter
    private lateinit var tokenizer: LegalBertTokenizer

    private val maxSeqLength = 128 // Standard sequence length for BERT

    init {
        try {
            tokenizer = LegalBertTokenizer(context)
            interpreter = Interpreter(loadModelFile())
        } catch (e: IOException) {
            // In a real app, provide more robust error handling or logging.
            throw IllegalStateException("Failed to initialize LegalBertService", e)
        }
    }

    /**
     * Generates a fixed-size embedding (vector representation) for a given text.
     *
     * @param text The input string.
     * @return A FloatArray representing the text's semantic embedding.
     */
    fun getEmbedding(text: String): FloatArray {
        // 1. Tokenize the input text.
        val tokenized = tokenizer.tokenize(text, maxSeqLength)

        // 2. Prepare inputs for the TFLite model.
        // BERT models typically require 3 inputs: input_ids, attention_mask, and token_type_ids.
        val inputIdsBuffer = ByteBuffer.allocateDirect(maxSeqLength * 4).apply {
            order(ByteOrder.nativeOrder())
            tokenized.inputIds.forEach { putInt(it.toInt()) } // TFLite models often use Int32
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

        // 3. Prepare output buffer.
        // The primary output for embeddings is the "last hidden state".
        // Shape: [1, maxSeqLength, 768] (for BERT-base)
        val outputEmbeddings = Array(1) { Array(maxSeqLength) { FloatArray(768) } }
        outputs[0] = outputEmbeddings

        // 4. Run inference.
        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        // 5. Return the embedding for the [CLS] token.
        // The [CLS] token's embedding is the first vector in the sequence and is
        // commonly used as a representation of the entire input sequence.
        return outputEmbeddings[0][0]
    }

    /**
     * Loads the TFLite model file from the assets folder.
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
