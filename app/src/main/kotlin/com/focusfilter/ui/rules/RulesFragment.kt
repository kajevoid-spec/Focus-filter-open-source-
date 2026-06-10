// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.rules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.R
import com.focusfilter.adapter.RuleAdapter
import com.focusfilter.data.db.entities.Rule
import com.focusfilter.databinding.FragmentRulesBinding
import com.focusfilter.model.NotificationAction
import com.focusfilter.viewmodel.RulesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class RulesFragment : Fragment() {

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!
    private val vm: RulesViewModel by viewModels()
    private lateinit var adapter: RuleAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupObservers()
        setupFab()
    }

    private fun setupRecycler() {
        adapter = RuleAdapter(
            onToggle = { rule, enabled -> vm.toggleRule(rule.id, enabled) },
            onDelete = { rule ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Rule")
                    .setMessage("Remove \"${rule.label.ifBlank { rule.value }}\"?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ -> vm.deleteRule(rule.id) }
                    .show()
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
    }

    private fun setupObservers() {
        vm.allRules.observe(viewLifecycleOwner) { rules ->
            adapter.submitList(rules)
            binding.tvRuleCount.text = "${rules.size} rules active"
            binding.emptyState.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupFab() {
        binding.fabAddRule.setOnClickListener { showAddRuleDialog() }
    }

    private fun showAddRuleDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_rule, null)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etValue)
        val etLabel = dialogView.findViewById<TextInputEditText>(R.id.etLabel)
        val spinnerType = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerType)
        val spinnerAction = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerAction)

        val types = listOf("KEYWORD", "VIP_CONTACT", "ALLOWED_APP", "CUSTOM")
        val actions = NotificationAction.values().map { it.label }

        spinnerType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types))
        spinnerAction.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, actions))
        spinnerType.setText(types[0], false)
        spinnerAction.setText(actions[0], false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Rule")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val value = etValue.text?.toString()?.trim() ?: return@setPositiveButton
                if (value.isBlank()) return@setPositiveButton
                val label = etLabel.text?.toString()?.trim() ?: value
                val type = spinnerType.text.toString()
                val actionLabel = spinnerAction.text.toString()
                val action = NotificationAction.values().find { it.label == actionLabel }?.name
                    ?: NotificationAction.ALLOW.name

                vm.addRule(Rule(
                    type = type,
                    value = value.lowercase(),
                    action = action,
                    label = label,
                    isEnabled = true,
                    focusModes = "ALL"
                ))
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
