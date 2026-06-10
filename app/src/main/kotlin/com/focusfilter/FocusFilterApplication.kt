// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focusfilter.data.db.AppDatabase
import com.focusfilter.data.db.entities.Rule
import com.focusfilter.data.keyword.KeywordSafelistManager
import com.focusfilter.data.repository.FocusModeRepository
import com.focusfilter.data.repository.NotificationRepository
import com.focusfilter.data.repository.RuleRepository
import com.focusfilter.service.BertSpamDetector
import com.focusfilter.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusFilterApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var cachedRules: List<Rule> = emptyList()
        private set

    fun invalidateRulesCache() {
        appScope.launch(Dispatchers.IO) {
            cachedRules = ruleRepository.getEnabledRules()
        }
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch(Dispatchers.IO) {
            val fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1_000)
            database.filteredNotificationDao().clearNonPermanentBefore(fourteenDaysAgo)
        }
        invalidateRulesCache()
        maybePostReminderNotification()
    }

    private fun maybePostReminderNotification() {
        val prefs = getSharedPreferences("focusfilter_prefs", Context.MODE_PRIVATE)
        val lastTs = prefs.getLong("last_reminder_ts", 0L)
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1_000
        if (System.currentTimeMillis() - lastTs < sevenDaysMs) return

        val channelId = "ff_reminder_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "FocusFilter Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "Periodic reminders to review filtered notifications" }
                )
            }
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_small)
            .setContentTitle("Don't lose your notifications!")
            .setContentText("You have notifications that will be deleted in 14 days. Open FocusFilter to review and save anything important.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You have notifications that will be deleted in 14 days. Open FocusFilter to review and save anything important."))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        nm.notify(1002, notification)
        prefs.edit().putLong("last_reminder_ts", System.currentTimeMillis()).apply()
    }

    // BUG 3: release ONNX session memory when the OS is critically low on RAM
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            spamDetector.close()
            spamDetector.initAsync(appScope)
        }
    }

    val database by lazy { AppDatabase.getInstance(this) }

    val notificationRepository by lazy {
        NotificationRepository(database.filteredNotificationDao())
    }
    val focusModeRepository by lazy {
        FocusModeRepository(database.focusModeDao())
    }
    val ruleRepository by lazy {
        RuleRepository(database.ruleDao())
    }

    val preferencesManager by lazy { PreferencesManager(this) }
    val keywordSafelistManager by lazy { KeywordSafelistManager(this) }

    val spamDetector: BertSpamDetector by lazy {
        BertSpamDetector(this).also { it.initAsync(appScope) }
    }
}
