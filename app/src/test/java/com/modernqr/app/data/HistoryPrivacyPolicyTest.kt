package com.modernqr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPrivacyPolicyTest {
    @Test
    fun `sensitive payload categories are never retained`() {
        val sensitiveValues = listOf(
            "WIFI:T:WPA;S:Home;P:top-secret;;",
            "BEGIN:VCARD\nVERSION:3.0\nFN:Sam\nTEL:+15551234\nEND:VCARD",
            "mailto:person@example.com?subject=Private",
            "tel:+15551234",
            "smsto:+15551234:private message"
        )

        sensitiveValues.forEach { value ->
            assertFalse(value, HistoryPrivacyPolicy.canStore(value, "QR_CODE"))
        }
    }

    @Test
    fun `ordinary text urls and unknown barcodes can be retained`() {
        assertTrue(HistoryPrivacyPolicy.canStore("hello", "QR_CODE"))
        assertTrue(HistoryPrivacyPolicy.canStore("https://example.com", "QR_CODE"))
        assertTrue(HistoryPrivacyPolicy.canStore("9781234567890", "EAN_13"))
    }

    @Test
    fun `literal search escapes sqlite wildcard characters`() {
        assertEquals("100\\%\\_done\\\\today", HistoryPrivacyPolicy.escapeLikeQuery("100%_done\\today"))
    }
}
