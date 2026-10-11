package com.ritikqrscanner.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ritikqrscanner.app.ui.screens.CameraPermissionContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deniedPermissionOffersRetryAndSettings() {
        composeRule.setContent {
            MaterialTheme {
                CameraPermissionContent(false, {}, {})
            }
        }

        composeRule.onNodeWithText("Camera access is needed to scan QR codes and barcodes.").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").assertIsDisplayed()
    }

    @Test
    fun permanentlyDeniedPermissionExplainsRecovery() {
        composeRule.setContent {
            MaterialTheme {
                CameraPermissionContent(true, {}, {})
            }
        }

        composeRule.onNodeWithText(
            "Camera access was permanently denied. Enable it in system settings to scan with the camera."
        ).assertIsDisplayed()
    }

    @Test
    fun permissionActionsRemainAvailableOnNarrowLargeTextLayout() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                MaterialTheme {
                    Box(Modifier.width(280.dp)) {
                        CameraPermissionContent(false, {}, {})
                    }
                }
            }
        }

        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").assertIsDisplayed()
    }
}
