package com.ritikqrscanner.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tourExplainsCoreValueAndCompletes() {
        var completed = false
        composeRule.setContent {
            MaterialTheme { OnboardingDialog(onComplete = { completed = true }) }
        }

        composeRule.onNodeWithText("Scan in a moment").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Create with confidence").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Private by design").assertIsDisplayed()
        composeRule.onNodeWithText("Start scanning").performClick()

        composeRule.runOnIdle { assertTrue(completed) }
    }
}
