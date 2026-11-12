package com.ritikqrscanner.app.studio

import android.graphics.Color
import java.util.Locale
import kotlin.math.abs

/** A deterministic, on-device theme planner. No prompt or QR payload leaves the phone. */
data class AiQrTheme(
    val prompt: String,
    val name: String,
    val motif: Motif,
    val primary: Int,
    val secondary: Int,
    val background: Int,
    val tagline: String
) {
    enum class Motif { CAR, NATURE, FOOD, MUSIC, SPACE, LOVE, BUSINESS, TRAVEL, CELEBRATION, ABSTRACT }
}

object AiQrThemePlanner {
    val suggestions = listOf("Sports car", "Nature", "Coffee shop", "Music", "Space", "Wedding", "Travel")

    fun create(prompt: String): AiQrTheme {
        val clean = prompt.trim().take(120)
        require(clean.isNotBlank()) { "Describe the theme you want." }
        val text = clean.lowercase(Locale.ROOT)
        val motif = when {
            listOf("car", "auto", "vehicle", "racing", "motor").any(text::contains) -> AiQrTheme.Motif.CAR
            listOf("nature", "forest", "plant", "eco", "leaf", "garden").any(text::contains) -> AiQrTheme.Motif.NATURE
            listOf("coffee", "cafe", "food", "restaurant", "pizza", "bakery").any(text::contains) -> AiQrTheme.Motif.FOOD
            listOf("music", "song", "dj", "concert", "band").any(text::contains) -> AiQrTheme.Motif.MUSIC
            listOf("space", "galaxy", "star", "planet", "cosmic").any(text::contains) -> AiQrTheme.Motif.SPACE
            listOf("wedding", "love", "heart", "romantic").any(text::contains) -> AiQrTheme.Motif.LOVE
            listOf("business", "professional", "company", "corporate").any(text::contains) -> AiQrTheme.Motif.BUSINESS
            listOf("travel", "flight", "beach", "holiday", "adventure").any(text::contains) -> AiQrTheme.Motif.TRAVEL
            listOf("party", "birthday", "celebration", "festival").any(text::contains) -> AiQrTheme.Motif.CELEBRATION
            else -> AiQrTheme.Motif.ABSTRACT
        }
        val colors = when (motif) {
            AiQrTheme.Motif.CAR -> intArrayOf(0xFFB71C1C.toInt(), 0xFFFFB300.toInt(), 0xFFFFF8F0.toInt())
            AiQrTheme.Motif.NATURE -> intArrayOf(0xFF155D27.toInt(), 0xFF7CB342.toInt(), 0xFFF1F8E9.toInt())
            AiQrTheme.Motif.FOOD -> intArrayOf(0xFF5D4037.toInt(), 0xFFFF8F00.toInt(), 0xFFFFF8E1.toInt())
            AiQrTheme.Motif.MUSIC -> intArrayOf(0xFF4A148C.toInt(), 0xFFE040FB.toInt(), 0xFFF8F0FF.toInt())
            AiQrTheme.Motif.SPACE -> intArrayOf(0xFF101C4C.toInt(), 0xFF7C4DFF.toInt(), 0xFFF0F2FF.toInt())
            AiQrTheme.Motif.LOVE -> intArrayOf(0xFFAD1457.toInt(), 0xFFF06292.toInt(), 0xFFFFF0F5.toInt())
            AiQrTheme.Motif.BUSINESS -> intArrayOf(0xFF0D3B66.toInt(), 0xFF2A9D8F.toInt(), 0xFFF1F7FA.toInt())
            AiQrTheme.Motif.TRAVEL -> intArrayOf(0xFF006064.toInt(), 0xFF00ACC1.toInt(), 0xFFEFFBFC.toInt())
            AiQrTheme.Motif.CELEBRATION -> intArrayOf(0xFF6A1B9A.toInt(), 0xFFFFB300.toInt(), 0xFFFFF8E7.toInt())
            AiQrTheme.Motif.ABSTRACT -> generatedPalette(clean)
        }
        return AiQrTheme(clean, clean.replaceFirstChar { it.titlecase(Locale.ROOT) }, motif, colors[0], colors[1], colors[2], tagline(motif))
    }

    private fun generatedPalette(seed: String): IntArray {
        val hue = (abs(seed.hashCode().toLong()) % 360).toFloat()
        return intArrayOf(
            Color.HSVToColor(floatArrayOf(hue, .82f, .48f)),
            Color.HSVToColor(floatArrayOf((hue + 42f) % 360f, .68f, .86f)),
            Color.HSVToColor(floatArrayOf(hue, .08f, .98f))
        )
    }

    private fun tagline(motif: AiQrTheme.Motif) = when (motif) {
        AiQrTheme.Motif.CAR -> "Scan it. Start your engines."
        AiQrTheme.Motif.NATURE -> "A fresh connection awaits."
        AiQrTheme.Motif.FOOD -> "Scan for something delicious."
        AiQrTheme.Motif.MUSIC -> "Turn up the connection."
        AiQrTheme.Motif.SPACE -> "Your next discovery is one scan away."
        AiQrTheme.Motif.LOVE -> "Made for a special moment."
        AiQrTheme.Motif.BUSINESS -> "Connect in one quick scan."
        AiQrTheme.Motif.TRAVEL -> "Scan into your next adventure."
        AiQrTheme.Motif.CELEBRATION -> "The celebration starts here."
        AiQrTheme.Motif.ABSTRACT -> "A QR made uniquely for you."
    }
}
