package com.example.resistiq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.resistiq.data.PreferencesManager
import com.example.resistiq.model.AppSettings
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.CalculatorTab
import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    val settings: StateFlow<AppSettings> = preferencesManager.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(themeMode = mode) }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(dynamicColor = enabled) }
        }
    }

    fun setDefaultTab(tab: CalculatorTab) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(defaultTab = tab) }
        }
    }

    fun setDefaultBandCount(bandCount: BandCount) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(defaultBandCount = bandCount) }
        }
    }

    fun setDefaultOhmsValues(first: ElectricalParameter, second: ElectricalParameter) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(defaultOhmsFirst = first, defaultOhmsSecond = second) }
        }
    }

    fun setDecimalPlaces(places: Int) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(decimalPlaces = places) }
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(hapticFeedback = enabled) }
        }
    }

    fun setShowFormula(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(showFormula = enabled) }
        }
    }

    fun setShowBandBreakdown(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(showBandBreakdown = enabled) }
        }
    }

    fun setShowESeries(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(showESeries = enabled) }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            preferencesManager.resetSettings()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            preferencesManager.clearHistory()
        }
    }
}
