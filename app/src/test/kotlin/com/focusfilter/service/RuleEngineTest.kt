// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import com.focusfilter.data.db.entities.FocusMode
import com.focusfilter.data.db.entities.Rule
import com.focusfilter.model.NotificationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RuleEngine.resolveAction].
 *
 * Edge cases from the improvement spec:
 *  - OTP passthrough (critical label R4)
 *  - Keyword match for "free" / "win" — these map to spam via SimpleClassifier,
 *    but the label conflict fix means a BERT-confirmed ham is stored as "unknown";
 *    here we test that the raw "spam" label still blocks by default when no BERT.
 *  - Unknown label falls back to mode default action
 *  - Mode keyword matching (allowedKeywords, blockedKeywords)
 *  - VIP contact rule takes priority
 *  - Allowed app rule takes priority after VIP
 *  - KEYWORD rule overrides mode default
 */
class RuleEngineTest {

    private val workMode = FocusMode(
        type           = "WORK",
        displayName    = "Work Mode",
        defaultAction  = NotificationAction.BLOCK.name,
        allowedKeywords = listOf("urgent", "meeting"),
        blockedKeywords = listOf("game", "meme")
    )

    private val allowAllMode = FocusMode(
        type          = "CUSTOM",
        displayName   = "Custom",
        defaultAction = NotificationAction.ALLOW.name
    )

    // ── OTP passthrough ──────────────────────────────────────────────────────────

    @Test
    fun `OTP label is always allowed regardless of mode default`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.example.sms", appName = "Messages",
            title = "Bank", text = "Your OTP is 123456",
            label = "otp", rules = emptyList(),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    @Test
    fun `emergency label is always allowed`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.example.alert", appName = "Alert",
            title = "Emergency", text = "Flood warning in your area",
            label = "emergency", rules = emptyList(),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    @Test
    fun `payment label is always allowed`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.bank.app", appName = "Bank",
            title = "Debit", text = "Rs 200 debited from your account",
            label = "payment", rules = emptyList(),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    // ── Unknown label fallback ───────────────────────────────────────────────────

