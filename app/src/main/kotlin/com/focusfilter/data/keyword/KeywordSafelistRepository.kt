// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.keyword

import androidx.lifecycle.LiveData

class KeywordSafelistRepository(private val dao: KeywordSafelistDao) {

    fun getAll(): LiveData<List<KeywordSafelist>> = dao.getAll()

    suspend fun add(keyword: String) {
        if (keyword.isBlank()) return
        dao.insert(KeywordSafelist(keyword = keyword.trim().lowercase()))
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
