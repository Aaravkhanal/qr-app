package com.ritikqrscanner.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF151B2C)
private val Cloud = Color(0xFFF8F9FF)
private val ElectricBlue = Color(0xFF3156C8)
private val DeepBlue = Color(0xFF19367F)
private val Mint = Color(0xFF006B66)
private val SoftBlue = Color(0xFFDCE1FF)
private val SoftMint = Color(0xFF9FF2E9)

private val LightColors = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlue,
    onPrimaryContainer = Color(0xFF00164E),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F1EA),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF76546C),
    tertiaryContainer = Color(0xFFFFD7EF),
    background = Cloud,
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3E6F0),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C6D0),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB5C4FF),
    onPrimary = Color(0xFF002A79),
    primaryContainer = DeepBlue,
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = SoftMint,
    onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF00504C),
    onSecondaryContainer = Color(0xFFB7F1EA),
    tertiary = Color(0xFFE5BAD4),
    tertiaryContainer = Color(0xFF5C3C53),
    background = Color(0xFF10131B),
    onBackground = Color(0xFFE3E2EC),
    surface = Color(0xFF171A23),
    onSurface = Color(0xFFE3E2EC),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C6D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    error = Color(0xFFFFB4AB)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QrAppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
