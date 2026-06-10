// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.adapter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusfilter.R
import com.focusfilter.databinding.ItemAppSelectBinding
import com.focusfilter.viewmodel.TrustedAppsViewModel

class AppSelectionAdapter(
    private val onToggle: (packageName: String, trusted: Boolean) -> Unit
) : ListAdapter<TrustedAppsViewModel.InstalledAppInfo, AppSelectionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemAppSelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrustedAppsViewModel.InstalledAppInfo) {
            val pm = binding.root.context.packageManager

            binding.tvAppName.text     = item.appName
            binding.tvPackageName.text = item.packageName

            try {
                binding.ivAppIcon.setImageDrawable(pm.getApplicationIcon(item.packageName))
            } catch (_: PackageManager.NameNotFoundException) {
                binding.ivAppIcon.setImageResource(R.drawable.ic_app_placeholder)
            }

            // Explicit boolean guard prevents the listener firing during the
            // programmatic isChecked update. Using tag would work but is fragile —
            // any other code that sets tag for another reason silently breaks it.
            var isBinding = false

            isBinding = true
            binding.switchTrusted.isChecked = item.isTrusted
            isBinding = false

            binding.switchTrusted.setOnCheckedChangeListener { _, checked ->
                if (isBinding) return@setOnCheckedChangeListener
                onToggle(item.packageName, checked)
            }

            binding.root.setOnClickListener {
                val newState = !item.isTrusted
                binding.switchTrusted.isChecked = newState
                onToggle(item.packageName, newState)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemAppSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TrustedAppsViewModel.InstalledAppInfo>() {
            override fun areItemsTheSame(a: TrustedAppsViewModel.InstalledAppInfo,
                                         b: TrustedAppsViewModel.InstalledAppInfo) =
                a.packageName == b.packageName

            override fun areContentsTheSame(a: TrustedAppsViewModel.InstalledAppInfo,
                                             b: TrustedAppsViewModel.InstalledAppInfo) =
                a == b
        }
    }
}
