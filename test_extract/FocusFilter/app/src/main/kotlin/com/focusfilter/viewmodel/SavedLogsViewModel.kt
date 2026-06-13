// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.focusfilter.FocusFilterApplication
import kotlinx.coroutines.launch

class SavedLogsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusFilterApplication

    /** Permanently saved notifications, newest first. */
    val savedLogs = app.notificationRepository.getPermanentlySaved().asLiveData()

    fun delete(id: Long) {
        viewModelScope.launch { app.notificationRepository.deleteById(id) }
    }
}
