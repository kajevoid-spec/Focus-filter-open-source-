// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.focusfilter.FocusFilterApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrustedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val app   = application as FocusFilterApplication
    private val prefs = app.preferencesManager

    data class InstalledAppInfo(
        val packageName: String,
        val appName: String,
        val isTrusted: Boolean
    )

    private val _apps = MutableLiveData<List<InstalledAppInfo>>()
    val apps: LiveData<List<InstalledAppInfo>> = _apps

    // Backing list — always the complete set, never filtered
    @Volatile private var allApps: List<InstalledAppInfo> = emptyList()

    private var searchQuery = ""

    init { loadApps() }

    fun loadApps() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val pm      = getApplication<Application>().packageManager
                val trusted = prefs.trustedApps

                // Use launcher-activity membership instead of FLAG_SYSTEM so that
                // user-facing apps like Gmail, Google, Calendar are always included.
                val launcherPackages = pm
                    .queryIntentActivities(
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
                    )
                    .map { it.activityInfo.packageName }
                    .toSet()

                pm.getInstalledApplications(0)
                    .filter { info ->
                        info.packageName != app.packageName &&
                        (info.packageName in launcherPackages || info.packageName in trusted)
                    }
                    .map { info ->
                        InstalledAppInfo(
                            packageName = info.packageName,
                            appName     = pm.getApplicationLabel(info).toString(),
                            isTrusted   = info.packageName in trusted
                        )
                    }
                    .sortedWith(
                        compareByDescending<InstalledAppInfo> { it.isTrusted }
                            .thenBy { it.appName.lowercase() }
                    )
            }
            allApps = result
            // Apply any query already typed before load finished
            _apps.value = applyQuery(result, searchQuery)
        }
    }

    fun search(query: String) {
        searchQuery = query.trim()
        _apps.value = applyQuery(allApps, searchQuery)
    }

    fun toggleTrusted(packageName: String, trusted: Boolean) {
        if (trusted) prefs.addTrustedApp(packageName) else prefs.removeTrustedApp(packageName)
        allApps = allApps.map { info ->
            if (info.packageName == packageName) info.copy(isTrusted = trusted) else info
        }
        _apps.value = applyQuery(allApps, searchQuery)
    }

    fun trustedCount(): Int = prefs.trustedApps.size

    private fun applyQuery(list: List<InstalledAppInfo>, query: String): List<InstalledAppInfo> {
        if (query.isBlank()) return list
        val q = query.lowercase()
        return list.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }
}
