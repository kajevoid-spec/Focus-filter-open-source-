// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.focusfilter.FocusFilterApplication

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app   = application as FocusFilterApplication
    private val prefs = app.preferencesManager

    val startOnBoot     = MutableLiveData(prefs.startOnBoot)
    val isLogOnlyMode   = MutableLiveData(prefs.isLogOnlyMode)
    val isAiSpamEnabled = MutableLiveData(prefs.isAiSpamDetectionEnabled)

    /**
     * True once the BERT model and tokenizer have fully loaded.
     * Backed by [BertSpamDetector.modelLoadedFlow] so the UI updates automatically.
     */
    val isModelLoaded = app.spamDetector.modelLoadedFlow.asLiveData()

    /**
     * Current spam sensitivity threshold (0.60–0.95).
     * Lower = catches more spam / higher false-positive risk.
     * Higher = misses borderline cases / fewer false positives.
     */
    val spamThreshold = MutableLiveData(prefs.spamThreshold)

    /**
     * True while an emergency bypass is active, false otherwise.
     * No countdown — the user already knows when they activated it.
     */
    val isBypassActive = MutableLiveData(prefs.isEmergencyBypassActive)

    fun setStartOnBoot(enabled: Boolean) {
        prefs.startOnBoot = enabled
        startOnBoot.value = enabled
    }

    fun setLogOnlyMode(enabled: Boolean) {
        prefs.isLogOnlyMode = enabled
        isLogOnlyMode.value = enabled
    }

    fun setAiSpamEnabled(enabled: Boolean) {
        prefs.isAiSpamDetectionEnabled = enabled
        isAiSpamEnabled.value = enabled
    }

    fun setSpamThreshold(value: Float) {
        prefs.spamThreshold = value
        spamThreshold.value = value
    }

    /** Pause all filtering for [hours] hours. */
    fun activateEmergencyBypass(hours: Long = 1L) {
        prefs.activateEmergencyBypass(hours * 60 * 60 * 1_000L)
        isBypassActive.value = true
    }

    fun cancelEmergencyBypass() {
        prefs.cancelEmergencyBypass()
        isBypassActive.value = false
    }
}
