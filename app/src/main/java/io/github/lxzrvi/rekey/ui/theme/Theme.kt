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
import io.github.lxzrvi.rekey.data.AccentChoice
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.design.GlassContract

/** iOS system blue. */
val IosBlue = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0A84FF)

private val LightColors = lightColorScheme(
    primary = IosBlue, onPrimary = BrandWhite,
    primaryContainer = IosBlue, onPrimaryContainer = BrandWhite,
    secondary = MutedInk, onSecondary = BrandWhite,
    background = BrandWhite, onBackground = Ink,
    surface = BrandWhite, onSurface = Ink,
    surfaceVariant = Paper, onSurfaceVariant = MutedInk,
    outline = Outline, outlineVariant = Outline,
)

private val DarkColors = darkColorScheme(
    primary = IosBlueDark, onPrimary = BrandWhite,
    primaryContainer = IosBlueDark, onPrimaryContainer = BrandWhite,
    secondary = DarkMutedInk, onSecondary = BrandBlack,
    background = BrandBlack, onBackground = BrandWhite,
    surface = DarkSurface, onSurface = BrandWhite,
    surfaceVariant = DarkSurface, onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline, outlineVariant = DarkOutline,
)

/** One default accent, or the user's own hex colour. */
fun AccentChoice.color(darkTheme: Boolean, customColor: Int? = null): Color = when (this) {
    AccentChoice.DEFAULT -> if (darkTheme) IosBlueDark else IosBlue
    AccentChoice.CUSTOM -> customColor?.let { Color(it) } ?: if (darkTheme) IosBlueDark else IosBlue
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
