package com.ritikqrscanner.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val THEME_MODE = intPreferencesKey("theme_mode") // 0 = Light, 1 = Dark, 2 = System
        val SCAN_SOUND = booleanPreferencesKey("scan_sound")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }

    val themeModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: 2
    }

    val scanSoundFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SCAN_SOUND] ?: true
    }

    val hapticFeedbackFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK] ?: true
    }

    val saveHistoryFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SAVE_HISTORY] ?: true
    }

    val onboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETE] ?: false
    }

    val reduceMotionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REDUCE_MOTION] ?: false
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setScanSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_SOUND] = enabled
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setSaveHistory(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SAVE_HISTORY] = enabled
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { preferences -> preferences[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[REDUCE_MOTION] = enabled }
    }
}
