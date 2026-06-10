// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.LongBuffer
import kotlin.math.exp

/**
 * On-device BERT spam detector using ONNX Runtime.
 *
 * Model: mrm8488/bert-tiny-finetuned-sms-spam-detection (INT8 quantised)
 * Labels: LABEL_0 = ham (legit), LABEL_1 = spam
 *
 * Initialisation is asynchronous — call [initAsync] after construction.
 * [detect] safely returns ham with modelAvailable=false until [isModelLoaded] is true.
 *
 * Safety contract: any exception during inference returns isSpam=false (treat as ham).
 */
class BertSpamDetector(private val context: Context) {

    companion object {
        private const val TAG = "BertSpamDetector"
        private const val MODEL_FILE = "model_int8.onnx"

        /** Default spam probability threshold when no user preference is set. */
        const val DEFAULT_THRESHOLD = 0.80f

        private const val SPAM_IDX = 1
        @Suppress("unused")
        private const val HAM_IDX = 0
    }

    data class SpamResult(
        val isSpam: Boolean,
        val spamProbability: Float,
        val modelAvailable: Boolean
    )

    private val tokenizer = BertTokenizer()
    private val env: OrtEnvironment? = runCatching { OrtEnvironment.getEnvironment() }.getOrNull()

    @Volatile private var session: OrtSession? = null
    @Volatile private var sessionInputNames: Set<String> = emptySet()

    private val _modelLoadedFlow = MutableStateFlow(false)

    /**
     * Emits true once the ONNX session and tokenizer are both ready.
     * Observe this in the UI to show model status.
     */
    val modelLoadedFlow: StateFlow<Boolean> = _modelLoadedFlow

    /** Convenience property for quick synchronous checks. */
    val isModelLoaded: Boolean get() = _modelLoadedFlow.value

    /**
     * Loads the tokenizer vocab and ONNX model on an IO thread.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    fun initAsync(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            tokenizer.initAsync(context)
            session = tryLoadModel()
            val ready = session != null && tokenizer.isReady
            _modelLoadedFlow.value = ready
            if (!ready) {
                Log.w(TAG, "Model not fully loaded — spam detection will be skipped.")
            }
        }
    }

    private fun tryLoadModel(): OrtSession? {
        val ortEnv = env ?: return null
        return try {
            val bytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            }
            val sess = ortEnv.createSession(bytes, opts)
            sessionInputNames = sess.inputNames ?: emptySet()
            Log.i(TAG, "ONNX model loaded. Inputs: $sessionInputNames")
            sess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ONNX model — spam detection disabled: ${e.message}")
            null
        }
    }

    /**
     * Returns whether [text] is spam, along with the raw probability.
     *
     * @param threshold Probability threshold (0–1) above which a result is treated as spam.
     *   Pass [PreferencesManager.spamThreshold] from the caller so the value stays configurable.
     *
     * Returns modelAvailable=false (and isSpam=false) if the model has not yet loaded.
     * Returns modelAvailable=true with isSpam=false on inference errors (fail-safe to ham).
     */
    fun detect(text: String, threshold: Float = DEFAULT_THRESHOLD): SpamResult {
        val sess = session
        val ortEnv = env
        // Model not yet loaded — return ham (safe default) until initAsync completes.
        if (!isModelLoaded || sess == null || ortEnv == null) {
            return SpamResult(isSpam = false, spamProbability = 0f, modelAvailable = false)
        }

        return try {
            val encoding = tokenizer.encode(text)
            val shape = longArrayOf(1L, BertTokenizer.MAX_LEN.toLong())

            val inputIdsTensor  = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(encoding.inputIds),      shape)
            val attMaskTensor   = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(encoding.attentionMask), shape)
            val tokTypesTensor  = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(encoding.tokenTypeIds),  shape)

            val inputs = buildMap {
                if ("input_ids"      in sessionInputNames) put("input_ids",      inputIdsTensor)
                if ("attention_mask" in sessionInputNames) put("attention_mask", attMaskTensor)
                if ("token_type_ids" in sessionInputNames) put("token_type_ids", tokTypesTensor)
                if (isEmpty()) {
                    put("input_ids",      inputIdsTensor)
                    put("attention_mask", attMaskTensor)
                    put("token_type_ids", tokTypesTensor)
                }
            }

            val outputs = sess.run(inputs)

            val logits: FloatArray = when (val raw = outputs[0].value) {
                is Array<*> -> (raw[0] as FloatArray)
                is FloatArray -> raw
                else -> throw IllegalStateException("Unexpected output type: ${raw?.javaClass}")
            }

            val maxL = logits.max()
            val exps = logits.map { exp((it - maxL).toDouble()).toFloat() }
            val sumE = exps.sum()
            val spamProb = exps[SPAM_IDX] / sumE

            inputIdsTensor.close(); attMaskTensor.close(); tokTypesTensor.close()
            outputs.close()

            Log.d(TAG, "spam_prob=%.3f threshold=%.2f text=\"${text.take(60)}\"".format(spamProb, threshold))
            SpamResult(isSpam = spamProb >= threshold, spamProbability = spamProb, modelAvailable = true)

        } catch (e: Exception) {
            Log.e(TAG, "Inference error — defaulting to ham: ${e.message}")
            SpamResult(isSpam = false, spamProbability = 0f, modelAvailable = true)
        }
    }

    /** Convenience alias used from the service. */
    fun isSpam(text: String, threshold: Float = DEFAULT_THRESHOLD): Boolean =
        detect(text, threshold).isSpam

    fun close() {
        runCatching { session?.close() }
    }
}
