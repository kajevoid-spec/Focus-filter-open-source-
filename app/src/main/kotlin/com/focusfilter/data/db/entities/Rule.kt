// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,       // "VIP_CONTACT", "ALLOWED_APP", "KEYWORD", "CUSTOM"
    val value: String,      // contact name/number, package name, keyword, or custom pattern
    val action: String,     // NotificationAction name
    val isEnabled: Boolean = true,
    val focusModes: String = "ALL",  // comma-separated modes or "ALL"
    val label: String = ""  // display label
)
