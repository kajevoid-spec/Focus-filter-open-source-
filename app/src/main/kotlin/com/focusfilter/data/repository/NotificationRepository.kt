// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.repository

import com.focusfilter.data.db.AppUsageStat
import com.focusfilter.data.db.dao.FilteredNotificationDao
import com.focusfilter.data.db.entities.FilteredNotification
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: FilteredNotificationDao) {

    fun getActiveFiltered(): Flow<List<FilteredNotification>> = dao.getActiveFiltered()

    fun getRecentLogs(limit: Int = 200): Flow<List<FilteredNotification>> = dao.getRecentLogs(limit)

    fun searchLogs(query: String): Flow<List<FilteredNotification>> = dao.searchLogs(query)

    fun getLogsByActions(actions: List<String>): Flow<List<FilteredNotification>> = dao.getLogsByActions(actions)

    fun getLogsByLabel(label: String): Flow<List<FilteredNotification>> = dao.getLogsByLabel(label)

    fun getOverrideLogs(): Flow<List<FilteredNotification>> = dao.getOverrideLogs()

    fun countFilteredToday(since: Long): Flow<Int> = dao.countFilteredSince(since)

    fun countAllowedToday(since: Long): Flow<Int> = dao.countAllowedSince(since)

    fun countBlockedSince(since: Long): Flow<Int> = dao.countBlockedSince(since)

    fun getLatestEntry(since: Long): Flow<FilteredNotification?> = dao.getLatestEntry(since)

    fun getTopBlockedApps(since: Long, limit: Int = 5): Flow<List<AppUsageStat>> =
        dao.getTopBlockedApps(since, limit)

    suspend fun insert(notification: FilteredNotification) = dao.insert(notification)

    suspend fun markRestored(id: Long) = dao.markRestored(id)

    suspend fun allowOnce(id: Long) = dao.allowOnce(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun clearLogsOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        dao.clearNonPermanentBefore(cutoff)
    }

    // ── Fix 5-7: Permanent save, auto-clear, inbox query ─────────────────────────

    /** Mark an item as permanently saved — it will never be auto-cleared. */
    suspend fun markPermanent(id: Long) = dao.markPermanent(id)

    /** Delete non-permanent entries older than [before] (Unix ms). */
    suspend fun clearNonPermanentBefore(before: Long) = dao.clearNonPermanentBefore(before)

    /** Stream of permanently saved items, newest first. */
    fun getPermanentlySaved(): Flow<List<FilteredNotification>> = dao.getPermanentlySaved()

    /** Stream of inbox items: non-restored, non-permanent, newest first. */
    fun getInboxItems(): Flow<List<FilteredNotification>> = dao.getInboxItems()

    /** Clear inbox without touching permanently saved logs. */
    suspend fun clearInboxOnly() = dao.clearInboxOnly()

    suspend fun updateAction(id: Long, action: String) {
        dao.updateAction(id, action)
    }
}
