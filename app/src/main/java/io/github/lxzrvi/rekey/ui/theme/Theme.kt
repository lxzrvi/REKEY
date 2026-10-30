package io.github.lxzrvi.rekey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.lxzrvi.rekey.data.AccentPalette
import io.github.lxzrvi.rekey.data.AppearanceSettings

private val LightColors = lightColorScheme(
    primary = Ink, onPrimary = BrandWhite,
    primaryContainer = Ink, onPrimaryContainer = BrandWhite,
    secondary = MutedInk, onSecondary = BrandWhite,
    background = Paper, onBackground = Ink,
    surface = BrandWhite, onSurface = Ink,
    surfaceVariant = Paper, onSurfaceVariant = MutedInk,
    outline = Outline, outlineVariant = Outline,
)

private val DarkColors = darkColorScheme(
    primary = BrandWhite, onPrimary = BrandBlack,
    primaryContainer = BrandWhite, onPrimaryContainer = BrandBlack,
    secondary = DarkMutedInk, onSecondary = BrandBlack,
    background = DarkPaper, onBackground = BrandWhite,
    surface = DarkSurface, onSurface = BrandWhite,
    surfaceVariant = DarkSurface, onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline, outlineVariant = DarkOutline,
)

fun AccentPalette.color(darkTheme: Boolean, customColor: Int? = null): Color = when (this) {
    AccentPalette.ICE -> if (darkTheme) Color(0xFF9DD8FF) else Color(0xFF286C9E)
    AccentPalette.VIOLET -> if (darkTheme) Color(0xFFC7B5FF) else Color(0xFF6B4BB8)
    AccentPalette.MINT -> if (darkTheme) Color(0xFF9FE8CB) else Color(0xFF237A5B)
    AccentPalette.AMBER -> if (darkTheme) Color(0xFFFFD28F) else Color(0xFF9A6200)
    AccentPalette.CUSTOM -> customColor?.let { Color(it) } ?: if (darkTheme) Color(0xFF9DD8FF) else Color(0xFF286C9E)
}

@Composable
fun RekeyTheme(
    settings: AppearanceSettings = AppearanceSettings(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColors else LightColors
    val accent = settings.accent.color(darkTheme, settings.customColor)
    val colors = base.copy(
        primary = accent,
        secondary = accent,
        primaryContainer = accent.copy(alpha = if (darkTheme) 0.22f else 0.14f),
        onPrimaryContainer = if (darkTheme) BrandWhite else Ink,
        outline = if (settings.highContrast) base.onBackground.copy(alpha = 0.75f) else base.outline,
        outlineVariant = if (settings.highContrast) base.onBackground.copy(alpha = 0.5f) else base.outlineVariant,
    )
    val radius = settings.cornerRadius.dp
    MaterialTheme(
        colorScheme = colors,
        typography = RekeyTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(radius * 0.7f),
            large = RoundedCornerShape(radius),
            extraLarge = RoundedCornerShape(radius + 4.dp),
        ),
        content = content,
    )
}
