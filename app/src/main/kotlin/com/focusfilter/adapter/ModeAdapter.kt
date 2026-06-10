// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusfilter.R
import com.focusfilter.data.db.entities.FocusMode
import com.google.android.material.card.MaterialCardView

class ModeAdapter(
    private val onActivate: (FocusMode) -> Unit,
    private val onDelete: ((FocusMode) -> Unit)? = null
) : ListAdapter<FocusMode, ModeAdapter.ViewHolder>(DIFF) {

    private var activeModeType: String? = null

    fun setActiveMode(type: String?) {
        val previous = activeModeType
        activeModeType = type
        if (previous != type) {
            currentList.forEachIndexed { index, mode ->
                if (mode.type == previous || mode.type == type) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mode, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivModeIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvModeName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvModeDesc)
        private val tvSchedule: TextView = itemView.findViewById(R.id.tvSchedule)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val tvAction: TextView = itemView.findViewById(R.id.tvDefaultAction)

        fun bind(mode: FocusMode) {
            val ctx = itemView.context
            val isActive = mode.type == activeModeType

            tvName.text = mode.displayName.ifBlank { mode.type.lowercase().replaceFirstChar { it.uppercase() } }
            tvDesc.text = mode.description

            // Schedule row always hidden (feature removed)
            tvSchedule.visibility = View.GONE

            // Icon
            val iconRes = when (mode.iconName) {
                "ic_gamepad" -> R.drawable.ic_gamepad
                "ic_briefcase" -> R.drawable.ic_briefcase
                "ic_moon" -> R.drawable.ic_moon
                "ic_settings" -> R.drawable.ic_settings
                "ic_timer" -> R.drawable.ic_timer
                "ic_block" -> R.drawable.ic_block
                "ic_check_circle" -> R.drawable.ic_check_circle
                else -> R.drawable.ic_shield
            }
            ivIcon.setImageResource(iconRes)

            // Default action badge
            tvAction.text = mode.defaultAction.lowercase().replaceFirstChar { it.uppercase() }
            tvAction.setTextColor(when (mode.defaultAction) {
                "BLOCK" -> ContextCompat.getColor(ctx, R.color.accent_red)
                "SILENT" -> ContextCompat.getColor(ctx, R.color.text_secondary)
                "HOLD" -> ContextCompat.getColor(ctx, R.color.accent_amber)
                else -> ContextCompat.getColor(ctx, R.color.accent_teal)
            })

            // Active highlight
            val accent = ContextCompat.getColor(ctx, R.color.accent_purple)
            val divider = ContextCompat.getColor(ctx, R.color.divider)
            card.strokeColor = if (isActive) accent else divider
            card.strokeWidth = if (isActive) ctx.resources.getDimensionPixelSize(R.dimen.card_stroke_selected) else 1
            ivCheck.visibility = if (isActive) View.VISIBLE else View.GONE

            // Delete only for user-created, non-built-in custom modes
            if (!mode.isBuiltIn && mode.isCustom && onDelete != null) {
                ivDelete.visibility = View.VISIBLE
                ivDelete.setOnClickListener { onDelete.invoke(mode) }
            } else {
                ivDelete.visibility = View.GONE
                ivDelete.setOnClickListener(null)
            }

            card.setOnClickListener { onActivate(mode) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FocusMode>() {
            override fun areItemsTheSame(a: FocusMode, b: FocusMode) = a.type == b.type
            override fun areContentsTheSame(a: FocusMode, b: FocusMode) = a == b
        }
    }
}
