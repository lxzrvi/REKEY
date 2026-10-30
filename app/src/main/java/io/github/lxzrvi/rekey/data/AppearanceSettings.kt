package io.github.lxzrvi.rekey.data

/** Seven premium accents; CUSTOM is the eighth, colourful swatch that opens the wheel. */
enum class AccentPalette { PLATINUM, AMETHYST, JADE, AURUM, ROSE, SAPPHIRE, COPPER, CUSTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Appearance-only preferences. Blur is permanently on at a fixed radius and the
 * corner radius is fixed, so neither is user-adjustable any more.
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.PLATINUM,
    val customColor: Int? = null,
    val textScale: Float = 1f,
    val animationsEnabled: Boolean = true,
    val highContrast: Boolean = false,
) {
    fun normalized(): AppearanceSettings = copy(textScale = textScale.coerceIn(0.9f, 1.2f))
}