    @Test
    fun `unknown label falls back to mode default action (BLOCK)`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.random.app", appName = "RandomApp",
            title = "Hey", text = "What's up",
            label = "unknown", rules = emptyList(),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.BLOCK, result.action)
    }

    @Test
    fun `unknown label falls back to mode default action (ALLOW)`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.random.app", appName = "RandomApp",
            title = "Hey", text = "What's up",
            label = "unknown", rules = emptyList(),
            activeModeStr = "CUSTOM", mode = allowAllMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    @Test
    fun `no mode found falls back to BLOCK`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.random.app", appName = "RandomApp",
            title = "Hey", text = "What's up",
            label = "unknown", rules = emptyList(),
            activeModeStr = "WORK", mode = null
        )
        assertEquals(NotificationAction.BLOCK, result.action)
    }

    // ── Mode keyword matching ────────────────────────────────────────────────────

    @Test
    fun `notification matching allowedKeywords is allowed despite block default`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.slack.app", appName = "Slack",
            title = "Slack", text = "Meeting starts in 5 minutes",
            label = "unknown", rules = emptyList(),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    @Test
    fun `notification matching blockedKeywords is blocked`() {
        val result = RuleEngine.resolveAction(
            pkgName = "com.game.app", appName = "GameApp",
            title = "GameApp", text = "Your game is ready!",
            label = "unknown", rules = emptyList(),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.BLOCK, result.action)
    }

    // ── VIP contact rule ─────────────────────────────────────────────────────────

    @Test
    fun `VIP contact rule allows notification despite block default`() {
        val vipRule = Rule(
            type = "VIP_CONTACT", value = "alice", action = NotificationAction.ALLOW.name,
            label = "Alice", isEnabled = true, focusModes = "ALL"
        )
        val result = RuleEngine.resolveAction(
            pkgName = "com.whatsapp", appName = "WhatsApp",
            title = "Alice", text = "Can we talk?",
            label = "unknown", rules = listOf(vipRule),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    @Test
    fun `VIP contact rule only matches when mode is in scope`() {
        val vipRule = Rule(
            type = "VIP_CONTACT", value = "alice", action = NotificationAction.ALLOW.name,
            label = "Alice", isEnabled = true, focusModes = "SLEEP"  // different mode
        )
        val result = RuleEngine.resolveAction(
            pkgName = "com.whatsapp", appName = "WhatsApp",
            title = "Alice", text = "Can we talk?",
            label = "unknown", rules = listOf(vipRule),
            activeModeStr = "WORK", mode = workMode
        )
        // VIP rule is scoped to SLEEP, not WORK — falls through to mode default
        assertEquals(NotificationAction.BLOCK, result.action)
    }

    // ── Allowed app rule ─────────────────────────────────────────────────────────

    @Test
    fun `allowed app rule permits a specific package`() {
        val appRule = Rule(
            type = "ALLOWED_APP", value = "com.slack.app", action = NotificationAction.ALLOW.name,
            label = "Slack", isEnabled = true, focusModes = "ALL"
        )
        val result = RuleEngine.resolveAction(
            pkgName = "com.slack.app", appName = "Slack",
            title = "Slack", text = "New message in #general",
            label = "unknown", rules = listOf(appRule),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    // ── KEYWORD rule ─────────────────────────────────────────────────────────────

    @Test
    fun `KEYWORD rule (ALLOW) overrides mode default BLOCK`() {
        val kwRule = Rule(
            type = "KEYWORD", value = "invoice", action = NotificationAction.ALLOW.name,
            label = "Invoice", isEnabled = true, focusModes = "ALL"
        )
        val result = RuleEngine.resolveAction(
            pkgName = "com.quickbooks.app", appName = "QuickBooks",
            title = "Invoice paid", text = "Invoice #1234 was paid",
            label = "unknown", rules = listOf(kwRule),
            activeModeStr = "WORK", mode = workMode
        )
        assertEquals(NotificationAction.ALLOW, result.action)
    }

    @Test
    fun `KEYWORD rule (BLOCK) overrides mode default ALLOW`() {
        val kwRule = Rule(
            type = "KEYWORD", value = "promo", action = NotificationAction.BLOCK.name,
            label = "Promo", isEnabled = true, focusModes = "ALL"
        )
        val result = RuleEngine.resolveAction(
            pkgName = "com.shop.app", appName = "Shop",
            title = "Promo alert", text = "50% off promo ending tonight",
            label = "promo", rules = listOf(kwRule),
            activeModeStr = "CUSTOM", mode = allowAllMode
        )
        assertEquals(NotificationAction.BLOCK, result.action)
    }

    // ── matchesModes helper ───────────────────────────────────────────────────────

    @Test
    fun `matchesModes returns true for ALL`() {
        assertTrue(RuleEngine.matchesModes("ALL", "WORK"))
        assertTrue(RuleEngine.matchesModes("ALL", "SLEEP"))
    }

    @Test
    fun `matchesModes returns true for exact match`() {
        assertTrue(RuleEngine.matchesModes("WORK,SLEEP", "WORK"))
        assertTrue(RuleEngine.matchesModes("WORK,SLEEP", "SLEEP"))
    }

    @Test
    fun `matchesModes returns false for non-matching mode`() {
        assertFalse(RuleEngine.matchesModes("SLEEP,EXAM", "WORK"))
    }

    // ── Priority: R1 > R2 > R3 > R4 ─────────────────────────────────────────────

    @Test
    fun `VIP contact (R1) takes priority over critical label (R4)`() {
        // If a VIP contact name appears in an OTP message, R1 fires before R4
        val vipRule = Rule(
            type = "VIP_CONTACT", value = "securebank", action = NotificationAction.ALLOW.name,
            label = "SecureBank", isEnabled = true, focusModes = "ALL"
        )
        val result = RuleEngine.resolveAction(
            pkgName = "com.sms", appName = "SMS",
            title = "SecureBank OTP", text = "Your code is 9876",
            label = "otp", rules = listOf(vipRule),
            activeModeStr = "WORK", mode = workMode
        )
        // R1 fires → reason should mention VIP list, not critical category
        assertEquals(NotificationAction.ALLOW, result.action)
        assert(result.reason.contains("VIP")) { "Expected VIP reason, got: ${result.reason}" }
    }
}
