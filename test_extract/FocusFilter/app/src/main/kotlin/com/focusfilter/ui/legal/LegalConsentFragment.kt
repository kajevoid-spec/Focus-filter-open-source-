// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.legal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.focusfilter.FocusFilterApplication
import com.focusfilter.R
import com.google.android.material.button.MaterialButton

class LegalConsentFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_legal_consent, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cbPrivacy   = view.findViewById<CheckBox>(R.id.cbPrivacyPolicy)
        val cbTerms     = view.findViewById<CheckBox>(R.id.cbTermsOfService)
        val btnContinue = view.findViewById<MaterialButton>(R.id.btnContinue)

        btnContinue.isEnabled = false

        val updateButton = {
            btnContinue.isEnabled = cbPrivacy.isChecked && cbTerms.isChecked
        }

        cbPrivacy.setOnCheckedChangeListener { _, _ -> updateButton() }
        cbTerms.setOnCheckedChangeListener   { _, _ -> updateButton() }

        view.findViewById<View>(R.id.layoutPrivacyRow)?.setOnClickListener {
            openUrl("https://focusfiltersite.netlify.app/legal")
        }

        view.findViewById<View>(R.id.layoutTermsRow)?.setOnClickListener {
            openUrl("https://focusfiltersite.netlify.app/legal")
        }

        btnContinue.setOnClickListener {
            val app = requireContext().applicationContext as FocusFilterApplication
            app.preferencesManager.hasAcceptedLegal = true
            findNavController().popBackStack()
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
