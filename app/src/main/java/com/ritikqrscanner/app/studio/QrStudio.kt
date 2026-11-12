package com.ritikqrscanner.app.studio

import android.graphics.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Every exported design must decode back to the exact original payload. */
object QrStudio {
    fun render(content: String, foreground: Int, background: Int, rounded: Boolean, logo: Bitmap?, template: String, title: String, subtitle: String = "", aiTheme: AiQrTheme? = null): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        ))
        val scale = maxOf(4, 768 / matrix.width)
        val size = matrix.width * scale
        val qr = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(qr)
        canvas.drawColor(background)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = foreground }
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (!matrix[x, y]) continue
            val finder = (x < 12 && y < 12) || (x >= matrix.width - 12 && y < 12) || (x < 12 && y >= matrix.height - 12)
            val left = (x * scale).toFloat(); val top = (y * scale).toFloat()
            if (rounded && !finder) canvas.drawRoundRect(left, top, left + scale, top + scale, scale * .4f, scale * .4f, paint)
            else canvas.drawRect(left, top, left + scale, top + scale, paint)
        }
        logo?.let {
            val width = size * .14f
            val rect = RectF((size - width) / 2, (size - width) / 2, (size + width) / 2, (size + width) / 2)
            paint.color = background
            canvas.drawRect(rect.left - scale, rect.top - scale, rect.right + scale, rect.bottom + scale, paint)
            canvas.drawBitmap(it, null, rect, null)
        }
        val output = if (template == "QR only" && aiTheme == null) qr else {
            val card = Bitmap.createBitmap(size + 96, size + 300, Bitmap.Config.ARGB_8888)
            val c = Canvas(card); c.drawColor(aiTheme?.background ?: Color.WHITE)
            paint.color = aiTheme?.primary ?: foreground; c.drawRect(0f, 0f, card.width.toFloat(), 16f, paint)
            aiTheme?.let { drawThemeArtwork(c, card.width.toFloat(), it, paint) }
            paint.textSize = 36f; paint.typeface = Typeface.DEFAULT_BOLD
            val label = title.ifBlank { aiTheme?.name ?: template }
            val safeTitle = android.text.TextUtils.ellipsize(label, android.text.TextPaint(paint), size.toFloat(), android.text.TextUtils.TruncateAt.END).toString()
            c.drawText(safeTitle, 48f, 84f, paint)
            paint.textSize = 24f; paint.typeface = Typeface.DEFAULT
            val safeSubtitle = android.text.TextUtils.ellipsize(subtitle, android.text.TextPaint(paint), size.toFloat(), android.text.TextUtils.TruncateAt.END).toString()
            c.drawText(safeSubtitle, 48f, 124f, paint)
            c.drawBitmap(qr, 48f, 150f, null)
            paint.textSize = 24f; paint.typeface = Typeface.DEFAULT
            c.drawText(aiTheme?.tagline ?: when(template) { "Wi-Fi card" -> if (content.startsWith("WIFI:", ignoreCase = true)) "Scan to set up Wi-Fi" else "Scan for details"; "Business card" -> "Scan to connect"; "Event invitation" -> "Scan for event details"; else -> "Scan with your camera" }, 48f, card.height - 52f, paint)
            qr.recycle(); card
        }
        try { verify(output, content) } catch (error: Exception) {
            output.recycle()
            throw IllegalArgumentException("Design could not be verified. Shorten the content, remove the logo, or use square dots.", error)
        }
        return output
    }

    private fun drawThemeArtwork(canvas: Canvas, width: Float, theme: AiQrTheme, paint: Paint) {
        paint.color = theme.secondary
        paint.alpha = 55
        canvas.drawCircle(width - 60f, 64f, 105f, paint)
        canvas.drawCircle(25f, 190f, 72f, paint)
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.strokeCap = Paint.Cap.ROUND
        when (theme.motif) {
            AiQrTheme.Motif.CAR -> {
                val y = 112f
                canvas.drawPath(Path().apply { moveTo(width - 230f, y); lineTo(width - 190f, y - 42f); lineTo(width - 105f, y - 42f); lineTo(width - 55f, y); close() }, paint)
                canvas.drawLine(width - 245f, y, width - 42f, y, paint)
                canvas.drawCircle(width - 195f, y + 5f, 18f, paint); canvas.drawCircle(width - 88f, y + 5f, 18f, paint)
            }
            AiQrTheme.Motif.NATURE -> repeat(3) { i -> canvas.drawOval(width - 205f + i * 52f, 45f + (i % 2) * 30f, width - 155f + i * 52f, 105f + (i % 2) * 30f, paint) }
            AiQrTheme.Motif.MUSIC -> { canvas.drawLine(width - 170f, 45f, width - 170f, 120f, paint); canvas.drawLine(width - 90f, 30f, width - 90f, 105f, paint); canvas.drawLine(width - 170f, 45f, width - 90f, 30f, paint); canvas.drawCircle(width - 190f, 125f, 20f, paint); canvas.drawCircle(width - 110f, 110f, 20f, paint) }
            AiQrTheme.Motif.SPACE -> { canvas.drawCircle(width - 125f, 80f, 45f, paint); canvas.drawOval(width - 200f, 60f, width - 50f, 100f, paint) }
            AiQrTheme.Motif.LOVE -> canvas.drawPath(Path().apply { moveTo(width - 125f, 130f); cubicTo(width - 230f, 60f, width - 165f, 5f, width - 125f, 52f); cubicTo(width - 85f, 5f, width - 20f, 60f, width - 125f, 130f) }, paint)
            AiQrTheme.Motif.TRAVEL -> { canvas.drawLine(width - 210f, 110f, width - 45f, 50f, paint); canvas.drawLine(width - 45f, 50f, width - 95f, 110f, paint) }
            else -> { canvas.drawCircle(width - 145f, 75f, 48f, paint); canvas.drawCircle(width - 75f, 105f, 28f, paint) }
        }
        paint.style = Paint.Style.FILL
    }

    fun verify(output: Bitmap, content: String) {
        val pixels = IntArray(output.width * output.height)
        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(output.width, output.height, pixels))))
        require(result.text == content) { "Design could not be verified" }
    }
}
