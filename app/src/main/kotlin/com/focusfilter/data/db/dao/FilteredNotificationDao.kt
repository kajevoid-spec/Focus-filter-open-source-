// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.dao

import androidx.room.*
import com.focusfilter.data.db.AppUsageStat
import com.focusfilter.data.db.entities.FilteredNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface FilteredNotificationDao {

    @Query("SELECT * FROM filtered_notifications WHERE isRestored = 0 ORDER BY timestamp DESC")
    fun getActiveFiltered(): Flow<List<FilteredNotification>>

    @Query("SELECT * FROM filtered_notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 200): Flow<List<FilteredNotification>>

    @Query("""
        SELECT * FROM filtered_notifications
        WHERE (appName LIKE '%' || :query || '%' OR classifierLabel LIKE '%' || :query || '%')
        ORDER BY timestamp DESC LIMIT 200
    """)
    fun searchLogs(query: String): Flow<List<FilteredNotification>>

    @Query("""
        SELECT * FROM filtered_notifications
        WHERE action IN (:actions)
        ORDER BY timestamp DESC LIMIT 200
    """)
    fun getLogsByActions(actions: List<String>): Flow<List<FilteredNotification>>

    @Query("""
        SELECT * FROM filtered_notifications
        WHERE classifierLabel = :label
        ORDER BY timestamp DESC LIMIT 200
    """)
    fun getLogsByLabel(label: String): Flow<List<FilteredNotification>>

    @Query("""
        SELECT * FROM filtered_notifications
        WHERE isOverride = 1
        ORDER BY timestamp DESC LIMIT 200
    """)
    fun getOverrideLogs(): Flow<List<FilteredNotification>>

    @Query("SELECT COUNT(*) FROM filtered_notifications WHERE action IN ('BLOCK','HOLD','SILENT') AND timestamp > :since")
    fun countFilteredSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM filtered_notifications WHERE action = 'ALLOW' AND timestamp > :since")
    fun countAllowedSince(since: Long): Flow<Int>

    @Query("SELECT * FROM filtered_notifications WHERE timestamp > :since ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEntry(since: Long): Flow<FilteredNotification?>

    @Query("""
        SELECT packageName, appName, COUNT(*) as count
        FROM filtered_notifications
        WHERE action IN ('BLOCK','HOLD','SILENT') AND timestamp > :since
        GROUP BY packageName
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getTopBlockedApps(since: Long, limit: Int = 5): Flow<List<AppUsageStat>>

    @Query("SELECT COUNT(*) FROM filtered_notifications WHERE action IN ('BLOCK','HOLD','SILENT') AND timestamp > :since")
    fun countBlockedSince(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: FilteredNotification)

    @Update
    suspend fun update(notification: FilteredNotification)

    @Query("UPDATE filtered_notifications SET isRestored = 1 WHERE id = :id")
    suspend fun markRestored(id: Long)

    @Query("UPDATE filtered_notifications SET isOverride = 1, isRestored = 1, action = 'OVERRIDE' WHERE id = :id")
    suspend fun allowOnce(id: Long)

    @Query("DELETE FROM filtered_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM filtered_notifications WHERE isRestored = 1")
    suspend fun clearRestored()

    @Query("DELETE FROM filtered_notifications WHERE timestamp < :before")
    suspend fun clearBefore(before: Long)

    @Query("DELETE FROM filtered_notifications")
    suspend fun clearAll()

    /** Clear inbox without touching permanently saved logs. */
    @Query("DELETE FROM filtered_notifications WHERE isPermanent = 0 AND isRestored = 0")
    suspend fun clearInboxOnly()

    // ── Fix 5: Permanent save & auto-clear ───────────────────────────────────────

    /** Mark a notification as permanently saved — it will never be auto-cleared. */
    @Query("UPDATE filtered_notifications SET isPermanent = 1 WHERE id = :id")
    suspend fun markPermanent(id: Long)

    /** Delete non-permanent entries older than [before] (Unix ms) — called on startup. */
    @Query("DELETE FROM filtered_notifications WHERE isPermanent = 0 AND timestamp < :before")
    suspend fun clearNonPermanentBefore(before: Long)

    /** All permanently saved items, newest first. */
    @Query("SELECT * FROM filtered_notifications WHERE isPermanent = 1 ORDER BY timestamp DESC")
    fun getPermanentlySaved(): Flow<List<FilteredNotification>>

    /** Inbox items: non-restored, non-permanent entries only. */
    @Query("""
        SELECT * FROM filtered_notifications
        WHERE isRestored = 0 AND isPermanent = 0
        ORDER BY timestamp DESC
    """)
    fun getInboxItems(): Flow<List<FilteredNotification>>

    @Query("UPDATE filtered_notifications SET action = :action WHERE id = :id")
    suspend fun updateAction(id: Long, action: String)
}
