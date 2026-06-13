// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusfilter.FocusFilterApplication

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = (context.applicationContext as FocusFilterApplication).preferencesManager
        if (!prefs.startOnBoot) return

        // Re-arm the notification listener service by toggling focus state
        // so the system re-binds the NotificationListenerService on boot.
        if (prefs.isFocusEnabled) {
            // Trigger application-level init (DB seed, rules cache, cleanup)
            // simply by accessing the application — the lazy inits do the rest.
            val app = context.applicationContext as FocusFilterApplication
            app.invalidateRulesCache()
        }
    }
}
