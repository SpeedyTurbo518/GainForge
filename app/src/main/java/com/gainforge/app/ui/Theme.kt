package com.gainforge.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

val IosBlue = Color(0xFF0A84FF)

@Immutable
data class Palette(
    val dark: Boolean,
    val accent: Color,
    val bg: Color,
    val text: Color,
    val subtext: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val border: Color,
    val divider: Color,
    val field: Color,
    val bar: Color,
    val sheet: Color,
    val good: Color = Color(0xFF30D158),
    val warn: Color = Color(0xFFFF9F0A),
    val danger: Color = Color(0xFFFF453A),
    val mint: Color = Color(0xFF63E6E2)
)

fun palette(dark: Boolean, accent: Color): Palette = if (dark) Palette(
    dark = true, accent = accent, bg = Color.Black, text = Color.White, subtext = Color(0xFF98989F),
    glassTop = Color.White.copy(alpha = .11f), glassBottom = Color.White.copy(alpha = .04f),
    border = Color.White.copy(alpha = .22f), divider = Color.White.copy(alpha = .10f),
    field = Color.White.copy(alpha = .08f), bar = Color(0xFF141417), sheet = Color(0xFF1C1C1E)
) else Palette(
    dark = false, accent = accent, bg = Color(0xFFF2F4F8), text = Color(0xFF0B0B0C), subtext = Color(0xFF6E6E73),
    glassTop = Color.White.copy(alpha = .9f), glassBottom = Color.White.copy(alpha = .62f),
    border = Color.White, divider = Color.Black.copy(alpha = .07f),
    field = Color.Black.copy(alpha = .045f), bar = Color(0xFFFCFDFF), sheet = Color.White
)

val LocalPalette = staticCompositionLocalOf { palette(false, IosBlue) }

@Composable
fun GlassTheme(dark: Boolean, accent: Color, content: @Composable () -> Unit) {
    val p = palette(dark, accent)
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = Color.White, background = Color.Black, onBackground = Color.White,
        surface = Color(0xFF1C1C1E), onSurface = Color.White, surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerLow = Color(0xFF1C1C1E), onSurfaceVariant = Color(0xFF98989F)
    ) else lightColorScheme(
        primary = accent, onPrimary = Color.White, background = p.bg, onBackground = p.text,
        surface = Color.White, onSurface = p.text, surfaceContainerHigh = Color.White,
        surfaceContainerLow = Color.White, onSurfaceVariant = p.subtext
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPalette provides p, LocalContentColor provides p.text, content = content)
    }
}
