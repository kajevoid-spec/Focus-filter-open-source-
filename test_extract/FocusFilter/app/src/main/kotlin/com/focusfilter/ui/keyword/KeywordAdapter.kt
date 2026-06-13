// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.keyword

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusfilter.R
import com.focusfilter.data.keyword.KeywordSafelist

class KeywordAdapter(
    private val onDelete: (Long) -> Unit
) : ListAdapter<KeywordSafelist, KeywordAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_keyword, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvKeyword: TextView  = itemView.findViewById(R.id.tvKeyword)
        private val ivDelete: ImageView  = itemView.findViewById(R.id.ivDelete)

        fun bind(item: KeywordSafelist) {
            tvKeyword.text = item.keyword
            ivDelete.setOnClickListener { onDelete(item.id) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<KeywordSafelist>() {
            override fun areItemsTheSame(a: KeywordSafelist, b: KeywordSafelist) = a.id == b.id
            override fun areContentsTheSame(a: KeywordSafelist, b: KeywordSafelist) = a == b
        }
    }
}
