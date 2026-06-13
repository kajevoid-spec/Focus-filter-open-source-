// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.ui.keyword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.focusfilter.FocusFilterApplication
import com.focusfilter.data.keyword.KeywordSafelist
import com.focusfilter.data.keyword.KeywordSafelistRepository
import kotlinx.coroutines.launch

class KeywordSafelistViewModel(application: Application) : AndroidViewModel(application) {

    private val app  = application as FocusFilterApplication
    private val repo = KeywordSafelistRepository(app.database.keywordSafelistDao())

    val keywords: LiveData<List<KeywordSafelist>> = repo.getAll()

    fun add(keyword: String) = viewModelScope.launch {
        repo.add(keyword)
        app.keywordSafelistManager.invalidateCache()
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id)
        app.keywordSafelistManager.invalidateCache()
    }
}
