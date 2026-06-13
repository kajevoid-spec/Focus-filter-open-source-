// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BERT WordPiece tokenizer backed by vocab.txt from assets.
 * Produces input_ids + attention_mask + token_type_ids at fixed length 128.
 *
 * Vocabulary loading is asynchronous — call [initAsync] before using [encode].
 * [encode] returns a zero-attention encoding (safe no-op) until [isReady] is true.
 *
 * The private constructor with a pre-loaded vocab map is used by [forTesting].
 */
class BertTokenizer private constructor(preloadedVocab: Map<String, Int>?) {

    constructor() : this(null)

    companion object {
        private const val TAG = "BertTokenizer"
        const val PAD_ID   = 0
        const val UNK_ID   = 100
        const val CLS_ID   = 101
        const val SEP_ID   = 102
        const val MAX_LEN  = 128
        private const val MAX_CHARS_PER_WORD = 100

        /** Creates a fully-initialised tokenizer with an injected vocabulary — for unit tests only. */
        fun forTesting(vocab: Map<String, Int> = emptyMap()): BertTokenizer =
            BertTokenizer(vocab)
    }

    data class Encoding(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray
    )

    @Volatile private var vocab: Map<String, Int> = preloadedVocab ?: emptyMap()

    /** True once vocab has been fully loaded and the tokenizer is safe to use. */
    @Volatile var isReady: Boolean = preloadedVocab != null
        private set

    /**
     * Loads vocab.txt from assets on an IO thread.
     * Safe to call multiple times — subsequent calls are no-ops if already ready.
     */
    suspend fun initAsync(context: Context) = withContext(Dispatchers.IO) {
        if (isReady) return@withContext
        val map = HashMap<String, Int>(32000)
        try {
            context.assets.open("vocab.txt").bufferedReader().useLines { seq ->
                seq.forEachIndexed { idx, line ->
                    val token = line.trim()
                    if (token.isNotEmpty()) map[token] = idx
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load vocab.txt: ${e.message}")
        }
        vocab = map
        // Only mark ready when the vocab actually loaded — an empty map means every
        // token encodes as [UNK], producing silent garbage inference results.
        isReady = map.isNotEmpty()
        if (!isReady) {
            Log.e(TAG, "Vocab empty after load attempt — tokenizer disabled.")
        } else {
            Log.d(TAG, "Vocab loaded asynchronously: ${vocab.size} tokens")
        }
    }

    /**
     * Encodes [text] into BERT input tensors.
     * Returns a zero-attention encoding if the tokenizer is not yet initialised.
     */
    fun encode(text: String): Encoding {
        if (!isReady) {
            return Encoding(
                LongArray(MAX_LEN) { PAD_ID.toLong() },
                LongArray(MAX_LEN) { 0L },
                LongArray(MAX_LEN) { 0L }
            )
        }

        val tokens = tokenize(text)
        val maxContent = MAX_LEN - 2
        val truncated = if (tokens.size > maxContent) tokens.subList(0, maxContent) else tokens

        val ids     = LongArray(MAX_LEN) { PAD_ID.toLong() }
        val mask    = LongArray(MAX_LEN) { 0L }
        val typeIds = LongArray(MAX_LEN) { 0L }

        ids[0] = CLS_ID.toLong(); mask[0] = 1L
        truncated.forEachIndexed { i, tok ->
            ids[i + 1] = (vocab[tok] ?: UNK_ID).toLong()
            mask[i + 1] = 1L
        }
        val sepPos = truncated.size + 1
        if (sepPos < MAX_LEN) { ids[sepPos] = SEP_ID.toLong(); mask[sepPos] = 1L }

        return Encoding(ids, mask, typeIds)
    }

    // ── Tokenization ────────────────────────────────────────────────────────────

    private fun tokenize(text: String): List<String> {
        val wordPieces = mutableListOf<String>()
        basicTokenize(text).forEach { word ->
            wordPieces.addAll(wordPiece(word))
        }
        return wordPieces
    }

    private fun basicTokenize(text: String): List<String> {
        val cleaned = buildString {
            for (ch in text) {
                val cp = ch.code
                when {
                    cp == 0 || cp == 0xFFFD || isControl(ch) -> continue
                    isChineseCjk(cp) -> { append(' '); append(ch); append(' ') }
                    else -> append(ch)
                }
            }
        }

        val tokens = mutableListOf<String>()
        for (raw in cleaned.lowercase().split(Regex("\\s+"))) {
            if (raw.isBlank()) continue
            val buf = StringBuilder()
            for (ch in stripAccents(raw)) {
                when {
                    isPunctuation(ch) -> {
                        if (buf.isNotEmpty()) { tokens.add(buf.toString()); buf.clear() }
                        tokens.add(ch.toString())
                    }
                    else -> buf.append(ch)
                }
            }
            if (buf.isNotEmpty()) tokens.add(buf.toString())
        }
        return tokens
    }

    private fun wordPiece(word: String): List<String> {
        if (word.length > MAX_CHARS_PER_WORD) return listOf("[UNK]")
        val subTokens = mutableListOf<String>()
        var start = 0
        var failed = false
        while (start < word.length) {
            var end = word.length
            var found: String? = null
            while (start < end) {
                val sub = if (start == 0) word.substring(start, end)
                           else "##" + word.substring(start, end)
                if (sub in vocab) { found = sub; break }
                end--
            }
            if (found == null) { failed = true; break }
            subTokens.add(found)
            start = end
        }
        return if (failed) listOf("[UNK]") else subTokens
    }

    // ── Unicode helpers ──────────────────────────────────────────────────────────

    private fun isControl(c: Char): Boolean {
        if (c == '\t' || c == '\n' || c == '\r') return false
        val type = Character.getType(c)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    private fun isChineseCjk(cp: Int): Boolean =
        (cp in 0x4E00..0x9FFF) || (cp in 0x3400..0x4DBF) ||
        (cp in 0x20000..0x2A6DF) || (cp in 0x2A700..0x2B73F) ||
        (cp in 0x2B740..0x2B81F) || (cp in 0x2B820..0x2CEAF) ||
        (cp in 0xF900..0xFAFF)   || (cp in 0x2F800..0x2FA1F)

    private fun isPunctuation(c: Char): Boolean {
        val cp = c.code
        if ((cp in 33..47) || (cp in 58..64) || (cp in 91..96) || (cp in 123..126)) return true
        val type = Character.getType(c)
        return type == Character.DASH_PUNCTUATION.toInt() ||
               type == Character.START_PUNCTUATION.toInt() ||
               type == Character.END_PUNCTUATION.toInt() ||
               type == Character.CONNECTOR_PUNCTUATION.toInt() ||
               type == Character.OTHER_PUNCTUATION.toInt() ||
               type == Character.MATH_SYMBOL.toInt() ||
               type == Character.CURRENCY_SYMBOL.toInt() ||
               type == Character.MODIFIER_SYMBOL.toInt() ||
               type == Character.OTHER_SYMBOL.toInt()
    }

    private fun stripAccents(s: String): String {
        val normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return normalized.filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
    }
}
