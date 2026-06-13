// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.keyword

import android.content.Context
import com.focusfilter.FocusFilterApplication

// BUG 7: suspend matchesKeyword, 5-min TTL cache, no runBlocking
class KeywordSafelistManager(private val context: Context) {

    private val dao: KeywordSafelistDao by lazy {
        (context.applicationContext as FocusFilterApplication).database.keywordSafelistDao()
    }

    @Volatile private var cachedKeywords: List<String>? = null
    @Volatile private var cacheBuiltAt: Long = 0L

    private val CACHE_TTL_MS = 5 * 60 * 1_000L // 5 minutes

    fun invalidateCache() {
        cachedKeywords = null
        cacheBuiltAt  = 0L
    }

    private suspend fun getKeywords(): List<String> {
        val now    = System.currentTimeMillis()
        val cached = cachedKeywords
        if (cached != null && (now - cacheBuiltAt) < CACHE_TTL_MS) return cached
        return dao.getEnabledSync().map { it.keyword.lowercase() }.also {
            cachedKeywords = it
            cacheBuiltAt   = now
        }
    }

    suspend fun matchesKeyword(text: String): Boolean {
        if (text.isBlank()) return false
        val normalized = text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return getKeywords().any { kw ->
            val normalizedKw = kw.lowercase()
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            val pattern = "\\b${Regex.escape(normalizedKw)}\\b"
            Regex(pattern).containsMatchIn(normalized)
        }
    }
}
