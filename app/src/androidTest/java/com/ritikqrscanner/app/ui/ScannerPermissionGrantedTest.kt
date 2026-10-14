package com.ritikqrscanner.app.ui

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritikqrscanner.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerPermissionGrantedTest {
    @get:Rule(order = 0)
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun grantedPermissionShowsScannerControls() {
        composeRule.dismissOnboardingIfShown()
        composeRule.onNodeWithText("Gallery").assertIsDisplayed()
        composeRule.onNodeWithText("On-device & private").assertIsDisplayed()
        composeRule.onNodeWithText("Align a QR code inside the frame").assertIsDisplayed()
    }
}
