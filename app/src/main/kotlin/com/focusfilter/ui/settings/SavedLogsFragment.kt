// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.adapter.NotificationAdapter
import com.focusfilter.databinding.FragmentSavedLogsBinding
import com.focusfilter.viewmodel.SavedLogsViewModel

class SavedLogsFragment : Fragment() {

    private var _binding: FragmentSavedLogsBinding? = null
    private val binding get() = _binding!!
    private val vm: SavedLogsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NotificationAdapter(
            onAllowOnce = { },
            onSave      = { },
            onDelete    = { notification ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove from Saved")
                    .setMessage("Permanently delete this saved notification?")
                    .setPositiveButton("Delete") { _, _ -> vm.delete(notification.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            showSaveAndAllowOnce = false
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        vm.savedLogs.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recycler.visibility   = if (list.isEmpty()) View.GONE   else View.VISIBLE
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
