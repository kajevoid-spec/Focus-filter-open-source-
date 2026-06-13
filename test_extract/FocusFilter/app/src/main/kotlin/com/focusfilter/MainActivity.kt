// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.focusfilter.databinding.ActivityMainBinding
import com.focusfilter.viewmodel.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.legalConsentFragment,
                R.id.trustedAppsOnboardingFragment -> bottomNav.visibility = View.GONE
                else                               -> bottomNav.visibility = View.VISIBLE
            }
        }

        val prefs = (applicationContext as FocusFilterApplication).preferencesManager
        if (!prefs.hasAcceptedLegal) {
            navController.navigate(R.id.legalConsentFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check legal acceptance in case user backed out of the consent screen
        val prefs = (applicationContext as FocusFilterApplication).preferencesManager
        if (!prefs.hasAcceptedLegal) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            if (navController.currentDestination?.id != R.id.legalConsentFragment) {
                navController.navigate(R.id.legalConsentFragment)
            }
        }
        // BUG 4: act on revoked notification access
        checkNotificationListenerPermission()
        // BUG 1: one-time battery-optimisation prompt for aggressive OEMs
        maybeShowBatteryOptimizationPrompt()
    }

    // BUG 4: if access is revoked while focus is on, deactivate and show Snackbar
    private fun checkNotificationListenerPermission() {
        val enabled = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: ""
        val hasAccess = enabled.contains(packageName)

        val app   = applicationContext as FocusFilterApplication
        val prefs = app.preferencesManager

        if (!hasAccess && prefs.isFocusEnabled) {
            // Obtain HomeViewModel from the activity scope so the same instance is used
            val homeVm = ViewModelProvider(this)[HomeViewModel::class.java]
            homeVm.deactivateFocus()

            Snackbar.make(
                binding.root,
                "Notification access was revoked — Focus Mode paused",
                Snackbar.LENGTH_LONG
            ).setAction("Grant") {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }.show()
        }
    }

    // BUG 1: show one-time exemption prompt on OEMs known to kill background services
    private fun maybeShowBatteryOptimizationPrompt() {
        val aggressiveOems = setOf("xiaomi", "realme", "oneplus", "oppo", "vivo", "iqoo")
        val manufacturer   = Build.MANUFACTURER.lowercase()
        if (manufacturer !in aggressiveOems) return

        val prefs = (applicationContext as FocusFilterApplication).preferencesManager
        if (prefs.hasShownBatteryOptimizationPrompt) return

        prefs.hasShownBatteryOptimizationPrompt = true

        MaterialAlertDialogBuilder(this)
            .setTitle("Improve reliability on ${Build.MANUFACTURER}")
            .setMessage(
                "${Build.MANUFACTURER} devices aggressively close background apps. " +
                "To keep FocusFilter filtering reliably, please exempt it from battery " +
                "optimization in your phone's battery settings."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .setNegativeButton("Not Now", null)
            .show()
    }
}
