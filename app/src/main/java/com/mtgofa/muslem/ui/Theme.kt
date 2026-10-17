package com.mtgofa.muslem.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.R

object Palette {
    val Emerald = Color(0xFF0F7A62)
    val EmeraldDark = Color(0xFF0A5A48)
    val EmeraldSoft = Color(0xFFE3F2EC)
    val Gold = Color(0xFFC8963E)
    val GoldSoft = Color(0xFFF7EEDC)
    val Bg = Color(0xFFF5F6F3)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF17201C)
    val Muted = Color(0xFF6A746F)
    val Line = Color(0xFFE6E9E5)
    val Paper = Color(0xFFFFF8EB)
}

val Plex = FontFamily(
    Font(R.font.plexar_regular, FontWeight.Normal),
    Font(R.font.plexar_semibold, FontWeight.SemiBold),
    Font(R.font.plexar_bold, FontWeight.Bold),
)

private val base = TextStyle(fontFamily = Plex)

private val AppTypography = Typography(
    displayLarge = base.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    displayMedium = base.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    headlineSmall = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleLarge = base.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
)

private val Colors = lightColorScheme(
    primary = Palette.Emerald,
    onPrimary = Color.White,
    primaryContainer = Palette.EmeraldSoft,
    onPrimaryContainer = Palette.EmeraldDark,
    secondary = Palette.Gold,
    onSecondary = Color.White,
    secondaryContainer = Palette.GoldSoft,
    onSecondaryContainer = Color(0xFF6B4A12),
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Color(0xFFEFF1EE),
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.Surface,
    surfaceContainerHighest = Color(0xFFEFF1EE),
    surfaceContainerLow = Palette.Surface,
    outline = Color(0xFFCBD1CC),
    outlineVariant = Palette.Line,
)

@Composable
fun MuslemTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = AppTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
