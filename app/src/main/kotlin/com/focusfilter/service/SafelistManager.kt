// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Hard-coded safelist ensuring critical app categories are NEVER silently blocked.
 *
 * Protection tiers:
 *  SYSTEM    — OS components essential for device function.
 *  FINANCE   — Banks, UPI, eWallets — blocking = trust catastrophe.
 *  AUTH      — OTP / 2-FA authenticators — blocking = account lockout.
 *  EMERGENCY — Emergency alerts, safety apps — must always pass.
 *  COMMS     — Phone, SMS, messaging — even a 1-second window is unacceptable.
 */
object SafelistManager {

    enum class ProtectionTier(val label: String) {
        SYSTEM("System app"),
        FINANCE("Banking / Finance"),
        AUTH("Authentication / OTP"),
        EMERGENCY("Emergency"),
        COMMS("Calls / Messages"),
        CALLS_ONLY("Calls Only")
    }

    data class SafelistEntry(val tier: ProtectionTier, val reason: String)

    // ── Exact package names ──────────────────────────────────────────────────────

    private val EXACT: Map<String, SafelistEntry> = mapOf(
        // ── Google core ──────────────────────────────────────────────────────────
        "com.google.android.dialer"                to SafelistEntry(ProtectionTier.COMMS,     "Google Phone"),
        "com.google.android.apps.messaging"        to SafelistEntry(ProtectionTier.COMMS,     "Google Messages"),
        "com.google.android.gms"                   to SafelistEntry(ProtectionTier.SYSTEM,    "Google Play Services"),
        // ── AOSP comms ───────────────────────────────────────────────────────────
        "com.android.phone"                        to SafelistEntry(ProtectionTier.COMMS,     "AOSP Phone"),
        "com.android.mms"                          to SafelistEntry(ProtectionTier.COMMS,     "AOSP MMS"),
        "com.android.messaging"                    to SafelistEntry(ProtectionTier.COMMS,     "AOSP Messaging"),
        // ── Popular messaging apps (COMMS) ───────────────────────────────────────
        "com.whatsapp"                             to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: WhatsApp"),
        "com.whatsapp.w4b"                         to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: WhatsApp Business"),
        "org.telegram.messenger"                   to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Telegram"),
        "org.telegram.plus"                        to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Telegram+"),
        "org.telegram.messenger.web"               to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Telegram"),
        "org.thoughtcrime.securesms"               to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Signal"),
        "com.viber.voip"                           to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Viber"),
        "com.skype.raider"                         to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Skype"),
        "com.microsoft.teams"                      to SafelistEntry(ProtectionTier.CALLS_ONLY, "Calls: Microsoft Teams"),
        // ── System clock / alarm apps (SYSTEM) ──────────────────────────────────
        "com.google.android.deskclock"             to SafelistEntry(ProtectionTier.SYSTEM,    "Google Clock"),
        "com.samsung.android.app.clockpackage"     to SafelistEntry(ProtectionTier.SYSTEM,    "Samsung Clock"),
        "com.sec.android.app.clockpackage"         to SafelistEntry(ProtectionTier.SYSTEM,    "Samsung Clock (Sec)"),
        "com.huawei.android.clock"                 to SafelistEntry(ProtectionTier.SYSTEM,    "Huawei Clock"),
        "com.oneplus.clock"                        to SafelistEntry(ProtectionTier.SYSTEM,    "OnePlus Clock"),
        "com.asus.alarmclock"                      to SafelistEntry(ProtectionTier.SYSTEM,    "ASUS Clock"),
        "com.lge.clock"                            to SafelistEntry(ProtectionTier.SYSTEM,    "LG Clock"),
        "com.motorola.alarmclock"                  to SafelistEntry(ProtectionTier.SYSTEM,    "Motorola Clock"),
        "com.sony.alarm"                           to SafelistEntry(ProtectionTier.SYSTEM,    "Sony Clock"),
        "com.htc.android.worldclock"               to SafelistEntry(ProtectionTier.SYSTEM,    "HTC Clock"),
        "com.realme.clock"                         to SafelistEntry(ProtectionTier.SYSTEM,    "Realme Clock"),
        "com.nothing.clock"                        to SafelistEntry(ProtectionTier.SYSTEM,    "Nothing Clock"),
        // ── Emergency ────────────────────────────────────────────────────────────
        "com.android.emergency"                    to SafelistEntry(ProtectionTier.EMERGENCY, "Emergency Info"),
        "com.google.android.apps.safetyhub"        to SafelistEntry(ProtectionTier.EMERGENCY, "Google Safety Hub"),
        // ── Authenticators ───────────────────────────────────────────────────────
        "com.google.android.apps.authenticator2"   to SafelistEntry(ProtectionTier.AUTH,      "Google Authenticator"),
        "com.authy.authy"                          to SafelistEntry(ProtectionTier.AUTH,      "Authy"),
        "com.lastpass.authenticator"               to SafelistEntry(ProtectionTier.AUTH,      "LastPass Authenticator"),
        "com.microsoft.authenticator"              to SafelistEntry(ProtectionTier.AUTH,      "Microsoft Authenticator"),
        "org.twofactorauth"                        to SafelistEntry(ProtectionTier.AUTH,      "2FAS Auth"),
        "net.bitwarden.authenticator"              to SafelistEntry(ProtectionTier.AUTH,      "Bitwarden Authenticator"),
        // ── India: UPI / Wallets ─────────────────────────────────────────────────
        "com.phonepe.app"                          to SafelistEntry(ProtectionTier.FINANCE,   "PhonePe"),
        "net.one97.paytm"                          to SafelistEntry(ProtectionTier.FINANCE,   "Paytm"),
        "com.google.android.apps.nbu.paisa.user"   to SafelistEntry(ProtectionTier.FINANCE,   "Google Pay"),
        "com.amazon.mShop.android.shopping"        to SafelistEntry(ProtectionTier.FINANCE,   "Amazon Pay"),
        "com.dreamplug.androidapp"                 to SafelistEntry(ProtectionTier.FINANCE,   "CRED"),
        "com.mobikwik_new"                         to SafelistEntry(ProtectionTier.FINANCE,   "MobiKwik"),
        "com.freecharge.android"                   to SafelistEntry(ProtectionTier.FINANCE,   "FreeCharge"),
        // ── Malaysia: Touch 'n Go + eWallets ─────────────────────────────────────
        "com.tngdigital.ewallet"                   to SafelistEntry(ProtectionTier.FINANCE,   "Touch 'n Go eWallet"),
        "my.com.boost"                             to SafelistEntry(ProtectionTier.FINANCE,   "Boost eWallet"),
        "com.maybank2u.life"                       to SafelistEntry(ProtectionTier.FINANCE,   "MAE by Maybank"),
        "com.maybank2u.mae"                        to SafelistEntry(ProtectionTier.FINANCE,   "MAE by Maybank"),
        "my.com.bigpay"                            to SafelistEntry(ProtectionTier.FINANCE,   "BigPay"),
        "my.com.setel"                             to SafelistEntry(ProtectionTier.FINANCE,   "Setel (Petronas)"),
        // ── SEA: Regional eWallets ───────────────────────────────────────────────
        "com.grabtaxi.passenger"                   to SafelistEntry(ProtectionTier.FINANCE,   "Grab / GrabPay"),
        "com.gojek.app"                            to SafelistEntry(ProtectionTier.FINANCE,   "Gojek / GoPay"),
        "ovo.id"                                   to SafelistEntry(ProtectionTier.FINANCE,   "OVO"),
        "id.dana"                                  to SafelistEntry(ProtectionTier.FINANCE,   "DANA"),
        "com.bankjago.android"                     to SafelistEntry(ProtectionTier.FINANCE,   "Jago"),
        "com.globe.gcash.android"                  to SafelistEntry(ProtectionTier.FINANCE,   "GCash"),
        "ph.com.globe.paymaya"                     to SafelistEntry(ProtectionTier.FINANCE,   "Maya (PayMaya)"),
        "com.paymaya"                              to SafelistEntry(ProtectionTier.FINANCE,   "Maya"),
        "vn.momo.client"                           to SafelistEntry(ProtectionTier.FINANCE,   "MoMo"),
        "com.vnpay.hdpayment"                      to SafelistEntry(ProtectionTier.FINANCE,   "VNPay"),
        "vn.zalopay.app"                           to SafelistEntry(ProtectionTier.FINANCE,   "ZaloPay"),
        "th.co.truemoney.wallet"                   to SafelistEntry(ProtectionTier.FINANCE,   "TrueMoney"),
        "com.kasikorn.retail.mbanking.wap"         to SafelistEntry(ProtectionTier.FINANCE,   "KBank"),
        // ── International finance ────────────────────────────────────────────────
        "com.paypal.android.p2pmobile"             to SafelistEntry(ProtectionTier.FINANCE,   "PayPal"),
        "com.venmo"                                to SafelistEntry(ProtectionTier.FINANCE,   "Venmo"),
        "com.squareup.cash"                        to SafelistEntry(ProtectionTier.FINANCE,   "Cash App"),
        "com.zelle"                                to SafelistEntry(ProtectionTier.FINANCE,   "Zelle"),
        "com.robinhood.android"                    to SafelistEntry(ProtectionTier.FINANCE,   "Robinhood"),
        "com.coinbase.android"                     to SafelistEntry(ProtectionTier.FINANCE,   "Coinbase")
    )

