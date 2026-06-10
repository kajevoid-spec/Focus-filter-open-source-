// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import com.focusfilter.data.db.entities.FocusMode
import com.focusfilter.data.db.entities.Rule
import com.focusfilter.model.NotificationAction

/**
 * Pure rule-matching logic. No database, no Android context, no coroutines — fully unit-testable.
 *
 * Decision order:
 *  R1 — VIP contacts          → ALLOW
 *  R2 — Allowed apps          → ALLOW (or action from rule)
 *  R3 — Keyword rules         → ALLOW / BLOCK per rule
 *  R4 — Custom rules          → ALLOW / BLOCK per rule
 *  R5 — Mode keyword overrides → ALLOW / BLOCK
 *  R6 — Mode default action   → fallback
 */
object RuleEngine {

    data class ActionResult(val action: NotificationAction, val reason: String)

    fun resolveAction(
        pkgName: String,
        appName: String,
        title: String,
        text: String,
        label: String,
        rules: List<Rule>,
        activeModeStr: String,
        mode: FocusMode?
    ): ActionResult {
        val combined = "$title $text".lowercase()
        val modeName = mode?.displayName?.ifBlank { null }
            ?: activeModeStr.lowercase().replaceFirstChar { it.uppercase() }

        // R1 — VIP contacts
        rules.filter { it.type == "VIP_CONTACT" && matchesModes(it.focusModes, activeModeStr) }
            .forEach { rule ->
                if (combined.contains(rule.value.lowercase())) {
                    val name = rule.label.ifBlank { rule.value }
                    return ActionResult(
                        NotificationAction.ALLOW,
                        "✓ Allowed — $name is on your VIP contact list."
                    )
                }
            }

        // R2 — Allowed apps (startsWith + "." boundary prevents crafted package name spoofing)
        rules.filter { it.type == "ALLOWED_APP" && matchesModes(it.focusModes, activeModeStr) }
            .forEach { rule ->
                if (pkgName == rule.value || pkgName.startsWith(rule.value + ".")) {
                    val action = runCatching { NotificationAction.valueOf(rule.action) }
                        .getOrDefault(NotificationAction.ALLOW)
                    val name = rule.label.ifBlank { appName }
                    return ActionResult(
                        action,
                        "✓ Allowed — $name is on your allowed apps list."
                    )
                }
            }

        // R3 — Keyword rules
        rules.filter { it.type == "KEYWORD" && matchesModes(it.focusModes, activeModeStr) }
            .forEach { rule ->
                if (combined.contains(rule.value.lowercase())) {
                    val action = runCatching { NotificationAction.valueOf(rule.action) }
                        .getOrDefault(NotificationAction.ALLOW)
                    val kw   = rule.label.ifBlank { rule.value }
                    val verb = if (action == NotificationAction.ALLOW) "✓ Allowed" else "✗ Blocked"
                    return ActionResult(
                        action,
                        "$verb — matched keyword rule \"$kw\"."
                    )
                }
            }

        // R4 — Custom rules
        rules.filter { it.type == "CUSTOM" && matchesModes(it.focusModes, activeModeStr) }
            .forEach { rule ->
                if (combined.contains(rule.value.lowercase())) {
                    val action = runCatching { NotificationAction.valueOf(rule.action) }
                        .getOrDefault(NotificationAction.BLOCK)
                    val name = rule.label.ifBlank { rule.value }
                    return ActionResult(
                        action,
                        "${if (action == NotificationAction.ALLOW) "✓" else "✗"} Matched custom rule \"$name\"."
                    )
                }
            }

        // R6 — Mode keyword overrides
        if (mode != null) {
            val matchAllow = mode.allowedKeywords.find { combined.contains(it.lowercase()) }
            if (matchAllow != null) {
                return ActionResult(
                    NotificationAction.ALLOW,
                    "✓ Allowed — \"$matchAllow\" is on $modeName's allowed keyword list."
                )
            }

            val matchBlock = mode.blockedKeywords.find { combined.contains(it.lowercase()) }
            if (matchBlock != null) {
                val action = runCatching { NotificationAction.valueOf(mode.defaultAction) }
                    .getOrDefault(NotificationAction.BLOCK)
                return ActionResult(
                    action,
                    "✗ Blocked — \"$matchBlock\" is on $modeName's blocked keyword list."
                )
            }

            // R7 — Mode default action
            val defaultAction = runCatching { NotificationAction.valueOf(mode.defaultAction) }
                .getOrDefault(NotificationAction.BLOCK)

            // Unknown label should never be silently blocked — hold for review instead.
            val finalAction = if (label == "unknown" && defaultAction == NotificationAction.BLOCK) {
                NotificationAction.HOLD
            } else defaultAction

            return ActionResult(finalAction, buildDefaultReason(finalAction, appName, label, modeName))
        }

        return ActionResult(
            NotificationAction.BLOCK,
            "✗ $appName blocked — $modeName is active and no rule matched."
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun buildDefaultReason(
        action: NotificationAction,
        appName: String,
        label: String,
        modeName: String
    ): String {
        val category = when (label) {
            "social"    -> "social media"
            "promo"     -> "promotional"
            "spam"      -> "spam"
            "games"     -> "gaming"
            "important" -> "work/calendar"
            "delivery"  -> "delivery"
            else        -> null
        }

        val subject = if (category != null) {
            "${category.replaceFirstChar { it.uppercase() }} notifications from $appName"
        } else {
            "Notifications from $appName"
        }

        val ruleNote = "Mode default — no specific rule matched."

        return when (action) {
            NotificationAction.BLOCK  ->
                "✗ $subject blocked during $modeName. $ruleNote"
            NotificationAction.SILENT ->
                "$subject silenced during $modeName. $ruleNote"
            NotificationAction.HOLD   ->
                "$subject held during $modeName. $ruleNote"
            NotificationAction.ALLOW  ->
                "✓ $subject allowed by $modeName default."
            else                      ->
                "$appName processed by $modeName."
        }
    }

    fun matchesModes(focusModes: String, activeMode: String): Boolean =
        focusModes == "ALL" || focusModes.split(",").any { it.trim() == activeMode }
}
