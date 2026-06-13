// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.adapter

import android.graphics.drawable.Drawable
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
import com.focusfilter.data.db.entities.FilteredNotification
import com.focusfilter.model.NotificationAction
import com.focusfilter.viewmodel.LogListItem
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter(
    private val onItemClick: ((FilteredNotification) -> Unit)? = null
) : ListAdapter<LogListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1

        val DIFF = object : DiffUtil.ItemCallback<LogListItem>() {
            override fun areItemsTheSame(a: LogListItem, b: LogListItem): Boolean = when {
                a is LogListItem.DateHeader && b is LogListItem.DateHeader -> a.label == b.label
                a is LogListItem.Entry && b is LogListItem.Entry -> a.notification.id == b.notification.id
                else -> false
            }
            override fun areContentsTheSame(a: LogListItem, b: LogListItem): Boolean = a == b
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is LogListItem.DateHeader -> TYPE_HEADER
        is LogListItem.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
            EntryViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LogListItem.DateHeader -> (holder as HeaderViewHolder).bind(item)
            is LogListItem.Entry -> (holder as EntryViewHolder).bind(item.notification)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        fun bind(item: LogListItem.DateHeader) { tvDate.text = item.label }
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvAction: TextView = itemView.findViewById(R.id.tvAction)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvMode: TextView = itemView.findViewById(R.id.tvMode)

        fun bind(item: FilteredNotification) {
            val ctx = itemView.context

            // App icon
            ivAppIcon.setImageDrawable(loadAppIcon(item.packageName) ?: ContextCompat.getDrawable(ctx, R.drawable.ic_app_placeholder))

            tvAppName.text = item.appName
            tvTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.timestamp))

            // Action badge
            val (actionLabel, actionColor) = when {
                item.isOverride -> "Override" to ContextCompat.getColor(ctx, R.color.accent_teal)
                item.action == NotificationAction.BLOCK.name -> "Blocked" to ContextCompat.getColor(ctx, R.color.accent_red)
                item.action == NotificationAction.HOLD.name -> "Held" to ContextCompat.getColor(ctx, R.color.accent_amber)
                item.action == NotificationAction.SILENT.name -> "Silenced" to ContextCompat.getColor(ctx, R.color.text_secondary)
                item.action == NotificationAction.ALLOW.name -> "Allowed" to ContextCompat.getColor(ctx, R.color.accent_teal)
                else -> item.action to ContextCompat.getColor(ctx, R.color.text_secondary)
            }
            tvAction.text = actionLabel
            tvAction.setTextColor(actionColor)

            // Human-readable reason
            tvReason.text = item.blockReason.ifBlank {
                if (item.action == NotificationAction.ALLOW.name) "Notification was allowed through."
                else "Notification was filtered."
            }

            // Mode badge
            val modeName = item.activeModeAtTime.lowercase()
                .split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            tvMode.text = modeName

            itemView.setOnClickListener { onItemClick?.invoke(item) }
        }

        private fun loadAppIcon(packageName: String): Drawable? {
            return try {
                itemView.context.packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
        }
    }
}
