// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

/**
 * Keyword-based on-device classifier.
 * Entirely local — no network calls, no external SDKs.
 *
 * Uses strict phrase-based matching only — never single generic words —
 * to eliminate false positives that let spam and phishing through.
 *
 * Label priority (highest → lowest):
 *   otp → payment → bank → delivery → emergency → security →
 *   promo → social → spam → important → games → unknown
 */
class SimpleClassifier : NotificationClassifier {

    override fun classify(notificationText: String): String {
        val lower = notificationText.lowercase()
        return when {
            lower.containsAny(
                "otp", "one-time password", "one time password",
                "one-time code", "one time code", "verification code",
                "your code is", "your otp", "auth code", "2fa code",
                "two-factor code", "two factor code", "enter code",
                "use code", "temporary code", "single use code",
                "do not share", "never share this", "expires in",
                "valid for", "login attempt", "sign-in code",
                "kod pengesahan", "pengesahan"
            ) -> "otp"

            lower.containsAny(
                "payment received", "payment successful", "payment failed",
                "payment of", "amount debited", "amount credited",
                "money sent", "money received", "funds transferred",
                "transfer successful", "upi payment", "transaction successful",
                "transaction failed", "transaction of", "debited from your",
                "credited to your", "auto debit", "auto-debit",
                "standing instruction", "direct debit", "recurring payment",
                "subscription renewed", "invoice due", "payment overdue",
                "payment reminder", "bayaran", "pembayaran diterima",
                "transaksi berjaya", "wang telah"
            ) -> "payment"

            lower.containsAny(
                "your bank", "bank account", "bank transfer", "banking alert",
                "bank statement", "account balance", "account locked",
                "account blocked", "account suspended", "account statement",
                "minimum balance", "low balance alert", "card blocked",
                "card declined", "card charged", "emi due", "loan emi",
                "credit card bill"
            ) -> "bank"

            lower.containsAny(
                "delivered", "out for delivery", "your package",
                "your order has", "your order is confirmed", "order confirmed",
                "order is confirmed", "order has been confirmed",
                "your shipment", "picked up by", "attempted delivery",
                "delivery failed", "reschedule delivery", "left at door",
                "handed to", "signed by", "return to sender",
                "pickup ready", "pick up your", "courier",
                "tracking number", "parcel", "shipment arrives",
                "estimated delivery", "package arrived"
            ) -> "delivery"

            lower.containsAny(
                "emergency alert", "medical emergency", "emergency contact",
                "sos alert", "critical alert", "safety alert", "amber alert",
                "evacuation", "emergency broadcast", "fire alert",
                "flood warning", "earthquake alert"
            ) -> "emergency"

            lower.containsAny(
                "suspicious login", "suspicious activity", "unauthorized access",
                "unauthorized login", "unusual login", "unusual activity",
                "security alert", "security breach", "account compromised",
                "fraud alert", "identity verification required",
                "login attempt blocked", "sign in attempt", "new device login",
                "new login detected", "password changed",
                "email changed", "phone number changed",
                "2-step verification", "recovery code"
            ) -> "security"

            lower.containsAny(
                "special offer", "exclusive offer", "limited offer",
                "% off", "flash sale", "mega sale", "big sale",
                "limited time", "coupon code", "promo code",
                "cashback offer", "earn rewards", "redeem rewards",
                "use code", "discount code", "deal expires"
            ) -> "promo"

            lower.containsAny(
                "liked", "commented", "followed", "mention",
                "tagged", "story", "reacted", "replied", "shared your"
            ) -> "social"

            lower.containsAny(
                "spam", "unsubscribe", "click here to claim", "win!",
                "winner", "prize", "claim your free", "you won free",
                "get it free", "congratulations you",
                "you have been selected", "you are a winner", "free gift",
                "click to collect", "tap to claim",
                "limited offer expires", "act now before"
            ) -> "spam"

            lower.containsAny(
                "meeting reminder", "call scheduled", "event reminder",
                "task assigned", "task due", "deadline today", "deadline tomorrow",
                "due today", "due tomorrow", "assigned to you",
                "interview scheduled", "appointment confirmed",
                "booking confirmed", "reservation confirmed",
                "check-in reminder", "flight reminder", "flight boarding"
            ) -> "important"

            lower.containsAny(
                "level up", "achievement", "game over", "match found",
                "your turn", "challenge", "high score", "leaderboard",
                "arena", "quest", "dungeon"
            ) -> "games"

            else -> "unknown"
        }
    }

    /**
     * Title-weighted classification: checks the title independently first.
     * If the title yields a confident label, use it — the title is the most
     * semantically dense field and tends to be more deliberate than the body.
     */
    override fun classify(title: String, body: String): String {
        val titleLabel = classify(title)
        if (titleLabel != "unknown") return titleLabel
        return classify("$title $body")
    }

    override fun hasScamSignals(text: String): Boolean {
        val lower = text.lowercase()
        val hasUrl = listOf(
            "bit.ly", "tinyurl", "t.me/", "http://",
            "click here", "tap here", "verify at",
            "login at", "confirm at", "visit now",
            "go to link", "follow link", "open link",
            "check link", "redeem at", "collect at"
        ).any { lower.contains(it) }
        val hasUrgency = listOf(
            "verify now", "confirm immediately",
            "act now", "your account will",
            "unusual activity", "unauthorized access",
            "account frozen", "immediate action required",
            "will be terminated", "will be suspended",
            "will be closed", "last warning", "final notice",
            "respond immediately", "failure to respond",
            "legal action", "arrest warrant", "police complaint"
        ).any { lower.contains(it) }
        return hasUrl || hasUrgency
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
