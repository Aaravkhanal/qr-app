package com.ritikqrscanner.app.ui

import android.Manifest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritikqrscanner.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratorUiTest {
    @get:Rule(order = 0)
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun validationAndFormStateSurviveActivityRecreation() {
        composeRule.onNodeWithText("Create").performClick()
        composeRule.onNodeWithText("Generate QR code").assertIsNotEnabled()
        composeRule.onNodeWithText("Complete the highlighted fields")
            .assertTextContains("Complete the highlighted fields")
        composeRule.onNodeWithText("Text").performClick()
        composeRule.onNodeWithText("Enter text").performTextInput("rotation-safe Unicode नमस्ते")
        composeRule.onNodeWithText("Generate QR code").assertIsEnabled()
        composeRule.onNodeWithText("Ready to generate").assertTextContains("Ready to generate")

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Enter text").assertTextContains("rotation-safe Unicode नमस्ते")
        composeRule.onNodeWithText("Generate QR code").assertIsEnabled()
    }
}
