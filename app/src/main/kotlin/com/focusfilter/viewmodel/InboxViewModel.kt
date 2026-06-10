// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.focusfilter.FocusFilterApplication
import com.focusfilter.data.db.entities.FilteredNotification
import kotlinx.coroutines.launch

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusFilterApplication

    /** Inbox items: non-restored, non-permanent entries. */
    val filteredNotifications: LiveData<List<FilteredNotification>> =
        app.notificationRepository.getInboxItems().asLiveData()

    fun restore(id: Long) {
        viewModelScope.launch { app.notificationRepository.markRestored(id) }
    }

    fun allowOnce(id: Long) {
        viewModelScope.launch { app.notificationRepository.allowOnce(id) }
    }

    fun savePermanently(id: Long) {
        viewModelScope.launch { app.notificationRepository.markPermanent(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { app.notificationRepository.deleteById(id) }
    }

    fun markAsOverride(id: Long) {
        viewModelScope.launch {
            app.notificationRepository.updateAction(id, "OVERRIDE")
        }
    }

    fun clearAll() {
        // Only clears non-permanent inbox items — saved logs are never touched.
        viewModelScope.launch { app.notificationRepository.clearInboxOnly() }
    }
}
