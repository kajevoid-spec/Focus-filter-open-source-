// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sender_reputation")
data class SenderReputation(
    @PrimaryKey val packageName: String,
    val allowCount: Int = 0,
    val blockCount: Int = 0,
    val lastSeen: Long = 0L
)
