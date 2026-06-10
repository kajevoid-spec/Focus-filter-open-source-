// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filtered_notifications")
data class FilteredNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val action: String,            // NotificationAction name
    val classifierLabel: String,   // e.g. "otp", "spam", "social", "games"
    val activeModeAtTime: String,  // mode type string
    val blockReason: String = "",  // Human-readable blocking reason
    val isRead: Boolean = false,
    val isRestored: Boolean = false,
    val isOverride: Boolean = false, // User pressed "Allow Once"
    val isPermanent: Boolean = false, // Saved permanently — never auto-cleared
    val sbnKey: String? = null        // StatusBarNotification.key — null for old rows
)
