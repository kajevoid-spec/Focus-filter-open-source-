// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.modes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.focusfilter.R
import com.focusfilter.viewmodel.ModesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

import androidx.navigation.NavOptions

class CreateModeFragment : Fragment() {

    private val vm: ModesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_create_mode, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etDesc = view.findViewById<TextInputEditText>(R.id.etDesc)
        val actvAction = view.findViewById<AutoCompleteTextView>(R.id.actvDefaultAction)
        val etAllowedKw = view.findViewById<TextInputEditText>(R.id.etAllowedKw)
        val etBlockedKw = view.findViewById<TextInputEditText>(R.id.etBlockedKw)
        val actions = listOf("Block", "Allow", "Hold")
        val actionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, actions)
        actvAction.setAdapter(actionAdapter)
        actvAction.setText("Block", false)


        btnBack.setOnClickListener { findNavController().popBackStack() }

        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                Snackbar.make(view, "Please enter a mode name.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actionStr = when (actvAction.text?.toString()?.trim()) {
                "Allow" -> "ALLOW"
                "Hold" -> "HOLD"
                else -> "BLOCK"
            }

            vm.createMode(
                name = name,
                description = etDesc.text?.toString()?.trim() ?: "",
                defaultAction = actionStr,
                silenceCalls = false,
                allowedKeywords = etAllowedKw.text?.toString()?.trim() ?: "",
                blockedKeywords = etBlockedKw.text?.toString()?.trim() ?: ""
            )

            Snackbar.make(view, "\"$name\" created!", Snackbar.LENGTH_SHORT).show()

            // Always navigate to the Modes list so the user sees their new mode,
            // regardless of whether we arrived from Home or Modes.
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.modesFragment, false)
                .build()
            try {
                findNavController().navigate(R.id.modesFragment, null, navOptions)
            } catch (e: Exception) {
                findNavController().popBackStack()
            }
        }
    }
}
