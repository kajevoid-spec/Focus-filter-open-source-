// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db.dao

import androidx.room.*
import com.focusfilter.data.db.entities.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY type, label")
    fun getAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE type = :type AND isEnabled = 1")
    fun getRulesByType(type: String): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<Rule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
