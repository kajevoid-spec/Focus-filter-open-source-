// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.modes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.R
import com.focusfilter.adapter.ModeAdapter
import com.focusfilter.viewmodel.ModesViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar

class ModesFragment : Fragment() {

    private val vm: ModesViewModel by viewModels()
    private lateinit var adapter: ModeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_modes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ModeAdapter(
            onActivate = { mode ->
                val activeModeType = vm.activeMode.value?.type
                if (mode.type == activeModeType) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Deactivate ${mode.displayName}?")
                        .setMessage("This will turn off focus filtering.")
                        .setPositiveButton("Deactivate") { _, _ -> vm.deactivateAll() }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    vm.activateMode(mode.type)
                }
            },
            onDelete = { mode ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete \"${mode.displayName}\"?")
                    .setMessage("This action cannot be undone.")
                    .setPositiveButton("Delete") { _, _ -> vm.deleteMode(mode.type) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val emptyState = view.findViewById<View>(R.id.emptyState)
        val fabCreate = view.findViewById<ExtendedFloatingActionButton>(R.id.fabCreateMode)

        vm.allModes.observe(viewLifecycleOwner) { modes ->
            adapter.submitList(modes)
            emptyState.visibility = if (modes.isEmpty()) View.VISIBLE else View.GONE
        }

        vm.activeMode.observe(viewLifecycleOwner) { active ->
            adapter.setActiveMode(active?.type)
        }

        vm.deleteError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show()
                vm.clearDeleteError()
            }
        }

        fabCreate.setOnClickListener {
            findNavController().navigate(R.id.action_modesFragment_to_createModeFragment)
        }

        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
