// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.keyword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.focusfilter.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class KeywordSafelistFragment : Fragment() {

    private val vm: KeywordSafelistViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_keyword_safelist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler  = view.findViewById<RecyclerView>(R.id.recycler)
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val fab        = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        val btnBack    = view.findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val adapter = KeywordAdapter { id ->
            AlertDialog.Builder(requireContext())
                .setTitle("Remove keyword?")
                .setPositiveButton("Remove") { _, _ -> vm.delete(id) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        vm.keywords.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            recycler.visibility   = if (list.isEmpty()) View.GONE   else View.VISIBLE
        }

        fab.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val input = EditText(requireContext()).apply {
            hint = "e.g. urgent, bank, mom"
            setSingleLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add keyword")
            .setMessage("Notifications containing this word will always be shown, regardless of filters.")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val keyword = input.text?.toString()?.trim() ?: ""
                if (keyword.isNotBlank()) {
                    vm.add(keyword)
                } else {
                    Snackbar.make(requireView(), "Keyword cannot be empty.", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
