package com.modernqr.app.domain.qr

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class QrErrorCorrection {
    LOW,
    MEDIUM,
    QUARTILE
}

data class QrColorAssessment(
    val isReliable: Boolean,
    val contrastRatio: Double,
    val message: String?
)

object QrGenerationPolicy {
    const val MAX_PAYLOAD_BYTES = 2_400
    const val MIN_CONTRAST_RATIO = 4.5

    fun payloadError(content: String): String? = when {
        content.isBlank() -> "Enter content before generating a QR code."
        content.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES ->
            "This content is too large for a reliable QR code. Shorten it and try again."
        else -> null
    }

    fun errorCorrectionFor(content: String): QrErrorCorrection = when {
        content.toByteArray(Charsets.UTF_8).size <= 400 -> QrErrorCorrection.QUARTILE
        content.toByteArray(Charsets.UTF_8).size <= 1_200 -> QrErrorCorrection.MEDIUM
        else -> QrErrorCorrection.LOW
    }

    fun assessColors(foregroundArgb: Int, backgroundArgb: Int): QrColorAssessment {
        val foregroundLuminance = relativeLuminance(foregroundArgb)
        val backgroundLuminance = relativeLuminance(backgroundArgb)
        val contrast = (max(foregroundLuminance, backgroundLuminance) + 0.05) /
            (min(foregroundLuminance, backgroundLuminance) + 0.05)

        val message = when {
            foregroundLuminance >= backgroundLuminance ->
                "Use a darker foreground than background for reliable scanning."
            contrast < MIN_CONTRAST_RATIO ->
                "These colors have low contrast. Choose a darker foreground or lighter background."
            else -> null
        }
        return QrColorAssessment(message == null, contrast, message)
    }

    private fun relativeLuminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
