package io.github.lxzrvi.rekey.data

import io.github.lxzrvi.rekey.design.GlassContract

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentPalette { ICE, VIOLET, MINT, AMBER }

/** Appearance-only preferences; no product behavior is inferred from the demo cards. */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.ICE,
    val blurEnabled: Boolean = true,
    val blurRadius: Float = GlassContract.DEFAULT_BLUR_DP,
    val cornerRadius: Float = 24f,
    val textScale: Float = 1f,
    val animationsEnabled: Boolean = true,
    val highContrast: Boolean = false,
) {
    fun normalized(): AppearanceSettings = copy(
        blurRadius = blurRadius.coerceIn(GlassContract.MIN_BLUR_DP, GlassContract.MAX_BLUR_DP),
        cornerRadius = cornerRadius.coerceIn(12f, 36f),
        textScale = textScale.coerceIn(0.9f, 1.2f),
    )
}
