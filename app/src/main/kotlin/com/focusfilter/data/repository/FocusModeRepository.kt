// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.repository

import com.focusfilter.data.db.dao.FocusModeDao
import com.focusfilter.data.db.entities.FocusMode
import kotlinx.coroutines.flow.Flow

class FocusModeRepository(private val dao: FocusModeDao) {

    fun getAllModes(): Flow<List<FocusMode>> = dao.getAllModes()

    fun getActiveMode(): Flow<FocusMode?> = dao.getActiveMode()

    suspend fun activateMode(type: String) {
        dao.deactivateAll()
        dao.activateMode(type)
    }

    suspend fun deactivateAll() = dao.deactivateAll()

    suspend fun getModeByType(type: String): FocusMode? = dao.getModeByType(type)

    suspend fun updateMode(mode: FocusMode) = dao.update(mode)

    suspend fun insertOrReplace(mode: FocusMode) = dao.insertOrReplace(mode)

    suspend fun deleteMode(type: String) = dao.deleteByType(type)
}
