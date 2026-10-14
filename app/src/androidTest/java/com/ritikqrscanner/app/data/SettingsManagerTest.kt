package com.ritikqrscanner.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsManagerTest {
    @Test
    fun settingsPersistAcrossManagerInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = SettingsManager(context)
        first.setThemeMode(1)
        first.setScanSound(false)
        first.setHapticFeedback(false)
        first.setSaveHistory(false)
        first.setReduceMotion(true)
        first.setOnboardingComplete(true)

        val restored = SettingsManager(context)
        assertEquals(1, restored.themeModeFlow.first())
        assertFalse(restored.scanSoundFlow.first())
        assertFalse(restored.hapticFeedbackFlow.first())
        assertFalse(restored.saveHistoryFlow.first())
        assertTrue(restored.reduceMotionFlow.first())
        assertTrue(restored.onboardingCompleteFlow.first())

        restored.setThemeMode(2)
        restored.setScanSound(true)
        restored.setHapticFeedback(true)
        restored.setSaveHistory(true)
        restored.setReduceMotion(false)
        restored.setOnboardingComplete(false)
        assertTrue(first.saveHistoryFlow.first())
        assertFalse(first.reduceMotionFlow.first())
        assertFalse(first.onboardingCompleteFlow.first())
    }
}
