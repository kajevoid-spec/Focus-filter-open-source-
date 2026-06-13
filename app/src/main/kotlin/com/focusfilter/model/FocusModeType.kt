// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.model

enum class FocusModeType(val displayName: String, val description: String) {
    GAMING("Gaming Mode", "Block distractions, allow essentials"),
    WORK("Work Mode", "Prioritize work apps and important alerts"),
    SLEEP("Sleep Mode", "Only emergencies will break through"),
    CUSTOM("Custom Mode", "Create your own rules and filters"),
    STUDY("Study Mode", "Deep focus for learning and studying"),
    EXAM("Exam Mode", "Maximum focus — only emergencies pass"),
    DEEP_FOCUS("Deep Focus", "Zero interruptions — you are in the zone"),
    READING("Reading Mode", "Quiet time for books and articles"),
    NONE("Focus Off", "All notifications pass through")
}
