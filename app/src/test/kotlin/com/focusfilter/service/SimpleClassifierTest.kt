// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SimpleClassifier.classify].
 *
 * Edge cases from the improvement spec:
 *  - OTP passthrough
 *  - free/win false-positive keywords
 *  - unknown label fallback
 *  - mode-keyword matching (social, promo, spam, games)
 */
class SimpleClassifierTest {

    private lateinit var classifier: SimpleClassifier

    @Before
    fun setup() {
        classifier = SimpleClassifier()
    }

    // ── OTP passthrough ──────────────────────────────────────────────────────────

    @Test
    fun `OTP keyword returns otp label`() {
        assertEquals("otp", classifier.classify("Your OTP is 482910"))
    }

    @Test
    fun `one-time password phrase returns otp label`() {
        assertEquals("otp", classifier.classify("Your one-time password: 123456"))
    }

    @Test
    fun `verification code phrase returns otp label`() {
        assertEquals("otp", classifier.classify("Use verification code 7890 to confirm"))
    }

    @Test
    fun `2fa phrase returns otp label`() {
        assertEquals("otp", classifier.classify("Your 2fa token has been sent"))
    }

    // ── Free/win false positives (spam label) ────────────────────────────────────

    @Test
    fun `free keyword returns spam label`() {
        assertEquals("spam", classifier.classify("Click here to claim your FREE gift card"))
    }

    @Test
    fun `win! phrase returns spam label`() {
        // Bare "win" was replaced with "win!" to avoid matching "winding", "winning streak", etc.
        assertEquals("spam", classifier.classify("You win! A brand new iPhone today!"))
    }

    @Test
    fun `winner keyword returns spam label`() {
        assertEquals("spam", classifier.classify("Congratulations, you are the winner of our draw"))
    }

    @Test
    fun `congratulations you phrase returns spam label`() {
        assertEquals("spam", classifier.classify("Congratulations you have been selected"))
    }

    @Test
    fun `prize keyword returns spam label`() {
        assertEquals("spam", classifier.classify("Collect your prize now before it expires"))
    }

    // ── Unknown label fallback ───────────────────────────────────────────────────

    @Test
    fun `unrecognised text returns unknown label`() {
        assertEquals("unknown", classifier.classify("Hey, let's catch up later this week!"))
    }

    @Test
    fun `empty text returns unknown label`() {
        assertEquals("unknown", classifier.classify(""))
    }

    @Test
    fun `whitespace-only text returns unknown label`() {
        assertEquals("unknown", classifier.classify("   "))
    }

    // ── Other label categories ───────────────────────────────────────────────────

    @Test
    fun `payment keyword returns payment label`() {
        assertEquals("payment", classifier.classify("Payment of Rs 500 received"))
    }

    @Test
    fun `emergency keyword returns emergency label`() {
        assertEquals("emergency", classifier.classify("EMERGENCY: flood warning in your area"))
    }

    @Test
    fun `social keyword returns social label`() {
        assertEquals("social", classifier.classify("Alice liked your photo"))
    }

    @Test
    fun `promo keyword returns promo label`() {
        assertEquals("promo", classifier.classify("50% off sale — limited time only!"))
    }

    @Test
    fun `game keyword returns games label`() {
        assertEquals("games", classifier.classify("Your quest is ready — enter the dungeon!"))
    }

    @Test
    fun `meeting keyword returns important label`() {
        assertEquals("important", classifier.classify("Meeting reminder: standup in 10 minutes"))
    }

    @Test
    fun `bank keyword returns bank label`() {
        assertEquals("bank", classifier.classify("Your bank account statement is ready"))
    }

    // ── OTP takes priority over spam keywords ────────────────────────────────────

    @Test
    fun `OTP takes priority over spam-like text`() {
        // "congratulations you" (spam trigger) appears alongside "verification code" (OTP trigger).
        // OTP is matched first in the when-chain, so it wins.
        assertEquals("otp", classifier.classify("Congratulations you verified! Your verification code is 9876"))
    }

    @Test
    fun `bare free word alone does not trigger spam label`() {
        // "free" alone was too broad — "feel free to reply" / "free shipping" are legitimate.
        // Only specific phrases like "claim your free" now trigger spam.
        assertEquals("unknown", classifier.classify("Feel free to reply at your convenience"))
    }

    @Test
    fun `bare win word alone does not trigger spam label`() {
        // "win" alone was too broad — "winning streak", "win the game" are not spam.
        assertEquals("unknown", classifier.classify("Keep up your winning streak today"))
    }

    @Test
    fun `help alone does not trigger emergency label`() {
        // Bare "help" caused support and help-centre notifications to pass as emergencies.
        assertEquals("unknown", classifier.classify("How can we help you? Visit our help centre"))
    }
}
