// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusfilter.R
import com.focusfilter.data.db.entities.Rule
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class RuleAdapter(
    private val onToggle: (Rule, Boolean) -> Unit,
    private val onDelete: (Rule) -> Unit
) : ListAdapter<Rule, RuleAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rule, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvAction: TextView = itemView.findViewById(R.id.tvAction)
        private val switchEnabled: MaterialSwitch = itemView.findViewById(R.id.switchEnabled)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(rule: Rule) {
            tvLabel.text = rule.label.ifBlank { rule.value }
            tvType.text = rule.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            tvAction.text = rule.action
            switchEnabled.isChecked = rule.isEnabled
            switchEnabled.setOnCheckedChangeListener { _, checked -> onToggle(rule, checked) }
            btnDelete.setOnClickListener { onDelete(rule) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Rule>() {
            override fun areItemsTheSame(a: Rule, b: Rule) = a.id == b.id
            override fun areContentsTheSame(a: Rule, b: Rule) = a == b
        }
    }
}
