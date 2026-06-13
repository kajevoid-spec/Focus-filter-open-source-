// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusfilter.adapter.AppSelectionAdapter
import com.focusfilter.databinding.FragmentTrustedAppsBinding
import com.focusfilter.viewmodel.TrustedAppsViewModel

class TrustedAppsFragment : Fragment() {

    private var _binding: FragmentTrustedAppsBinding? = null
    private val binding get() = _binding!!
    private val vm: TrustedAppsViewModel by viewModels()
    private lateinit var adapter: AppSelectionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrustedAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupObservers()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = AppSelectionAdapter { packageName, trusted ->
            vm.toggleTrusted(packageName, trusted)
        }

        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@TrustedAppsFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            vm.search(query)
            if (query.length >= 3) {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                binding.etSearch.clearFocus()
            }
        }
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            binding.etSearch.clearFocus()
            true
        }
    }

    private fun setupObservers() {
        vm.apps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
