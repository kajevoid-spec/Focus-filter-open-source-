// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.util

import android.content.Context
import android.content.SharedPreferences
import com.focusfilter.model.FocusModeType
import com.focusfilter.service.BertSpamDetector

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focusfilter_prefs", Context.MODE_PRIVATE)

    var activeModeType: String
        get() = prefs.getString(KEY_ACTIVE_MODE, FocusModeType.NONE.name) ?: FocusModeType.NONE.name
        set(value) = prefs.edit().putString(KEY_ACTIVE_MODE, value).apply()

    var isFocusEnabled: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FOCUS_ENABLED, value).apply()

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, false)
        set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    var totalFocusMinutesToday: Long
        get() = prefs.getLong(KEY_FOCUS_MINUTES, 0L)
        set(value) = prefs.edit().putLong(KEY_FOCUS_MINUTES, value).apply()

    var focusStartTimestamp: Long
        get() = prefs.getLong(KEY_FOCUS_START, 0L)
        set(value) = prefs.edit().putLong(KEY_FOCUS_START, value).apply()

    var lastResetDay: Int
        get() = prefs.getInt(KEY_LAST_RESET_DAY, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_RESET_DAY, value).apply()

    var hasAcceptedLegal: Boolean
        get() = prefs.getBoolean(KEY_LEGAL_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_LEGAL_ACCEPTED, value).apply()

    var emergencyBypassUntil: Long
        get() = prefs.getLong(KEY_EMERGENCY_BYPASS_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_EMERGENCY_BYPASS_UNTIL, value).apply()

    val isEmergencyBypassActive: Boolean
        get() = emergencyBypassUntil > System.currentTimeMillis()

    fun activateEmergencyBypass(durationMs: Long = 60 * 60 * 1000L) {
        emergencyBypassUntil = System.currentTimeMillis() + durationMs
    }

    fun cancelEmergencyBypass() {
        emergencyBypassUntil = 0L
    }

    var isLogOnlyMode: Boolean
        get() = prefs.getBoolean(KEY_LOG_ONLY_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_LOG_ONLY_MODE, value).apply()

    var isAiSpamDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_SPAM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_SPAM_ENABLED, value).apply()

    var spamThreshold: Float
        get() = prefs.getFloat(KEY_SPAM_THRESHOLD, BertSpamDetector.DEFAULT_THRESHOLD)
        set(value) = prefs.edit().putFloat(KEY_SPAM_THRESHOLD, value.coerceIn(0.70f, 0.95f)).apply()

    // ── Onboarding ───────────────────────────────────────────────────────────────

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    // BUG 1: one-time battery-optimisation prompt for aggressive OEMs
    var hasShownBatteryOptimizationPrompt: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_OPT_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_OPT_SHOWN, value).apply()

    // ── Trusted Apps ─────────────────────────────────────────────────────────────

    private var trustedAppsRaw: Set<String>
        get() = prefs.getStringSet(KEY_TRUSTED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_TRUSTED_APPS, value).apply()

    val trustedApps: Set<String> get() = trustedAppsRaw

    fun isTrustedApp(packageName: String): Boolean = packageName in trustedAppsRaw

    fun addTrustedApp(packageName: String) {
        trustedAppsRaw = trustedAppsRaw + packageName
    }

    fun removeTrustedApp(packageName: String) {
        trustedAppsRaw = trustedAppsRaw - packageName
    }

    fun setTrustedApps(packages: Set<String>) {
        trustedAppsRaw = packages
    }

    companion object {
        private const val KEY_ACTIVE_MODE            = "active_mode"
        private const val KEY_FOCUS_ENABLED          = "focus_enabled"
        private const val KEY_START_ON_BOOT          = "start_on_boot"
        private const val KEY_FOCUS_MINUTES          = "focus_minutes_today"
        private const val KEY_FOCUS_START            = "focus_start_ts"
        private const val KEY_LAST_RESET_DAY         = "last_reset_day"
        private const val KEY_LEGAL_ACCEPTED         = "has_accepted_legal"
        private const val KEY_EMERGENCY_BYPASS_UNTIL = "emergency_bypass_until"
        private const val KEY_LOG_ONLY_MODE          = "log_only_mode"
        private const val KEY_AI_SPAM_ENABLED        = "ai_spam_detection_enabled"
        private const val KEY_SPAM_THRESHOLD         = "spam_threshold"
        private const val KEY_TRUSTED_APPS           = "trusted_apps"
        private const val KEY_ONBOARDING_COMPLETE    = "onboarding_complete"
        private const val KEY_BATTERY_OPT_SHOWN      = "battery_opt_shown"
    }
}
