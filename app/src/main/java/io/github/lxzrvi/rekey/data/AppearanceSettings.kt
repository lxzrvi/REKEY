package io.github.lxzrvi.rekey.data

/**
 * Only two accent choices are exposed: the iOS-style blue default, or a custom
 * hex colour. The older premium palette is parked in [ParkedAccents].
 */
enum class AccentChoice { DEFAULT, CUSTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Appearance-only preferences. Blur is permanently on at a fixed radius, corners
 * are fixed, animations are always on and high contrast is gone, so none of those
 * are user-adjustable any more.
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentChoice = AccentChoice.DEFAULT,
    val customColor: Int? = null,
    val textScale: Float = 1f,
) {
    fun normalized(): AppearanceSettings = copy(textScale = textScale.coerceIn(0.9f, 1.2f))
}
