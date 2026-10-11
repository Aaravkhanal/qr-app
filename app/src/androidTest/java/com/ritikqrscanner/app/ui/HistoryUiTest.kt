package com.ritikqrscanner.app.ui

import android.Manifest
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritikqrscanner.app.MainActivity
import com.ritikqrscanner.app.data.AppDatabase
import com.ritikqrscanner.app.data.HistoryItem
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryUiTest {
    @get:Rule(order = 0)
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun individualDeletionAndClearHistoryRequireConfirmation() {
        val dao = AppDatabase.getDatabase(composeRule.activity).historyDao()
        runBlocking {
            dao.clearHistory()
            dao.insert(history("older", 10))
            dao.insert(history("newer", 20))
        }

        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntil(2_000) {
            composeRule.onAllNodesWithContentDescription("Delete").fetchSemanticsNodes().size == 2
        }

        composeRule.onAllNodesWithContentDescription("Delete")[0].performClick()
        composeRule.onNodeWithText("Delete history item?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitUntil(2_000) {
            composeRule.onNodeWithText("newer").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("newer").assertDoesNotExist()
        composeRule.onNodeWithText("older").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Clear history").performClick()
        composeRule.onNodeWithText("Clear history?").assertIsDisplayed()
        composeRule.onNodeWithText("Clear").performClick()
        composeRule.waitUntil(2_000) {
            composeRule.onNodeWithText("No history yet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("No history yet").assertIsDisplayed()
    }

    private fun history(content: String, timestamp: Long) = HistoryItem(
        content = content,
        format = "QR_CODE",
        type = "Scanned",
        timestamp = timestamp
    )
}
