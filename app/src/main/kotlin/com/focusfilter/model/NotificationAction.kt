// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.model

enum class NotificationAction(val label: String) {
    ALLOW("Allow"),
    BLOCK("Block"),
    SILENT("Silent"),
    HOLD("Hold"),
    OVERRIDE("Allow Once")
}
