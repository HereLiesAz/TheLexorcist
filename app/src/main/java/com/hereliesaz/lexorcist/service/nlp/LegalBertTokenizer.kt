package com.hereliesaz.lexorcist.service.nlp

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections

/**
 * A custom tokenizer for BERT models, specifically implementing the WordPiece algorithm.
 * This class is responsible for converting raw text into the format required by a BERT TFLite model.
 *
 * @param context The application context, used to access model assets like the vocabulary file.
 */
class LegalBertTokenizer(private val context: Context) {

    // A map from token strings to their integer IDs.
    private val vocab: MutableMap<String, Int> = mutableMapOf()
    private val inverseVocab: MutableMap<Int, String> = mutableMapOf()

    // Special tokens used by BERT.
    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val unkToken = "[UNK]"
    private val padToken = "[PAD]"

    init {
        loadVocab()
    }

    /**
     * The primary public method for tokenizing text.
     *
     * @param text The raw input string.
     * @param maxSeqLength The maximum sequence length. The output will be padded or truncated to this length.
     * @return A [TokenizerOutput] object containing the model inputs.
     */
    fun tokenize(text: String, maxSeqLength: Int = 128): TokenizerOutput {
        // 1. Start with the [CLS] token.
        val tokens = mutableListOf(clsToken)

        // 2. Perform basic tokenization (whitespace) and then WordPiece tokenization.
        val basicTokens = text.trim().split("\\s+".toRegex())
        for (token in basicTokens) {
            tokens.addAll(wordPieceTokenize(token.lowercase()))
        }

        // 3. Truncate if necessary before adding [SEP].
        if (tokens.size > maxSeqLength - 1) {
            tokens.subList(maxSeqLength - 1, tokens.size).clear()
        }

        // 4. Add the [SEP] token.
        tokens.add(sepToken)

        // 5. Convert tokens to IDs.
        val inputIds = tokens.map { vocab.getOrDefault(it, vocab[unkToken]!!) }.toMutableList()

        // 6. Generate attention mask.
        val attentionMask = MutableList(inputIds.size) { 1 }

        // 7. Pad the sequences to the max length.
        val paddingLength = maxSeqLength - inputIds.size
        if (paddingLength > 0) {
            val padId = vocab[padToken]!!
            inputIds.addAll(Collections.nCopies(paddingLength, padId))
            attentionMask.addAll(Collections.nCopies(paddingLength, 0))
        }

        // 8. Generate token type IDs (all 0s for single-sequence tasks).
        val tokenTypeIds = MutableList(maxSeqLength) { 0 }

        return TokenizerOutput(
            inputIds = inputIds.map { it.toLong() }.toLongArray(),
            attentionMask = attentionMask.map { it.toLong() }.toLongArray(),
            tokenTypeIds = tokenTypeIds.map { it.toLong() }.toLongArray()
        )
    }

    /**
     * Implements the WordPiece tokenization algorithm.
     * If a token is not in the vocabulary, it's broken down into the largest possible sub-words.
     */
    private fun wordPieceTokenize(token: String): List<String> {
        if (vocab.containsKey(token)) {
            return listOf(token)
        }

        val subTokens = mutableListOf<String>()
        var remaining = token
        while (remaining.isNotEmpty()) {
            var i = remaining.length
            while (i > 0) {
                val sub = (if (subTokens.isEmpty()) "" else "##") + remaining.substring(0, i)
                if (vocab.containsKey(sub)) {
                    subTokens.add(sub)
                    remaining = remaining.substring(i)
                    break
                }
                i--
            }
            if (i == 0) {
                // Could not find any sub-word. Add [UNK] and stop.
                return listOf(unkToken)
            }
        }
        return subTokens
    }

    /**
     * Loads the vocabulary from the "vocab.txt" file in the assets folder.
     */
    private fun loadVocab() {
        context.assets.open("vocab.txt").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).forEachLine { line ->
                val token = line.trim()
                if (token.isNotEmpty()) {
                    val id = vocab.size
                    vocab[token] = id
                    inverseVocab[id] = token
                }
            }
        }
    }

    /**
     * Data class to hold the output of the tokenizer, ready for the TFLite model.
     */
    data class TokenizerOutput(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray
    )
}
