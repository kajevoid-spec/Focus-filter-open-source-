// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.focusfilter.FocusFilterApplication
import com.focusfilter.data.db.entities.FocusMode
import com.focusfilter.model.FocusModeType
import kotlinx.coroutines.launch
import java.util.UUID

class ModesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusFilterApplication

    val allModes: LiveData<List<FocusMode>> = app.focusModeRepository.getAllModes().asLiveData()
    val activeMode: LiveData<FocusMode?> = app.focusModeRepository.getActiveMode().asLiveData()

    private val _deleteError = MutableLiveData<String?>()
    val deleteError: LiveData<String?> = _deleteError

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

    /**
     * Creates a new custom mode.
     *
     * [allowedKeywords] and [blockedKeywords] are accepted as comma-separated strings
     * (matching the UI text input) and converted to typed lists here so that storage
     * always uses the JSON-backed [FocusMode.allowedKeywords] / [FocusMode.blockedKeywords] fields.
     */
    fun createMode(
        name: String,
        description: String,
        defaultAction: String,
        silenceCalls: Boolean,
        allowedKeywords: String,
        blockedKeywords: String
    ) {
        val type = "CUSTOM_${UUID.randomUUID().toString().take(8).uppercase()}"
        val mode = FocusMode(
            type            = type,
            displayName     = name,
            description     = description,
            iconName        = "ic_settings",
            silenceCalls    = silenceCalls,
            allowedKeywords = allowedKeywords.parseCsvKeywords(),
            blockedKeywords = blockedKeywords.parseCsvKeywords(),
            defaultAction   = defaultAction,
            isCustom        = true,
            isBuiltIn       = false
        )
        viewModelScope.launch { app.focusModeRepository.insertOrReplace(mode) }
    }

    fun updateMode(mode: FocusMode) {
        viewModelScope.launch { app.focusModeRepository.updateMode(mode) }
    }

    fun deleteMode(type: String) {
        viewModelScope.launch {
            val mode = app.focusModeRepository.getModeByType(type)
            if (mode?.isBuiltIn == true) {
                _deleteError.postValue("Built-in modes cannot be deleted.")
                return@launch
            }
            if (app.preferencesManager.activeModeType == type) {
                app.focusModeRepository.deactivateAll()
                app.preferencesManager.isFocusEnabled = false
                app.preferencesManager.activeModeType = FocusModeType.NONE.name
            }
            app.focusModeRepository.deleteMode(type)
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun deactivateAll() {
        viewModelScope.launch {
            app.focusModeRepository.deactivateAll()
            app.preferencesManager.isFocusEnabled = false
            app.preferencesManager.activeModeType = FocusModeType.NONE.name
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun String.parseCsvKeywords(): List<String> =
        split(",").map { it.trim() }.filter { it.isNotBlank() }
}
