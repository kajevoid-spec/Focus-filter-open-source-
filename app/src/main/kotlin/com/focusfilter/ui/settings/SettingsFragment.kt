// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.focusfilter.R
import com.focusfilter.databinding.FragmentSettingsBinding
import com.focusfilter.viewmodel.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val vm: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPermissionButtons()
        setupSwitches()
        setupSafety()
        setupAiSection()
        setupTrustedApps()
        setupSavedLogs()
        setupKeywordSafelist()
        setupLegalAndAbout()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupPermissionButtons() {
        binding.btnGrantNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun setupSwitches() {
        vm.startOnBoot.observe(viewLifecycleOwner) { binding.switchStartOnBoot.isChecked = it }
        binding.switchStartOnBoot.setOnCheckedChangeListener { _, checked -> vm.setStartOnBoot(checked) }

        vm.isLogOnlyMode.observe(viewLifecycleOwner) { binding.switchLogOnly.isChecked = it }
        binding.switchLogOnly.setOnCheckedChangeListener { _, checked -> vm.setLogOnlyMode(checked) }

        vm.isAiSpamEnabled.observe(viewLifecycleOwner) { binding.switchAiSpam.isChecked = it }
        binding.switchAiSpam.setOnCheckedChangeListener { _, checked -> vm.setAiSpamEnabled(checked) }
    }

    private fun setupSafety() {
        vm.isBypassActive.observe(viewLifecycleOwner) { active ->
            if (active) {
                binding.tvBypassStatus.text = "Active"
                binding.tvBypassStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_red))
                binding.btnBypassToggle.text = "Cancel Bypass"
                binding.btnBypassToggle.setOnClickListener { vm.cancelEmergencyBypass() }
            } else {
                binding.tvBypassStatus.text = "Off"
                binding.tvBypassStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_teal))
                binding.btnBypassToggle.text = "Pause 1 Hour"
                binding.btnBypassToggle.setOnClickListener { confirmBypass() }
            }
        }
    }

    private fun confirmBypass() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pause Filtering for 1 Hour?")
            .setMessage(
                "All notifications will pass through for 60 minutes.\n\n" +
                "Use this for appointments, emergencies, or when you need full access temporarily."
            )
            .setPositiveButton("Pause 1 Hour") { _, _ -> vm.activateEmergencyBypass(1L) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupAiSection() {
        val green  = ContextCompat.getColor(requireContext(), R.color.accent_teal)
        val amber  = ContextCompat.getColor(requireContext(), R.color.accent_amber)

        vm.isModelLoaded.observe(viewLifecycleOwner) { loaded ->
            if (loaded) {
                binding.tvModelStatus.text = "Model loaded"
                binding.tvModelStatus.setTextColor(green)
            } else {
                binding.tvModelStatus.text = "Loading model…"
                binding.tvModelStatus.setTextColor(amber)
            }
            updateAiControlsVisibility(binding.switchAiSpam.isChecked)
        }

        vm.spamThreshold.observe(viewLifecycleOwner) { threshold ->
            if (binding.sliderSpamThreshold.value != threshold) {
                binding.sliderSpamThreshold.value = threshold
            }
            binding.tvThresholdValue.text = "%.0f%%".format(threshold * 100)
        }

        binding.sliderSpamThreshold.addOnChangeListener { _, value, fromUser ->
            if (fromUser) vm.setSpamThreshold(value)
        }

        binding.switchAiSpam.setOnCheckedChangeListener { _, checked ->
            vm.setAiSpamEnabled(checked)
            updateAiControlsVisibility(checked)
        }
        updateAiControlsVisibility(binding.switchAiSpam.isChecked)
    }

    private fun updateAiControlsVisibility(aiEnabled: Boolean) {
        val visibility = if (aiEnabled) View.VISIBLE else View.GONE
        binding.layoutThreshold.visibility = visibility
        binding.tvModelStatus.visibility   = visibility
    }

    private fun setupTrustedApps() {
        binding.rowTrustedApps.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_trustedApps)
        }
    }

    private fun setupSavedLogs() {
        binding.rowSavedLogs.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_savedLogs)
        }
    }

    private fun setupKeywordSafelist() {
        binding.rowKeywordSafelist.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_keywordSafelist)
        }
    }

    private fun setupLegalAndAbout() {
        binding.btnPrivacyPolicy.setOnClickListener {
            openUrl("https://focusfiltersite.netlify.app/legal")
        }
        binding.btnTermsOfService.setOnClickListener {
            openUrl("https://focusfiltersite.netlify.app/legal")
        }
        binding.btnAboutWebsite.setOnClickListener {
            openUrl("https://focusfiltersite.netlify.app/")
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun updatePermissionStatus() {
        val notifGranted = isNotificationListenerEnabled()

        val green = ContextCompat.getColor(requireContext(), R.color.accent_teal)
        val red   = ContextCompat.getColor(requireContext(), R.color.accent_red)

        binding.tvNotifStatus.text  = if (notifGranted) "Granted" else "Not granted"
        binding.tvNotifStatus.setTextColor(if (notifGranted) green else red)
        binding.btnGrantNotification.visibility = if (notifGranted) View.GONE else View.VISIBLE
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            requireContext().contentResolver, "enabled_notification_listeners") ?: ""
        return enabled.contains(requireContext().packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
