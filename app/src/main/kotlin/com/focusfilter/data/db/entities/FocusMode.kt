// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-defined or built-in focus mode.
 *
 * [allowedKeywords] and [blockedKeywords] are stored in the database as JSON arrays
 * (via [com.focusfilter.data.db.converters.KeywordsConverter]) to avoid the silent
 * breakage that occurs when a keyword itself contains a comma.
 */
@Entity(tableName = "focus_modes")
data class FocusMode(
    @PrimaryKey val type: String,
    val displayName: String = "",
    val description: String = "",
    val iconName: String = "ic_shield",
    val isActive: Boolean = false,
    val silenceCalls: Boolean = false,
    val allowedApps: String = "",
    val allowedContacts: String = "",
    val allowedKeywords: List<String> = emptyList(),
    val blockedKeywords: List<String> = emptyList(),
    val defaultAction: String = "BLOCK",
    val isCustom: Boolean = false,
    val isBuiltIn: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val scheduleStart: String = "",
    val scheduleEnd: String = ""
)
