// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.onboarding

import android.content.Context
import androidx.core.widget.addTextChangedListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.FocusFilterApplication
import com.focusfilter.R
import com.focusfilter.adapter.AppSelectionAdapter
import com.focusfilter.databinding.FragmentTrustedAppsOnboardingBinding
import com.focusfilter.viewmodel.TrustedAppsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TrustedAppsOnboardingFragment : Fragment() {

    private var _binding: FragmentTrustedAppsOnboardingBinding? = null
    private val binding get() = _binding!!
    private val vm: TrustedAppsViewModel by viewModels()
    private lateinit var adapter: AppSelectionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrustedAppsOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prevent skipping onboarding with the back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (vm.trustedCount() < 1) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select at least one app")
                    .setMessage("FocusFilter works best when you trust at least one important app — like your messaging or banking app. Please select one to continue.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                completeonboarding()
            }
        }

        setupRecyclerView()
        setupSearch()   // ADD
        setupObservers()

        binding.btnDone.isEnabled = false
        binding.tvSelectAtLeastOne.visibility = View.VISIBLE

        binding.btnDone.setOnClickListener { completeonboarding() }
    }

    private fun completeonboarding() {
        val app = requireContext().applicationContext as FocusFilterApplication
        app.preferencesManager.hasCompletedOnboarding = true
        findNavController().navigate(R.id.action_trustedAppsOnboarding_to_home)
    }

    private fun setupRecyclerView() {
        adapter = AppSelectionAdapter { packageName, trusted ->
            vm.toggleTrusted(packageName, trusted)
        }

        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@TrustedAppsOnboardingFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        vm.apps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
            val count = vm.trustedCount()
            binding.btnDone.isEnabled = count > 0
            binding.tvSelectAtLeastOne.visibility = if (count > 0) View.GONE else View.VISIBLE
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            vm.search(query)
            if (query.length >= 6) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                binding.etSearch.clearFocus()
            }
        }
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            binding.etSearch.clearFocus()
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
