// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val count: Int
)
