// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.keyword

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KeywordSafelistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(keyword: KeywordSafelist)

    @Delete
    suspend fun delete(keyword: KeywordSafelist)

    @Query("SELECT * FROM keyword_safelist ORDER BY addedAt DESC")
    fun getAll(): LiveData<List<KeywordSafelist>>

    @Query("SELECT * FROM keyword_safelist")
    suspend fun getAllSync(): List<KeywordSafelist>

    @Query("SELECT * FROM keyword_safelist WHERE isEnabled = 1")
    suspend fun getEnabledSync(): List<KeywordSafelist>

    @Query("UPDATE keyword_safelist SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM keyword_safelist WHERE id = :id")
    suspend fun deleteById(id: Long)
}
