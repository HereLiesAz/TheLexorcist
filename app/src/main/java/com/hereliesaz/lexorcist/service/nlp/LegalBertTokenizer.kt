package com.hereliesaz.lexorcist.service.nlp

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections

/**
 * A custom tokenizer for BERT models, specifically implementing the WordPiece algorithm.
 * This class is responsible for converting raw text into the format required by a BERT TFLite model.
 * It handles vocabulary loading, tokenization, and the creation of input tensors.
 *
 * @param context The application context, used to access model assets like the vocabulary file.
 * @property vocab A map from token strings to their integer IDs.
 */
class LegalBertTokenizer(private val context: Context) {

    private val vocab: MutableMap<String, Int> = mutableMapOf()
    private val inverseVocab: MutableMap<Int, String> = mutableMapOf()

    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val unkToken = "[UNK]"
    private val padToken = "[PAD]"

    init {
        loadVocab()
    }

    /**
     * The primary public method for tokenizing text. It takes a raw string and converts it
     * into the three input arrays required by BERT models.
     *
     * @param text The raw input string to be tokenized.
     * @param maxSeqLength The maximum sequence length. The output will be padded or truncated to this length.
     * @return A [TokenizerOutput] object containing the `inputIds`, `attentionMask`, and `tokenTypeIds`.
     */
    fun tokenize(text: String, maxSeqLength: Int = 128): TokenizerOutput {
        // 1. Start with the classification token.
        val tokens = mutableListOf(clsToken)

        // 2. Perform basic whitespace tokenization, then WordPiece tokenization on each word.
        val basicTokens = text.trim().split("\\s+".toRegex())
        for (token in basicTokens) {
            tokens.addAll(wordPieceTokenize(token.lowercase()))
        }

        // 3. Truncate if the sequence is too long, preserving space for the separator token.
        if (tokens.size > maxSeqLength - 1) {
            tokens.subList(maxSeqLength - 1, tokens.size).clear()
        }

        // 4. Add the separator token.
        tokens.add(sepToken)

        // 5. Convert token strings to their corresponding integer IDs from the vocabulary.
        val inputIds = tokens.map { vocab.getOrDefault(it, vocab[unkToken]!!) }.toMutableList()

        // 6. Create the attention mask, which is 1 for real tokens and 0 for padding.
        val attentionMask = MutableList(inputIds.size) { 1 }

        // 7. Pad the sequences with the padding token ID and 0s in the attention mask.
        val paddingLength = maxSeqLength - inputIds.size
        if (paddingLength > 0) {
            val padId = vocab[padToken]!!
            inputIds.addAll(Collections.nCopies(paddingLength, padId))
            attentionMask.addAll(Collections.nCopies(paddingLength, 0))
        }

        // 8. Create token type IDs, which are all 0 for single-sequence classification/embedding tasks.
        val tokenTypeIds = MutableList(maxSeqLength) { 0 }

        return TokenizerOutput(
            inputIds = inputIds.map { it.toLong() }.toLongArray(),
            attentionMask = attentionMask.map { it.toLong() }.toLongArray(),
            tokenTypeIds = tokenTypeIds.map { it.toLong() }.toLongArray()
        )
    }

    /**
     * Implements the WordPiece tokenization algorithm. An unknown token is greedily broken down
     * into the largest possible in-vocabulary sub-words.
     *
     * @param token A single word to be tokenized.
     * @return A list of one or more sub-word tokens.
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
                // Failsafe for characters that are not in the vocabulary at all.
                return listOf(unkToken)
            }
        }
        return subTokens
    }

    /**
     * Loads the vocabulary from the "vocab.txt" file in the assets folder into a map.
     * @throws IOException if the vocab file cannot be found or read.
     */
    @Throws(IOException::class)
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
     * Data class to hold the three standard inputs required by a BERT model.
     */
    data class TokenizerOutput(
        /** The sequence of token IDs. */
        val inputIds: LongArray,
        /** A mask to distinguish real tokens from padding tokens (1 for real, 0 for padding). */
        val attentionMask: LongArray,
        /** A segment mask to distinguish between different sentences (all 0s for single-sequence tasks). */
        val tokenTypeIds: LongArray
    )
}
