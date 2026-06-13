// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.focusfilter.FocusFilterApplication
import com.focusfilter.data.db.AppUsageStat
import com.focusfilter.data.db.entities.FilteredNotification
import com.focusfilter.data.db.entities.FocusMode
import com.focusfilter.model.FocusModeType
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusFilterApplication

    val activeMode: LiveData<FocusMode?> = app.focusModeRepository.getActiveMode().asLiveData()
    val isFocusEnabled: Boolean get() = app.preferencesManager.isFocusEnabled

    val filteredTodayCount: LiveData<Int> =
        app.notificationRepository.countFilteredToday(todayStart()).asLiveData()

    val allowedTodayCount: LiveData<Int> =
        app.notificationRepository.countAllowedToday(todayStart()).asLiveData()

    val latestEntry: LiveData<FilteredNotification?> =
        app.notificationRepository.getLatestEntry(todayStart()).asLiveData()

    val topBlockedApps: LiveData<List<AppUsageStat>> =
        app.notificationRepository.getTopBlockedApps(todayStart(), 3).asLiveData()

    init {
        checkAndResetDailyStats()
    }

    fun activateMode(type: String) {
        viewModelScope.launch {
            app.focusModeRepository.activateMode(type)
            app.preferencesManager.activeModeType = type
            app.preferencesManager.isFocusEnabled = true

            if (app.preferencesManager.focusStartTimestamp == 0L) {
                app.preferencesManager.focusStartTimestamp = System.currentTimeMillis()
            }
        }
    }

    fun activateMode(type: FocusModeType) = activateMode(type.name)

    fun deactivateFocus() {
        viewModelScope.launch {
            app.focusModeRepository.deactivateAll()
            app.preferencesManager.isFocusEnabled = false
            app.preferencesManager.activeModeType = FocusModeType.NONE.name

            val start = app.preferencesManager.focusStartTimestamp
            if (start > 0L) {
                val elapsed = (System.currentTimeMillis() - start) / 60000L
                app.preferencesManager.totalFocusMinutesToday += elapsed
                app.preferencesManager.focusStartTimestamp = 0L
            }
        }
    }

    fun getFocusTimeFormatted(): String {
        var minutes = app.preferencesManager.totalFocusMinutesToday
        val start = app.preferencesManager.focusStartTimestamp
        if (start > 0L && app.preferencesManager.isFocusEnabled) {
            minutes += (System.currentTimeMillis() - start) / 60000L
        }
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    fun getTimeSavedFormatted(blockedCount: Int): String {
        // 30 seconds per blocked notification is a rough estimate.
        // Label it clearly so users understand it is not a precise measurement.
        val minutes = (blockedCount * 30) / 60
        return if (minutes < 1) "~0m saved (est.)" else "~${minutes}m saved (est.)"
    }

    private fun checkAndResetDailyStats() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (app.preferencesManager.lastResetDay != today) {
            app.preferencesManager.totalFocusMinutesToday = 0L
            if (!app.preferencesManager.isFocusEnabled) {
                app.preferencesManager.focusStartTimestamp = 0L
            }
            app.preferencesManager.lastResetDay = today
        }
    }

    private fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
