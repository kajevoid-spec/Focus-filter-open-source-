// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.focusfilter.FocusFilterApplication
import com.focusfilter.data.db.entities.FilteredNotification
import com.focusfilter.model.NotificationAction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class LogListItem {
    data class DateHeader(val label: String) : LogListItem()
    data class Entry(val notification: FilteredNotification) : LogListItem()
}

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusFilterApplication

    val filterChip = MutableLiveData("all")
    val searchQuery = MutableLiveData("")

    private val rawLogs: LiveData<List<FilteredNotification>> =
        app.notificationRepository.getRecentLogs(300).asLiveData()

    val displayItems: LiveData<List<LogListItem>> = MediatorLiveData<List<LogListItem>>().apply {
        val combine = {
            val logs = rawLogs.value ?: emptyList()
            val chip = filterChip.value ?: "all"
            val query = searchQuery.value?.trim() ?: ""
            value = buildGroupedList(applyFilter(logs, chip, query))
        }
        addSource(rawLogs) { combine() }
        addSource(filterChip) { combine() }
        addSource(searchQuery) { combine() }
    }

    private fun applyFilter(logs: List<FilteredNotification>, chip: String, query: String): List<FilteredNotification> {
        var filtered = when (chip) {
            "blocked" -> logs.filter { it.action in listOf(NotificationAction.BLOCK.name, NotificationAction.HOLD.name, NotificationAction.SILENT.name) && !it.isOverride }
            "allowed" -> logs.filter { it.action == NotificationAction.ALLOW.name }
            "override" -> logs.filter { it.isOverride }
            "social" -> logs.filter { it.classifierLabel == "social" }
            "important" -> logs.filter { it.classifierLabel in listOf("otp", "payment", "bank", "emergency", "security") }
            else -> logs
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.classifierLabel.contains(query, ignoreCase = true) ||
                it.blockReason.contains(query, ignoreCase = true)
            }
        }
        return filtered
    }

    private fun buildGroupedList(logs: List<FilteredNotification>): List<LogListItem> {
        val result = mutableListOf<LogListItem>()
        var lastDay = ""
        for (log in logs) {
            val day = getDayLabel(log.timestamp)
            if (day != lastDay) {
                result.add(LogListItem.DateHeader(day))
                lastDay = day
            }
            result.add(LogListItem.Entry(log))
        }
        return result
    }

    private fun getDayLabel(timestamp: Long): String {
        val cal = Calendar.getInstance()
        val now = cal.clone() as Calendar

        cal.timeInMillis = timestamp
        return when {
            isSameDay(cal, now) -> "Today"
            isYesterday(cal, now) -> "Yesterday"
            else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(a: Calendar, b: Calendar): Boolean {
        val yesterday = b.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(a, yesterday)
    }

    fun clearAllLogs() {
        // Never deletes isPermanent = 1 entries — saved logs are forever.
        viewModelScope.launch {
            app.notificationRepository.clearNonPermanentBefore(Long.MAX_VALUE)
        }
    }

    fun clearOldLogs(days: Int) {
        viewModelScope.launch { app.notificationRepository.clearLogsOlderThan(days) }
    }
}
