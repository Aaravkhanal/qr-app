package com.ritikqrscanner.app.ui

import android.Manifest
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritikqrscanner.app.MainActivity
import com.ritikqrscanner.app.data.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule(order = 0)
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun themeSelectionUpdatesUiAndPersists() {
        composeRule.dismissOnboardingIfShown()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Dark").performClick()

        val settings = SettingsManager(composeRule.activity)
        assertEquals(1, runBlocking { settings.themeModeFlow.filter { it == 1 }.first() })
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dark").assertIsOn()

        composeRule.onNodeWithText("System default").performClick()
        assertEquals(2, runBlocking { settings.themeModeFlow.filter { it == 2 }.first() })
        composeRule.waitForIdle()
        composeRule.onNodeWithText("System default").assertIsOn()
    }
}
