package com.ritikqrscanner.app.studio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritikqrscanner.app.scanner.decodeGalleryBarcodes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GalleryMultipleTest {
    @Test fun sharedImageReturnsBothDistinctCodes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = QrStudio.render("https://example.com/first", Color.BLACK, Color.WHITE, false, null, "QR only", "")
        val second = QrStudio.render("https://example.com/second", Color.BLACK, Color.WHITE, false, null, "QR only", "")
        val combined = Bitmap.createBitmap(first.width + second.width + 80, maxOf(first.height, second.height) + 80, Bitmap.Config.ARGB_8888)
        Canvas(combined).apply { drawColor(Color.WHITE); drawBitmap(first, 0f, 40f, null); drawBitmap(second, (first.width + 80).toFloat(), 40f, null) }
        val directory = File(context.cacheDir, "qr_share").apply { mkdirs() }
        val file = File(directory, "multiple-test.png")
        try {
            file.outputStream().use { combined.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            assertEquals(setOf("https://example.com/first", "https://example.com/second"), decodeGalleryBarcodes(context, uri).map { it.text }.toSet())
        } finally { first.recycle(); second.recycle(); combined.recycle(); file.delete() }
    }
}
