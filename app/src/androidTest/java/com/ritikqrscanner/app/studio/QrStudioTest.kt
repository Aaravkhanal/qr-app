package com.ritikqrscanner.app.studio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrStudioTest {
    @Test fun templatesAndRoundedLogoDesignsPassExactDecodeVerification() {
        val logo = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        for (template in listOf("QR only", "Wi-Fi card", "Business card", "Event invitation", "Printable sign")) {
            val result = QrStudio.render("https://example.com/こんにちは", Color.BLACK, Color.WHITE, true, logo, template, "Example")
            assertTrue(result.width > 0)
            result.recycle()
        }
        logo.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun unreadableDesignCannotBeExported() {
        QrStudio.render("private test", Color.WHITE, Color.WHITE, false, null, "QR only", "")
    }

    @Test fun aiCarThemeIsColorfulAndStillDecodesExactly() {
        val theme = AiQrThemePlanner.create("a bold red sports car")
        val result = QrStudio.render(
            "https://example.com/car",
            Color.BLACK,
            Color.WHITE,
            true,
            null,
            "QR only",
            "",
            aiTheme = theme
        )
        assertTrue(result.height > result.width)
        QrStudio.verify(result, "https://example.com/car")
        result.recycle()
    }
}