    // ── Package prefixes ─────────────────────────────────────────────────────────

    private data class PrefixEntry(val prefix: String, val entry: SafelistEntry)

    private val PREFIXES: List<PrefixEntry> = listOf(
        // Android OS — FLAG_SYSTEM verified at runtime for ALL SYSTEM-tier entries
        PrefixEntry("com.android.",          SafelistEntry(ProtectionTier.SYSTEM,  "Android system component")),
        PrefixEntry("android",               SafelistEntry(ProtectionTier.SYSTEM,  "Android framework")),
        // OEM system UI — FLAG_SYSTEM verified so spoofed packages on other devices are rejected
        PrefixEntry("com.samsung.android.",  SafelistEntry(ProtectionTier.SYSTEM,  "Samsung system")),
        PrefixEntry("com.miui.",             SafelistEntry(ProtectionTier.SYSTEM,  "MIUI system")),
        PrefixEntry("com.oneplus.",          SafelistEntry(ProtectionTier.SYSTEM,  "OnePlus system")),
        PrefixEntry("com.oplus.",            SafelistEntry(ProtectionTier.SYSTEM,  "OPPO system")),
        PrefixEntry("com.vivo.",             SafelistEntry(ProtectionTier.SYSTEM,  "Vivo system")),
        PrefixEntry("com.iqoo.",             SafelistEntry(ProtectionTier.SYSTEM,  "Vivo system")),
        PrefixEntry("com.realme.",           SafelistEntry(ProtectionTier.SYSTEM,  "Realme system")),
        PrefixEntry("com.motorola.",         SafelistEntry(ProtectionTier.SYSTEM,  "Motorola system")),
        PrefixEntry("com.google.android.gms",SafelistEntry(ProtectionTier.SYSTEM,  "Google Play Services")),
        // Shopee regional variants (my, id, sg, ph, vn, th, br …)
        PrefixEntry("com.shopee.",           SafelistEntry(ProtectionTier.FINANCE, "ShopeePay")),
        // Indian banks
        PrefixEntry("com.sbi.",              SafelistEntry(ProtectionTier.FINANCE, "SBI bank")),
        PrefixEntry("com.icici.",            SafelistEntry(ProtectionTier.FINANCE, "ICICI bank")),
        PrefixEntry("com.hdfc.",             SafelistEntry(ProtectionTier.FINANCE, "HDFC bank")),
        PrefixEntry("com.axis.",             SafelistEntry(ProtectionTier.FINANCE, "Axis bank")),
        PrefixEntry("com.kotak.",            SafelistEntry(ProtectionTier.FINANCE, "Kotak bank")),
        PrefixEntry("com.indusind.",         SafelistEntry(ProtectionTier.FINANCE, "IndusInd bank")),
        PrefixEntry("com.rbl.",              SafelistEntry(ProtectionTier.FINANCE, "RBL bank")),
        PrefixEntry("com.yesbank.",          SafelistEntry(ProtectionTier.FINANCE, "Yes bank")),
        PrefixEntry("com.pnb.",              SafelistEntry(ProtectionTier.FINANCE, "Punjab National Bank")),
        PrefixEntry("com.boi.",              SafelistEntry(ProtectionTier.FINANCE, "Bank of India")),
        // US / UK banks
        PrefixEntry("com.chase.",            SafelistEntry(ProtectionTier.FINANCE, "Chase")),
        PrefixEntry("com.bofa.",             SafelistEntry(ProtectionTier.FINANCE, "Bank of America")),
        PrefixEntry("com.wellsfargo.",       SafelistEntry(ProtectionTier.FINANCE, "Wells Fargo")),
        PrefixEntry("com.citibank.",         SafelistEntry(ProtectionTier.FINANCE, "Citibank")),
        PrefixEntry("com.barclays.",         SafelistEntry(ProtectionTier.FINANCE, "Barclays")),
        PrefixEntry("com.hsbc.",             SafelistEntry(ProtectionTier.FINANCE, "HSBC")),
        // RSA SecurID
        PrefixEntry("com.rsa.",              SafelistEntry(ProtectionTier.AUTH,    "RSA SecurID"))
    )

