package com.example.resistiq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.resistiq.data.PreferencesManager
import com.example.resistiq.model.ResistorHistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    val historyItems: StateFlow<List<ResistorHistoryItem>> = preferencesManager.historyItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            preferencesManager.deleteHistoryItem(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            preferencesManager.clearHistory()
        }
    }
}
