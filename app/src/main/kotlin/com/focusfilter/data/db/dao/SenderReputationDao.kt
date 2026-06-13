// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.focusfilter.data.db.entities.SenderReputation

@Dao
interface SenderReputationDao {

    @Upsert
    suspend fun upsert(reputation: SenderReputation)

    @Query("SELECT * FROM sender_reputation WHERE packageName = :pkg")
    suspend fun get(pkg: String): SenderReputation?

    /** Inserts a blank row only if one doesn't already exist — never overwrites counts. */
    @Query("INSERT OR IGNORE INTO sender_reputation (packageName, allowCount, blockCount, lastSeen) VALUES (:pkg, 0, 0, 0)")
    suspend fun insertIfAbsent(pkg: String)

    @Query("UPDATE sender_reputation SET allowCount = allowCount + 1, lastSeen = :ts WHERE packageName = :pkg")
    suspend fun incrementAllow(pkg: String, ts: Long)

    @Query("UPDATE sender_reputation SET blockCount = blockCount + 1, lastSeen = :ts WHERE packageName = :pkg")
    suspend fun incrementBlock(pkg: String, ts: Long)
}
