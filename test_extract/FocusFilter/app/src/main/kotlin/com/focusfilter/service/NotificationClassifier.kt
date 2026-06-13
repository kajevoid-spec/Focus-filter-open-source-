// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

/**
 * Interface for classifying notification text into semantic categories.
 * All classification is done locally on-device — no data leaves the device.
 *
 * Valid labels: "otp", "payment", "bank", "delivery", "emergency", "security",
 *               "promo", "social", "spam", "important", "games", "trusted", "unknown"
 */
interface NotificationClassifier {

    /** Classify using combined notification text. */
    fun classify(notificationText: String): String

    /**
     * Classify with title weighting: the notification title carries more semantic
     * signal than the body (it's what the app author chose to highlight), so it is
     * checked independently first. If the title alone yields a confident label,
     * that label is returned immediately without looking at the body.
     *
     * Default implementation falls back to combined text — override for better accuracy.
     */
    fun classify(title: String, body: String): String = classify("$title $body")

    /**
     * Returns true if the text contains URL-based or urgency-based scam signals.
     * Default implementation always returns false — override for real detection.
     */
    fun hasScamSignals(text: String): Boolean = false
}
