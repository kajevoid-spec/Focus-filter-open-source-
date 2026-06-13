// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.logs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.R
import com.focusfilter.adapter.LogAdapter
import com.focusfilter.databinding.FragmentLogsBinding
import com.focusfilter.viewmodel.LogListItem
import com.focusfilter.viewmodel.LogsViewModel

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val vm: LogsViewModel by viewModels()
    private lateinit var adapter: LogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LogAdapter { notification ->
            val timeFormatted = java.text.SimpleDateFormat(
                "MMM d, yyyy · h:mm a", java.util.Locale.getDefault()
            ).format(java.util.Date(notification.timestamp))

            val actionLabel = when (notification.action) {
                "ALLOW"  -> "Allowed"
                "BLOCK"  -> "Blocked"
                "SILENT" -> "Silenced"
                "HOLD"   -> "Held"
                else     -> notification.action.lowercase().replaceFirstChar { it.uppercase() }
            }

            val modeDisplay = notification.activeModeAtTime.lowercase().replaceFirstChar { it.uppercase() }

            val message = buildString {
                if (notification.title.isNotBlank()) appendLine(notification.title)
                if (notification.text.isNotBlank()) appendLine(notification.text)
                appendLine()
                appendLine("$actionLabel · $modeDisplay")
                appendLine(timeFormatted)
                if (notification.blockReason.isNotBlank()) {
                    appendLine()
                    appendLine(notification.blockReason)
                }
            }

            val launchIntent = requireContext().packageManager
                .getLaunchIntentForPackage(notification.packageName)

            val builder = AlertDialog.Builder(requireContext())
                .setTitle(notification.appName)
                .setMessage(message.trim())
                .setNegativeButton("Close", null)

            if (launchIntent != null) {
                builder.setPositiveButton("Open App") { _, _ -> startActivity(launchIntent) }
            }

            builder.show()
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val chip = when (checkedIds.firstOrNull()) {
                R.id.chipBlocked   -> "blocked"
                R.id.chipAllowed   -> "allowed"
                R.id.chipOverride  -> "override"
                R.id.chipSocial    -> "social"
                R.id.chipImportant -> "important"
                else               -> "all"
            }
            vm.filterChip.value = chip
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                vm.searchQuery.value = query
                if (query.length >= 6) {
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                            as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                    binding.etSearch.clearFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            binding.etSearch.clearFocus()
            true
        }

        vm.displayItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val hasData = items.any { it is LogListItem.Entry }
            binding.emptyState.visibility = if (hasData) View.GONE else View.VISIBLE
            binding.recycler.visibility = if (hasData) View.VISIBLE else View.GONE
        }

        binding.ivClearMenu.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear activity logs")
                .setMessage("Choose how many days of logs to remove.")
                .setPositiveButton("Clear all") { _, _ -> vm.clearAllLogs() }
                .setNeutralButton("Older than 7 days") { _, _ -> vm.clearOldLogs(7) }
                .setNegativeButton("Cancel", null)
                .show()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
