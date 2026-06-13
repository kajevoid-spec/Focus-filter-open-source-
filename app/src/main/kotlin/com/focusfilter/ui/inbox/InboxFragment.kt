// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.adapter.NotificationAdapter
import com.focusfilter.databinding.FragmentInboxBinding
import com.focusfilter.service.FocusFilterNotificationService
import com.focusfilter.viewmodel.InboxViewModel
import com.google.android.material.snackbar.Snackbar

class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    private val vm: InboxViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NotificationAdapter(
            onAllowOnce = { notification ->
                // BUG 6: check if the notification is still in the status bar before
                // attempting to allow it. If it has already been dismissed, inform the
                // user instead of silently doing nothing.
                val service      = FocusFilterNotificationService.instance?.get()
                val activeKeys   = service?.activeNotifications
                    ?.map { it.key }
                    ?.toSet()
                    ?: emptySet()
                // Use stored sbnKey for exact match; fall back to package-name check
                // for older rows that predate the sbnKey column (no regression).
                val isStillAlive = notification.sbnKey
                    ?.let { activeKeys.contains(it) }
                    ?: activeKeys.any { it.contains(notification.packageName) }

                if (!isStillAlive && service != null) {
                    Snackbar.make(
                        binding.root,
                        "This notification was already dismissed and cannot be restored",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@NotificationAdapter
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Allow Once")
                    .setMessage("Let \"${notification.appName}\" through just this once?\nThe filter rule stays active.")
                    .setPositiveButton("Allow Once") { _, _ ->
                        vm.allowOnce(notification.id)
                        vm.markAsOverride(notification.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onSave = { notification ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Save Permanently")
                    .setMessage("Save this notification permanently?\nIt will appear in Settings → Saved Notifications and will never be auto-deleted.")
                    .setPositiveButton("Save") { _, _ -> vm.savePermanently(notification.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onDelete = { vm.delete(it.id) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        vm.filteredNotifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val isEmpty = list.isEmpty()
            binding.emptyState.visibility   = if (isEmpty) View.VISIBLE else View.GONE
            binding.recycler.visibility     = if (isEmpty) View.GONE   else View.VISIBLE
            binding.btnClearAll.isEnabled   = !isEmpty
        }

        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear inbox")
                .setMessage("This will permanently remove all held notifications.")
                .setPositiveButton("Clear all") { _, _ -> vm.clearAll() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
