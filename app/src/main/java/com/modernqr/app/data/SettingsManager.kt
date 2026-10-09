package com.modernqr.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val THEME_MODE = intPreferencesKey("theme_mode") // 0 = Light, 1 = Dark, 2 = System
        val AUTO_SCAN = booleanPreferencesKey("auto_scan")
    }

    val themeModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: 2
    }

    val autoScanFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SCAN] ?: false
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setAutoScan(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SCAN] = enabled
        }
    }
}
