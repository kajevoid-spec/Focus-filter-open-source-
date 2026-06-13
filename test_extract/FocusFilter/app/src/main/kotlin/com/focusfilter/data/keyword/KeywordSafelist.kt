// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.keyword

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keyword_safelist")
data class KeywordSafelist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val category: String = "CUSTOM",
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
