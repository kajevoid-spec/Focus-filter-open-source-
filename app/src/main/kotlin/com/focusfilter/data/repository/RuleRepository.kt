// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.repository

import com.focusfilter.data.db.dao.RuleDao
import com.focusfilter.data.db.entities.Rule
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val dao: RuleDao) {

    fun getAllRules(): Flow<List<Rule>> = dao.getAllRules()

    fun getRulesByType(type: String): Flow<List<Rule>> = dao.getRulesByType(type)

    suspend fun getEnabledRules(): List<Rule> = dao.getEnabledRules()

    suspend fun insert(rule: Rule) = dao.insert(rule)

    suspend fun update(rule: Rule) = dao.update(rule)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
