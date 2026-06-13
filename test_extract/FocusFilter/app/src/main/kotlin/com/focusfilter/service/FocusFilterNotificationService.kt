// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focusfilter.FocusFilterApplication
import com.focusfilter.R
import com.focusfilter.data.db.entities.FilteredNotification
import com.focusfilter.model.FocusModeType
import com.focusfilter.model.NotificationAction
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Core notification filtering service — V5.1 pipeline.
 *
 * Decision pipeline (runs per notification on an IO thread):
 *
 *  Step 1   — Focus OFF?              → log as "pass", return
 *  Step 2   — Phone call?             → ALLOW immediately ("call" label)
 *  Step 3   — Safelist match?         → ALLOW and log (all tiers including SYSTEM)
 *  Step 3.5 — Keyword safelist?       → ALLOW and log
 *  Step 4   — Trusted apps?           → ALLOW and log
 *  Step 5   — BERT spam gate:
 *               ≥ 0.90 → BLOCK
 *               ≥ 0.70 → HOLD
 *  Step 6   — Classifier labels notification
 *  Step 7   — URL/scam signal detection
 *  Step 8   — BERT + Classifier cross-validation
 *  Step 9   — Critical labels safety net (otp, bank, emergency, payment, delivery, security)
 *  Step 10  — Rule engine
 *  Step 11  — Mode default (unknown label → HOLD not BLOCK)
 *  Step 12  — Exception handler: any uncaught exception → ALLOW
 *
 * Every return path calls logEntry(). Reputation is tracked after every decision.
 */
class FocusFilterNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG             = "FocusFilterService"
        private const val CHANNEL_SERVICE = "ff_service_channel"
        private const val NOTIF_ID_FG     = 1001

        // BUG 6: weak ref so BUG 6 allow-once check can query activeNotifications
        var instance: WeakReference<FocusFilterNotificationService>? = null
            private set
    }

    // Keeps the last 20 processed SBN keys to deduplicate rapid repost storms
    // without accidentally suppressing legitimately re-posted notifications.
    private val recentlyProcessedKeys = java.util.Collections.synchronizedSet(
        object : java.util.LinkedHashSet<String>() {
            override fun add(element: String): Boolean {
                if (size >= 20) iterator().apply { next(); remove() }
                return super.add(element)
            }
        }
    )

    private val semanticClassifier: NotificationClassifier = SimpleClassifier()
    private val spamDetector get() = (applicationContext as FocusFilterApplication).spamDetector

    // BUG 1: start foreground immediately when the listener connects
    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = WeakReference(this)
        createServiceChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setContentTitle("FocusFilter is active")
            .setContentText("Monitoring notifications in the background")
            .setSmallIcon(R.drawable.ic_shield_small)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID_FG, notification)
        Log.i(TAG, "Listener connected — foreground service started")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "Listener disconnected")
    }

    // BUG 2: START_STICKY so Android auto-restarts the service after being killed
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        spamDetector // warm up lazy init
        Log.i(TAG, "FocusFilter notification service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "FocusFilter notification service destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = applicationContext as FocusFilterApplication
        if (!app.preferencesManager.isFocusEnabled) return
        if (sbn.packageName == packageName) return

        val prevKey = recentlyProcessedKeys.add(sbn.key)
        if (!prevKey) return

        val extras  = sbn.notification?.extras ?: return
        val title   = extras.getString("android.title") ?: ""
        val text    = extras.getCharSequence("android.text")?.toString() ?: ""
        val appName = getAppName(sbn.packageName)
        val ts      = sbn.postTime

        // BUG 2: use app-level scope so processing survives a service restart
        app.appScope.launch(Dispatchers.IO) {
            try {
                processNotification(sbn, app, title, text, appName, ts)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error processing ${sbn.packageName} — allowing through: ${e.message}")
                logEntry(app, sbn.packageName, appName, title, text, ts,
                    app.preferencesManager.activeModeType,
                    NotificationAction.ALLOW, "error_fallback",
                    "Rule engine error — allowed as safety fallback.", isRestored = true, sbnKey = sbn.key)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    private suspend fun processNotification(
        sbn: StatusBarNotification,
        app: FocusFilterApplication,
        title: String,
        text: String,
        appName: String,
        ts: Long
    ) {
        val prefs         = app.preferencesManager
        val activeModeStr = prefs.activeModeType
        val pkgName       = sbn.packageName
        val logOnly       = prefs.isLogOnlyMode
        val combined      = "$title $text"
        val combinedLower = combined.lowercase()

        // ── STEP 1: Focus OFF? — silent pass-through, no DB entry ───────────────
        if (activeModeStr == FocusModeType.NONE.name) return

        // ── STEP 1.5: Emergency bypass active? ──────────────────────────────────
        if (prefs.isEmergencyBypassActive) {
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, "emergency",
                "Emergency bypass active — all notifications allowed.", isRestored = true)
            return
        }

        // ── STEP 2: Phone call? ──────────────────────────────────────────────────
        val callKeywords = listOf(
            "incoming call", "voice call",
            "video call", "missed call", "is calling",
            "ringing", "call from", "audio call",
            "is video calling", "is audio calling",
            "decline call", "tap to answer",
            "ongoing call", "call ended", "missed video call",
            "missed voice call", "missed audio call"
        )
        val category         = sbn.notification?.category
        val isCallByCategory = category == Notification.CATEGORY_CALL ||
                               category == Notification.CATEGORY_MISSED_CALL
        val isCallByText     = callKeywords.any { combinedLower.contains(it) }
        if (isCallByCategory || isCallByText) {
            Log.d(TAG, "[CALL] Always allowing call notification from $pkgName")
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, "call",
                "✓ Phone call — always allowed.", isRestored = true)
            trackReputation(app, pkgName, ts, allowed = true)
            return
        }

        // ── STEP 3: Safelist check (all tiers logged, including SYSTEM) ──────────
        val safeEntry = SafelistManager.getEntry(pkgName, packageManager, title, text)
        if (safeEntry != null) {
            Log.d(TAG, "[SAFE] ${safeEntry.tier.label} — $pkgName always allowed")
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, "important",
                "Protected — ${safeEntry.tier.label}: ${safeEntry.reason}.", isRestored = true)
            trackReputation(app, pkgName, ts, allowed = true)
            return
        }

        // ── STEP 3.5: Keyword safelist ───────────────────────────────────────────
        if (app.keywordSafelistManager.matchesKeyword(combined)) {
            Log.d(TAG, "[KEYWORD] Allowed $pkgName — matched keyword safelist")
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, "trusted",
                "✓ Matched keyword safelist.", isRestored = true)
            trackReputation(app, pkgName, ts, allowed = true)
            return
        }

        // ── STEP 4: Trusted apps ─────────────────────────────────────────────────
        if (prefs.isTrustedApp(pkgName)) {
            Log.d(TAG, "[TRUSTED] $appName ($pkgName) on trusted list — always allowed")
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, "trusted",
                "✓ $appName is on your Trusted Apps list — always allowed.", isRestored = true)
            trackReputation(app, pkgName, ts, allowed = true)
            return
        }

        // ── STEP 5: BERT spam gate ───────────────────────────────────────────────
        var bertResult: BertSpamDetector.SpamResult? = null
        if (prefs.isAiSpamDetectionEnabled) {
            val result = spamDetector.detect(combined, prefs.spamThreshold)
            bertResult = result
            if (result.modelAvailable) {
                val prob = result.spamProbability
                if (prob >= 0.90f) {
                    // High confidence spam → BLOCK
                    val pct = "%.0f%%".format(prob * 100)
                    Log.i(TAG, "[AI] High-confidence spam ($pct) — blocking $pkgName")
                    cancelNotification(sbn.key)
                    logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                        NotificationAction.BLOCK, "spam",
                        "AI detected spam ($pct confidence).", isRestored = false, sbnKey = sbn.key)
                    trackReputation(app, pkgName, ts, allowed = false)
                    return
                }
                if (prob >= 0.70f) {
                    // Medium confidence → HOLD, let user decide
                    val pct = "%.0f%%".format(prob * 100)
                    Log.i(TAG, "[AI] Uncertain spam ($pct) — holding $pkgName")
                    if (!logOnly) cancelNotification(sbn.key)
                    logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                        NotificationAction.HOLD, "spam",
                        "AI uncertain ($pct confidence) — held for review.", isRestored = false, sbnKey = sbn.key)
                    trackReputation(app, pkgName, ts, allowed = false)
                    return
                }
                if (prob >= 0.55f) {
                    val quickLabel = semanticClassifier.classify(title, text)
                    if (quickLabel in setOf("promo", "spam", "social")) {
                        val pct = "%.0f%%".format(prob * 100)
                        Log.i(TAG, "[AI] Medium-confidence spam ($pct) + $quickLabel — holding $pkgName")
                        if (!logOnly) cancelNotification(sbn.key)
                        logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                            NotificationAction.HOLD, quickLabel,
                            "AI medium confidence ($pct) and classifier says $quickLabel — held for review.",
                            isRestored = false, sbnKey = sbn.key)
                        trackReputation(app, pkgName, ts, allowed = false)
                        return
                    }
                }
            }
        }

        // ── STEP 6: Classifier labels the notification ───────────────────────────
        val label = semanticClassifier.classify(title, text)

        // ── STEP 7: URL and scam signal detection (runs on ALL labels) ──────────
        if (semanticClassifier.hasScamSignals(combined)) {
            Log.i(TAG, "[SCAM] Suspicious signals in $label notification from $pkgName — holding")
            if (!logOnly) cancelNotification(sbn.key)
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.HOLD, label,
                "Notification with suspicious signals — held for review.",
                isRestored = false, sbnKey = sbn.key)
            trackReputation(app, pkgName, ts, allowed = false)
            return
        }

        // ── STEP 8: BERT + Classifier cross-validation ───────────────────────────
        val bertSaidSpam = bertResult?.modelAvailable == true &&
                           (bertResult?.spamProbability ?: 0f) >= 0.70f
        if (bertSaidSpam) {
            val safeLabels = setOf("otp", "bank", "payment", "emergency")
            if (label == "spam" || label == "promo") {
                // Both agree → BLOCK
                Log.i(TAG, "[CROSS] BERT + classifier both say $label — blocking $pkgName")
                if (!logOnly) cancelNotification(sbn.key)
                logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                    NotificationAction.BLOCK, label,
                    "AI and classifier both flagged as $label — blocked.",
                    isRestored = false, sbnKey = sbn.key)
                trackReputation(app, pkgName, ts, allowed = false)
                return
            } else if (label in safeLabels) {
                // Disagree → HOLD, never BLOCK
                Log.i(TAG, "[CROSS] BERT says spam but classifier says $label — holding $pkgName")
                if (!logOnly) cancelNotification(sbn.key)
                logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                    NotificationAction.HOLD, label,
                    "AI flagged spam but classifier says $label — held for review.",
                    isRestored = false, sbnKey = sbn.key)
                trackReputation(app, pkgName, ts, allowed = false)
                return
            }
        }

        // ── STEP 9: Critical labels safety net ───────────────────────────────────
        val criticalLabels = setOf("otp", "bank", "emergency", "payment", "delivery", "security")
        val bertProb = bertResult?.spamProbability ?: 0f
        if (label in criticalLabels && bertProb < 0.50f) {
            Log.d(TAG, "[CRITICAL] $label from $pkgName — BERT clear, always allowed")
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, "important",
                "Critical label ($label) — BERT clear, always allowed.", isRestored = true)
            trackReputation(app, pkgName, ts, allowed = true)
            return
        }
        if (label in criticalLabels && bertProb >= 0.50f) {
            Log.d(TAG, "[CRITICAL] $label from $pkgName — BERT uncertain, holding for review")
            if (!logOnly) cancelNotification(sbn.key)
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.HOLD, label,
                "Critical label ($label) but AI uncertain — held for review.", isRestored = false, sbnKey = sbn.key)
            trackReputation(app, pkgName, ts, allowed = false)
            return
        }

        // ── STEP 10: Rule engine ─────────────────────────────────────────────────
        val rules  = app.cachedRules
        val mode   = app.focusModeRepository.getModeByType(activeModeStr)
        val actionResult = RuleEngine.resolveAction(
            pkgName       = pkgName,
            appName       = appName,
            title         = title,
            text          = text,
            label         = label,
            rules         = rules,
            activeModeStr = activeModeStr,
            mode          = mode
        )

        // ── STEP 11: Mode default ────────────────────────────────────────────────
        if (actionResult.action == NotificationAction.ALLOW) {
            logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
                NotificationAction.ALLOW, label, actionResult.reason, isRestored = true)
            trackReputation(app, pkgName, ts, allowed = true)
            return
        }

        // Unknown label should never be silently blocked — downgrade to HOLD for review.
        val finalAction = if (label == "unknown" && actionResult.action == NotificationAction.BLOCK) {
            NotificationAction.HOLD
        } else {
            actionResult.action
        }

        if (!logOnly) cancelNotification(sbn.key)
        logEntry(app, pkgName, appName, title, text, ts, activeModeStr,
            finalAction, label, actionResult.reason, isRestored = false, sbnKey = sbn.key)
        trackReputation(app, pkgName, ts, allowed = false)
    }

    private suspend fun trackReputation(
        app: FocusFilterApplication,
        pkgName: String,
        ts: Long,
        allowed: Boolean
    ) {
        try {
            val dao = app.database.senderReputationDao()
            dao.insertIfAbsent(pkgName)
            if (allowed) dao.incrementAllow(pkgName, ts) else dao.incrementBlock(pkgName, ts)
        } catch (e: Exception) {
            Log.w(TAG, "Reputation tracking failed for $pkgName: ${e.message}")
        }
    }

    private suspend fun logEntry(
        app: FocusFilterApplication,
        pkgName: String,
        appName: String,
        title: String,
        text: String,
        ts: Long,
        activeModeStr: String,
        action: NotificationAction,
        label: String,
        reason: String,
        isRestored: Boolean,
        sbnKey: String? = null
    ) {
        app.notificationRepository.insert(FilteredNotification(
            packageName      = pkgName,
            appName          = appName,
            title            = title,
            text             = text,
            timestamp        = ts,
            action           = action.name,
            classifierLabel  = label,
            activeModeAtTime = activeModeStr,
            blockReason      = reason,
            isRestored       = isRestored,
            sbnKey           = sbnKey
        ))
    }

    private fun getAppName(pkgName: String): String =
        try {
            val pm   = applicationContext.packageManager
            val info = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            pkgName.substringAfterLast(".")
        }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVICE,
                "FocusFilter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps FocusFilter running in the background"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
