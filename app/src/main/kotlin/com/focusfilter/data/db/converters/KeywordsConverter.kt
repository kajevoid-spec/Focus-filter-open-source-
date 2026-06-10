// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.converters

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * Room TypeConverter for [List<String>] ↔ JSON array text.
 *
 * Using JSON arrays (instead of raw CSV) prevents silent breakage when a keyword
 * contains a comma. The fallback in [fromJson] handles legacy CSV rows that existed
 * before the migration to JSON.
 */
class KeywordsConverter {

    @TypeConverter
    fun fromJson(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        // Fast path: valid JSON array
        if (json.trimStart().startsWith('[')) {
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }
        }
        // Fallback: legacy CSV string (pre-migration rows)
        return json.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    @TypeConverter
    fun toJson(keywords: List<String>): String {
        val arr = JSONArray()
        keywords.forEach { arr.put(it) }
        return arr.toString()
    }
}
