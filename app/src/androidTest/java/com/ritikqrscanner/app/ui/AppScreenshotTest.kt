package com.ritikqrscanner.app.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ritikqrscanner.app.MainActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppScreenshotTest {
    @get:Rule(order = 0)
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureCurrentLightAndDarkExperience() {
        dismissWelcomeIfVisible()
        openSettings()
        composeRule.onNodeWithText("Light").performClick()
        composeRule.onNodeWithText("Show welcome tour again").performScrollTo().performClick()
        waitForText("Scan in a moment")
        capture("01-welcome-light.png")

        composeRule.onNodeWithText("Skip").performClick()
        capture("02-scanner-light.png")

        composeRule.onNodeWithText("Create").performClick()
        composeRule.onNodeWithText("Text").performClick()
        composeRule.onNodeWithText("Enter text").performTextInput("Private, fast, and made on-device")
        composeRule.onNodeWithText("Generate QR code").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Generated QR code").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Generated QR code").performScrollTo()
        capture("03-create-light.png")

        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Private, fast, and made on-device").fetchSemanticsNodes().isNotEmpty()
        }
        capture("04-history-light.png")

        openSettings()
        capture("05-settings-light.png")
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.waitForIdle()
        capture("06-settings-dark.png")

        composeRule.onNodeWithText("Scan").performClick()
        capture("07-scanner-dark.png")
        composeRule.onNodeWithText("Create").performClick()
        capture("08-create-dark.png")
        composeRule.onNodeWithText("History").performClick()
        capture("09-history-dark.png")
    }

    private fun dismissWelcomeIfVisible() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Skip").performClick()
        }
    }

    private fun openSettings() {
        composeRule.onNodeWithText("Settings").performClick()
        waitForText("Appearance")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        Thread.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(
            instrumentation.targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "app-screenshots"
        )
        assertTrue(directory.exists() || directory.mkdirs())
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(directory, name).outputStream().use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        screenshot.recycle()
    }
}
