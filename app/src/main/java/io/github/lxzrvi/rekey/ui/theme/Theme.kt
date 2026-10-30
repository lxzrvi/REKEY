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
import io.github.lxzrvi.rekey.design.GlassContract

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

/** Premium accent pairs: a deep tone for light mode, a lifted tone for dark mode. */
fun AccentPalette.color(darkTheme: Boolean, customColor: Int? = null): Color = when (this) {
    AccentPalette.PLATINUM -> if (darkTheme) Color(0xFFB8D4E8) else Color(0xFF3D5A73)
    AccentPalette.AMETHYST -> if (darkTheme) Color(0xFFC7B5FF) else Color(0xFF6B4BB8)
    AccentPalette.JADE -> if (darkTheme) Color(0xFF8FE6C4) else Color(0xFF1F7A5C)
    AccentPalette.AURUM -> if (darkTheme) Color(0xFFF2CE7B) else Color(0xFF8A6410)
    AccentPalette.ROSE -> if (darkTheme) Color(0xFFFFAFC6) else Color(0xFFA8365A)
    AccentPalette.SAPPHIRE -> if (darkTheme) Color(0xFFA6C0FF) else Color(0xFF2547A8)
    AccentPalette.COPPER -> if (darkTheme) Color(0xFFF0B48A) else Color(0xFF8A4B2A)
    AccentPalette.CUSTOM -> customColor?.let { Color(it) } ?: if (darkTheme) Color(0xFFB8D4E8) else Color(0xFF3D5A73)
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
    val radius = GlassContract.CORNER_DP.dp
    MaterialTheme(
        colorScheme = colors,
        typography = RekeyTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(radius),
            large = RoundedCornerShape(radius),
            extraLarge = RoundedCornerShape(radius + 4.dp),
        ),
        content = content,
    )
}
