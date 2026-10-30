package io.github.lxzrvi.rekey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = BrandWhite,
    primaryContainer = Ink,
    onPrimaryContainer = BrandWhite,
    secondary = MutedInk,
    onSecondary = BrandWhite,
    background = Paper,
    onBackground = Ink,
    surface = BrandWhite,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = MutedInk,
    outline = Outline,
    outlineVariant = Outline,
)

private val DarkColors = darkColorScheme(
    primary = BrandWhite,
    onPrimary = BrandBlack,
    primaryContainer = BrandWhite,
    onPrimaryContainer = BrandBlack,
    secondary = DarkMutedInk,
    onSecondary = BrandBlack,
    background = DarkPaper,
    onBackground = BrandWhite,
    surface = DarkSurface,
    onSurface = BrandWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
)

private val RekeyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RekeyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Intentionally monochrome: OEM dynamic colors must not replace the brand palette.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RekeyTypography,
        shapes = RekeyShapes,
        content = content,
    )
}
