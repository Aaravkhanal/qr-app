package com.ritikqrscanner.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule

fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.dismissOnboardingIfShown() {
    waitForIdle()
    onAllNodesWithText("Skip").fetchSemanticsNodes().firstOrNull()?.let {
        onAllNodesWithText("Skip")[0].performClick()
        waitForIdle()
    }
}
