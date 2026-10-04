package com.example.resistiq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.resistiq.model.AppSettings
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.CalculatorTab
import com.example.resistiq.model.ElectricalParameter
import com.example.resistiq.model.ResistorHistoryItem
import com.example.resistiq.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "resistiq_prefs")

/** Pure mapping between [AppSettings] and Preferences, kept separate so it can be unit-tested. */
object SettingsMapper {

    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val DEFAULT_TAB = stringPreferencesKey("default_tab")
    val DEFAULT_BAND_COUNT = stringPreferencesKey("default_band_count")
    val DEFAULT_OHMS_FIRST = stringPreferencesKey("default_ohms_first")
    val DEFAULT_OHMS_SECOND = stringPreferencesKey("default_ohms_second")
    val DECIMAL_PLACES = intPreferencesKey("decimal_places")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val SHOW_FORMULA = booleanPreferencesKey("show_formula")
    val SHOW_BAND_BREAKDOWN = booleanPreferencesKey("show_band_breakdown")
    val SHOW_E_SERIES = booleanPreferencesKey("show_e_series")

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    fun read(preferences: Preferences): AppSettings {
        val defaults = AppSettings()
        val first = enumOrDefault(preferences[DEFAULT_OHMS_FIRST], defaults.defaultOhmsFirst)
        var second = enumOrDefault(preferences[DEFAULT_OHMS_SECOND], defaults.defaultOhmsSecond)
        if (second == first) {
            second = ElectricalParameter.entries.first { it != first }
        }
        return AppSettings(
            themeMode = enumOrDefault(preferences[THEME_MODE], defaults.themeMode),
            dynamicColor = preferences[DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultTab = enumOrDefault(preferences[DEFAULT_TAB], defaults.defaultTab),
            defaultBandCount = enumOrDefault(preferences[DEFAULT_BAND_COUNT], defaults.defaultBandCount),
            defaultOhmsFirst = first,
            defaultOhmsSecond = second,
            decimalPlaces = (preferences[DECIMAL_PLACES] ?: defaults.decimalPlaces).coerceIn(1, 3),
            hapticFeedback = preferences[HAPTIC_FEEDBACK] ?: defaults.hapticFeedback,
            showFormula = preferences[SHOW_FORMULA] ?: defaults.showFormula,
            showBandBreakdown = preferences[SHOW_BAND_BREAKDOWN] ?: defaults.showBandBreakdown,
            showESeries = preferences[SHOW_E_SERIES] ?: defaults.showESeries
        )
    }

    fun write(preferences: MutablePreferences, settings: AppSettings) {
        preferences[THEME_MODE] = settings.themeMode.name
        preferences[DYNAMIC_COLOR] = settings.dynamicColor
        preferences[DEFAULT_TAB] = settings.defaultTab.name
        preferences[DEFAULT_BAND_COUNT] = settings.defaultBandCount.name
        preferences[DEFAULT_OHMS_FIRST] = settings.defaultOhmsFirst.name
        preferences[DEFAULT_OHMS_SECOND] = settings.defaultOhmsSecond.name
        preferences[DECIMAL_PLACES] = settings.decimalPlaces
        preferences[HAPTIC_FEEDBACK] = settings.hapticFeedback
        preferences[SHOW_FORMULA] = settings.showFormula
        preferences[SHOW_BAND_BREAKDOWN] = settings.showBandBreakdown
        preferences[SHOW_E_SERIES] = settings.showESeries
    }
}

class PreferencesManager(private val context: Context) {

    private val historyKey = stringPreferencesKey("history_data")

    private val safeData: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val appSettingsFlow: Flow<AppSettings> = safeData.map { SettingsMapper.read(it) }

    /** Atomic read-modify-write so a change never clobbers values not yet loaded in the UI. */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            SettingsMapper.write(preferences, transform(SettingsMapper.read(preferences)))
        }
    }

    suspend fun resetSettings() {
        updateSettings { AppSettings() }
    }

    val historyItemsFlow: Flow<List<ResistorHistoryItem>> = safeData.map { preferences ->
        HistorySerializer.deserializeList(preferences[historyKey].orEmpty())
    }

    suspend fun saveHistoryItem(item: ResistorHistoryItem) {
        context.dataStore.edit { preferences ->
            val existing = HistorySerializer.deserializeList(preferences[historyKey].orEmpty())
            preferences[historyKey] = HistorySerializer.serializeList(HistorySerializer.prepend(existing, item))
        }
    }

    suspend fun deleteHistoryItem(id: Long) {
        context.dataStore.edit { preferences ->
            val remaining = HistorySerializer.deserializeList(preferences[historyKey].orEmpty())
                .filter { it.id != id }
            preferences[historyKey] = HistorySerializer.serializeList(remaining)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(historyKey)
        }
    }
}
