// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.dao

import androidx.room.*
import com.focusfilter.data.db.entities.FocusMode
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusModeDao {

    @Query("SELECT * FROM focus_modes ORDER BY isCustom ASC, type ASC")
    fun getAllModes(): Flow<List<FocusMode>>

    @Query("SELECT * FROM focus_modes ORDER BY isCustom ASC, type ASC")
    suspend fun getAllModesList(): List<FocusMode>

    @Query("SELECT * FROM focus_modes WHERE isActive = 1 LIMIT 1")
    fun getActiveMode(): Flow<FocusMode?>

    @Query("SELECT * FROM focus_modes WHERE type = :type")
    suspend fun getModeByType(type: String): FocusMode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(mode: FocusMode)

    @Query("UPDATE focus_modes SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE focus_modes SET isActive = 1 WHERE type = :type")
    suspend fun activateMode(type: String)

    @Update
    suspend fun update(mode: FocusMode)

    @Query("DELETE FROM focus_modes WHERE type = :type")
    suspend fun deleteByType(type: String)
}
