// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.focusfilter.FocusFilterApplication
import com.focusfilter.data.db.entities.Rule
import kotlinx.coroutines.launch

class RulesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusFilterApplication

    val allRules: LiveData<List<Rule>> = app.ruleRepository.getAllRules().asLiveData()

    fun addRule(rule: Rule) {
        viewModelScope.launch {
            app.ruleRepository.insert(rule)
            app.invalidateRulesCache()
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            app.ruleRepository.deleteById(id)
            app.invalidateRulesCache()
        }
    }

    fun toggleRule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            app.ruleRepository.setEnabled(id, enabled)
            app.invalidateRulesCache()
        }
    }
}
