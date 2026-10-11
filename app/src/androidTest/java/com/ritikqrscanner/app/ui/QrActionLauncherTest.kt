package com.ritikqrscanner.app.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritikqrscanner.app.R
import com.ritikqrscanner.app.domain.qr.QrAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrActionLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun unavailableIntentReturnsExplanationWithoutLaunching() {
        val dispatcher = RecordingDispatcher(canResolve = false)

        val message = launchQrAction(context, QrAction.OpenUrl("https://example.com"), dispatcher)

        assertEquals(context.getString(R.string.no_browser_app), message)
        assertFalse(dispatcher.launched)
    }

    @Test
    fun SMSIntentPreservesCompleteColonMessage() {
        val dispatcher = RecordingDispatcher(canResolve = true)
        val message = "Meet: lobby: 10:30"

        val result = launchQrAction(context, QrAction.SendSms("+12025550123", message), dispatcher)

        assertNull(result)
        assertTrue(dispatcher.launched)
        assertEquals("+12025550123", dispatcher.intent?.data?.schemeSpecificPart)
        assertEquals(message, dispatcher.intent?.getStringExtra("sms_body"))
    }

    @Test
    fun launchFailureBecomesAVisibleErrorMessage() {
        val dispatcher = RecordingDispatcher(canResolve = true, failLaunch = true)

        val result = launchQrAction(context, QrAction.Dial("12345"), dispatcher)

        assertEquals(context.getString(R.string.action_open_failed), result)
    }

    private class RecordingDispatcher(
        private val canResolve: Boolean,
        private val failLaunch: Boolean = false
    ) : QrIntentDispatcher {
        var launched = false
        var intent: Intent? = null

        override fun canResolve(intent: Intent): Boolean = canResolve

        override fun launch(intent: Intent) {
            if (failLaunch) error("mock launch failure")
            launched = true
            this.intent = intent
        }
    }
}
