// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.home

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.focusfilter.FocusFilterApplication
import com.focusfilter.R
import com.focusfilter.databinding.FragmentHomeBinding
import com.focusfilter.model.FocusModeType
import com.focusfilter.viewmodel.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()
    private var suppressToggleListener = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupModeCards()
        setupToggle()
        setupObservers()

        val prefs = (requireContext().applicationContext as FocusFilterApplication).preferencesManager
        if (!prefs.hasCompletedOnboarding) {
            view.post {
                if (isAdded) findNavController().navigate(R.id.action_home_to_trustedAppsOnboarding)
            }
        }

        val tabNavOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.nav_graph, false)
            .build()

        binding.tvManageModes.setOnClickListener {
            findNavController().navigate(R.id.modesFragment, null, tabNavOptions)
        }

        binding.tvViewAll.setOnClickListener {
            findNavController().navigate(R.id.logsFragment, null, tabNavOptions)
        }

        val seenPrefs = requireContext().getSharedPreferences("ff_seen", 0)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            !seenPrefs.getBoolean("android9_warning_shown", false)) {
            Snackbar.make(
                binding.root,
                "Android 9 or below detected. Some features may not work as intended.",
                Snackbar.LENGTH_LONG
            ).show()
            seenPrefs.edit().putBoolean("android9_warning_shown", true).apply()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupObservers() {
        vm.activeMode.observe(viewLifecycleOwner) { mode ->
            val isActive = mode != null && vm.isFocusEnabled
            val rawName = mode?.displayName?.takeIf { it.isNotBlank() }
                ?: FocusModeType.values().find { it.name == mode?.type }?.displayName
                ?: "Focus Mode"
            val modeName = rawName.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trim()
            updateStatusCard(isActive, mode?.type, modeName)
            updateModeCardHighlight(mode?.type)
            updateSummary()
        }

        vm.filteredTodayCount.observe(viewLifecycleOwner) { count ->
            binding.tvFilteredCount.text = count.toString()
        }

        vm.allowedTodayCount.observe(viewLifecycleOwner) { count ->
            binding.tvAllowedCount.text = count.toString()
        }

        vm.latestEntry.observe(viewLifecycleOwner) { entry ->
            if (entry != null) {
                binding.latestCard.visibility = View.VISIBLE
                binding.tvLatestApp.text = entry.appName
                binding.tvLatestAction.text = buildString {
                    append(entry.action.lowercase().replaceFirstChar { it.uppercase() })
                    append(" · ")
                    append(entry.classifierLabel.replaceFirstChar { it.uppercase() })
                }
                binding.tvLatestTime.text = formatTime(entry.timestamp)

                try {
                    val icon = requireContext().packageManager.getApplicationIcon(entry.packageName)
                    binding.ivLatestIcon.setImageDrawable(icon)
                } catch (e: Exception) {
                    binding.ivLatestIcon.setImageResource(R.drawable.ic_app_placeholder)
                }
            } else {
                binding.latestCard.visibility = View.GONE
            }
        }
    }

    private fun setupToggle() {
        binding.switchFocus.setOnCheckedChangeListener { _, checked ->
            if (suppressToggleListener) return@setOnCheckedChangeListener
            if (checked) {
                if (vm.isFocusEnabled) return@setOnCheckedChangeListener
                val selected = getSelectedMode()
                if (selected == null) {
                    suppressToggleListener = true
                    binding.switchFocus.isChecked = false
                    suppressToggleListener = false
                    Snackbar.make(requireView(), "Select a focus mode first", Snackbar.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                if (!isNotificationListenerEnabled()) {
                    suppressToggleListener = true
                    binding.switchFocus.isChecked = false
                    suppressToggleListener = false
                    Snackbar.make(requireView(), "Grant notification access first", Snackbar.LENGTH_LONG)
                        .setAction("Grant") {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }.show()
                    return@setOnCheckedChangeListener
                }
                activateModeWithBatteryCheck(selected)
                animateStatusCard(true)
            } else {
                vm.deactivateFocus()
                animateStatusCard(false)
            }
        }
    }

    private fun activateModeWithBatteryCheck(mode: FocusModeType) {
        val bm    = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        when {
            level in 1..15 -> {
                // Hard confirmation dialog — user must explicitly choose to continue
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Low Battery Warning")
                    .setMessage("Your battery is at $level%. Enabling Focus Mode may use additional battery.\n\nContinue anyway?")
                    .setPositiveButton("Continue") { _, _ -> vm.activateMode(mode) }
                    .setNegativeButton("Cancel") { _, _ ->
                        suppressToggleListener = true
                        binding.switchFocus.isChecked = false
                        suppressToggleListener = false
                    }
                    .show()
            }
            level in 16..20 -> {
                // Soft informational warning — does not block activation
                Snackbar.make(
                    requireView(),
                    "Battery is getting low — Focus Mode may be affected",
                    Snackbar.LENGTH_LONG
                ).show()
                vm.activateMode(mode)
            }
            else -> vm.activateMode(mode)
        }
    }

    private fun setupModeCards() {
        binding.cardGaming.setOnClickListener { selectMode(FocusModeType.GAMING) }
        binding.cardWork.setOnClickListener { selectMode(FocusModeType.WORK) }
        binding.cardSleep.setOnClickListener { selectMode(FocusModeType.SLEEP) }
        binding.cardCustom.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createMode)
        }
    }

    private var selectedMode: FocusModeType? = null

    private fun selectMode(mode: FocusModeType) {
        selectedMode = mode
        updateModeCardHighlight(mode.name)
        if (vm.isFocusEnabled) {
            vm.activateMode(mode)
        }
    }

    private fun getSelectedMode(): FocusModeType? = selectedMode

    private fun updateModeCardHighlight(activeType: String?) {
        val accent = ContextCompat.getColor(requireContext(), R.color.accent_purple)
        val default = ContextCompat.getColor(requireContext(), R.color.divider)

        listOf(
            FocusModeType.GAMING to binding.cardGaming,
            FocusModeType.WORK to binding.cardWork,
            FocusModeType.SLEEP to binding.cardSleep,
            FocusModeType.CUSTOM to binding.cardCustom
        ).forEach { (mode, card) ->
            val isSelected = mode.name == activeType
            card.strokeColor = if (isSelected) accent else default
            card.strokeWidth = if (isSelected) resources.getDimensionPixelSize(R.dimen.card_stroke_selected) else 1
            if (isSelected && selectedMode == null) selectedMode = mode
        }
        binding.ivCheckGaming.visibility = if (activeType == FocusModeType.GAMING.name) View.VISIBLE else View.GONE
        binding.ivCheckWork.visibility = if (activeType == FocusModeType.WORK.name) View.VISIBLE else View.GONE
        binding.ivCheckSleep.visibility = if (activeType == FocusModeType.SLEEP.name) View.VISIBLE else View.GONE
        binding.ivCheckCustom.visibility = if (activeType == FocusModeType.CUSTOM.name) View.VISIBLE else View.GONE
    }

    private fun updateStatusCard(isActive: Boolean, modeType: String?, modeName: String) {
        suppressToggleListener = true
        binding.switchFocus.isChecked = isActive
        suppressToggleListener = false

        if (isActive) {
            binding.tvStatusTitle.text = getString(R.string.focus_mode_active)
            binding.tvModeName.text = modeName
            binding.tvProtectedMessage.text = getString(R.string.protected_message)
            binding.ivModeIcon.setImageResource(modeIconRes(modeType))
            binding.ivModeIcon.visibility = View.VISIBLE
        } else {
            binding.tvStatusTitle.text = getString(R.string.focus_mode_inactive)
            binding.tvModeName.text = "No active mode"
            binding.tvProtectedMessage.text = getString(R.string.inactive_message)
            binding.ivModeIcon.visibility = View.INVISIBLE
        }
    }

    private fun modeIconRes(modeType: String?): Int = when (modeType) {
        FocusModeType.GAMING.name -> R.drawable.ic_gamepad
        FocusModeType.WORK.name   -> R.drawable.ic_briefcase
        FocusModeType.SLEEP.name  -> R.drawable.ic_moon
        FocusModeType.CUSTOM.name -> R.drawable.ic_settings
        else                      -> R.drawable.ic_shield_small
    }

    private fun updateSummary() {
        binding.tvFocusTime.text = vm.getFocusTimeFormatted()
    }

    private fun animateStatusCard(active: Boolean) {
        val anim = if (active) R.anim.scale_in else R.anim.fade_out
        binding.cardStatus.startAnimation(AnimationUtils.loadAnimation(requireContext(), anim))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        return enabledListeners.contains(requireContext().packageName)
    }

    private fun formatTime(ts: Long): String {
        val now     = java.util.Calendar.getInstance()
        val then    = java.util.Calendar.getInstance().also { it.timeInMillis = ts }
        val diffMs  = now.timeInMillis - ts
        val minutes = diffMs / 60_000
        val hours   = minutes / 60

        fun sameDay(a: java.util.Calendar, b: java.util.Calendar) =
            a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)

        fun isYesterday(past: java.util.Calendar, ref: java.util.Calendar): Boolean {
            val y = ref.clone() as java.util.Calendar
            y.add(java.util.Calendar.DAY_OF_YEAR, -1)
            return sameDay(past, y)
        }

        return when {
            minutes < 1  -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            sameDay(then, now) -> "${hours}h ago"
            isYesterday(then, now) -> "Yesterday"
            else -> {
                val daysAgo = ((now.timeInMillis - then.timeInMillis) /
                    (24 * 60 * 60 * 1_000)).toInt()
                if (daysAgo <= 6) "${daysAgo}d ago"
                else java.text.SimpleDateFormat("dd MMM",
                    java.util.Locale.getDefault()).format(java.util.Date(ts))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