    fun getEntry(packageName: String, pm: PackageManager? = null, title: String = "", text: String = ""): SafelistEntry? {
        val safeEntry = EXACT[packageName] ?: run {
            var found: SafelistEntry? = null
            for (pe in PREFIXES) {
                if (packageName.startsWith(pe.prefix)) {
                    if (pe.entry.tier == ProtectionTier.SYSTEM && pm != null) {
                        if (!isActualSystemApp(packageName, pm)) continue
                    }
                    found = pe.entry
                    break
                }
            }
            found
        } ?: return null

        if (safeEntry.tier == ProtectionTier.CALLS_ONLY) {
            val combined = "$title $text".lowercase()
            val isCall = listOf(
                "incoming call", "video call",
                "voice call", "missed call", "is calling",
                "ringing", "call from", "audio call",
                "is video calling", "is audio calling",
                "decline call", "tap to answer",
                "ongoing call", "call ended", "missed video call",
                "missed voice call", "missed audio call"
            ).any { combined.contains(it) }
            if (!isCall) return null
        }

        return safeEntry
    }

    fun isProtected(packageName: String, pm: PackageManager? = null): Boolean =
        getEntry(packageName, pm) != null

    fun protectionReason(packageName: String, pm: PackageManager? = null): String {
        val e = getEntry(packageName, pm) ?: return "Not protected"
        return "${e.tier.label} — ${e.reason}"
    }

    private fun isActualSystemApp(packageName: String, pm: PackageManager): Boolean {
        return try {
            val flags = pm.getApplicationInfo(packageName, 0).flags
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }
}
