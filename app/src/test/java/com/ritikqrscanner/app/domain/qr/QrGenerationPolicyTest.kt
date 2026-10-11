package com.ritikqrscanner.app.domain.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrGenerationPolicyTest {
    @Test
    fun `black on white is reliable`() {
        val result = QrGenerationPolicy.assessColors(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

        assertTrue(result.isReliable)
        assertTrue(result.contrastRatio >= QrGenerationPolicy.MIN_CONTRAST_RATIO)
        assertNull(result.message)
    }

    @Test
    fun `low contrast and inverted colors are rejected`() {
        val lowContrast = QrGenerationPolicy.assessColors(0xFF777777.toInt(), 0xFF888888.toInt())
        val inverted = QrGenerationPolicy.assessColors(0xFFFFFFFF.toInt(), 0xFF000000.toInt())

        assertFalse(lowContrast.isReliable)
        assertFalse(inverted.isReliable)
    }

    @Test
    fun `payload limit counts UTF-8 bytes`() {
        assertNull(QrGenerationPolicy.payloadError("a".repeat(QrGenerationPolicy.MAX_PAYLOAD_BYTES)))
        assertTrue(QrGenerationPolicy.payloadError("🙂".repeat(601)) != null)
    }

    @Test
    fun `error correction reduces as payload grows`() {
        assertEquals(QrErrorCorrection.QUARTILE, QrGenerationPolicy.errorCorrectionFor("a".repeat(400)))
        assertEquals(QrErrorCorrection.MEDIUM, QrGenerationPolicy.errorCorrectionFor("a".repeat(401)))
        assertEquals(QrErrorCorrection.LOW, QrGenerationPolicy.errorCorrectionFor("a".repeat(1_201)))
    }
}
