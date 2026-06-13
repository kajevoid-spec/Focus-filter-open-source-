// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusfilter.R
import com.focusfilter.data.db.entities.FilteredNotification
import com.focusfilter.model.NotificationAction
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onAllowOnce: (FilteredNotification) -> Unit,
    private val onSave: (FilteredNotification) -> Unit,
    private val onDelete: (FilteredNotification) -> Unit,
    private val showSaveAndAllowOnce: Boolean = true
) : ListAdapter<FilteredNotification, NotificationAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView     = itemView.findViewById(R.id.card)
        private val ivAppIcon: ImageView       = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView        = itemView.findViewById(R.id.tvAppName)
        private val tvTitle: TextView          = itemView.findViewById(R.id.tvTitle)
        private val tvBody: TextView           = itemView.findViewById(R.id.tvBody)
        private val tvTime: TextView           = itemView.findViewById(R.id.tvTime)
        private val tvAction: TextView         = itemView.findViewById(R.id.tvAction)
        private val tvLabel: TextView          = itemView.findViewById(R.id.tvLabel)
        private val tvReason: TextView         = itemView.findViewById(R.id.tvReason)

        fun bind(item: FilteredNotification) {
            val ctx = itemView.context

            ivAppIcon.setImageDrawable(
                loadAppIcon(item.packageName)
                    ?: ContextCompat.getDrawable(ctx, R.drawable.ic_app_placeholder)
            )

            tvAppName.text = item.appName
            tvTime.text    = formatTime(item.timestamp)

            tvTitle.text = item.title.ifBlank { item.appName }
            tvBody.text  = item.text.ifBlank { "No content" }
            tvTitle.visibility = if (item.title.isNotBlank()) View.VISIBLE else View.GONE

            val (actionLabel, actionColor) = when (item.action) {
                NotificationAction.BLOCK.name  -> "Blocked"  to ContextCompat.getColor(ctx, R.color.accent_red)
                NotificationAction.HOLD.name   -> "Held"     to ContextCompat.getColor(ctx, R.color.accent_amber)
                NotificationAction.SILENT.name -> "Silenced" to ContextCompat.getColor(ctx, R.color.text_secondary)
                NotificationAction.ALLOW.name  -> "Allowed"  to ContextCompat.getColor(ctx, R.color.accent_teal)
                "OVERRIDE"                     -> "Allowed Once" to ContextCompat.getColor(ctx, R.color.accent_teal)
                else                           -> "Filtered" to ContextCompat.getColor(ctx, R.color.accent_red)
            }
            tvAction.text = actionLabel
            tvAction.setTextColor(actionColor)
            tvLabel.text = item.classifierLabel.replaceFirstChar { it.uppercase() }

            tvReason.text = item.blockReason.ifBlank {
                "Filtered by ${item.activeModeAtTime.lowercase().replaceFirstChar { it.uppercase() }} mode."
            }
            tvReason.visibility = View.VISIBLE

            card.setOnClickListener {
                showDetailDialog(item)
            }

            card.setOnLongClickListener {
                showActionDialog(item)
                true
            }
        }

        private fun showDetailDialog(item: FilteredNotification) {
            val ctx = itemView.context
            val timeFormatted = java.text.SimpleDateFormat(
                "MMM d, yyyy · h:mm a", Locale.getDefault()
            ).format(java.util.Date(item.timestamp))

            val actionLabel = when (item.action) {
                "ALLOW"    -> "Allowed"
                "BLOCK"    -> "Blocked"
                "SILENT"   -> "Silenced"
                "HOLD"     -> "Held"
                "OVERRIDE" -> "Allowed Once"
                else       -> item.action.lowercase().replaceFirstChar { it.uppercase() }
            }

            val message = buildString {
                if (item.text.isNotBlank()) appendLine(item.text)
                appendLine()
                appendLine("$actionLabel · ${item.classifierLabel.replaceFirstChar { it.uppercase() }}")
                appendLine(timeFormatted)
                if (item.blockReason.isNotBlank()) {
                    appendLine()
                    appendLine(item.blockReason)
                }
            }

            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(item.packageName)
            val builder = AlertDialog.Builder(ctx)
                .setTitle(item.appName + if (item.title.isNotBlank()) " · ${item.title}" else "")
                .setMessage(message.trim())
                .setNegativeButton("Close", null)

            if (launchIntent != null) {
                builder.setPositiveButton("Open App") { _, _ -> ctx.startActivity(launchIntent) }
            }
            builder.show()
        }

        private fun showActionDialog(item: FilteredNotification) {
            val ctx = itemView.context
            val options = if (showSaveAndAllowOnce) {
                arrayOf("Allow Once", "Save", "Delete")
            } else {
                arrayOf("Delete")
            }

            AlertDialog.Builder(ctx)
                .setItems(options) { _, which ->
                    if (showSaveAndAllowOnce) {
                        when (which) {
                            0 -> onAllowOnce(item)
                            1 -> onSave(item)
                            2 -> onDelete(item)
                        }
                    } else {
                        when (which) {
                            0 -> onDelete(item)
                        }
                    }
                }
                .show()
        }

        private fun loadAppIcon(packageName: String): Drawable? =
            try { itemView.context.packageManager.getApplicationIcon(packageName) }
            catch (e: Exception) { null }

        private fun formatTime(ts: Long): String {
            val diff    = System.currentTimeMillis() - ts
            val minutes = diff / 60_000
            val hours   = minutes / 60
            return when {
                minutes < 1  -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours   < 24 -> "${hours}h ago"
                else         -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FilteredNotification>() {
            override fun areItemsTheSame(a: FilteredNotification, b: FilteredNotification) = a.id == b.id
            override fun areContentsTheSame(a: FilteredNotification, b: FilteredNotification) = a == b
        }
    }
}
