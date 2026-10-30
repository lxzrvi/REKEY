package io.github.lxzrvi.rekey.data

/**
 * Only two accent choices are exposed: the iOS-style blue default, or a custom
 * hex colour. The older premium palette is parked in [ParkedAccents].
 */
enum class AccentChoice { DEFAULT, CUSTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Appearance-only preferences. Blur, corners, animations and text size are all
 * fixed by contract now, so the drawer only carries theme and accent.
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentChoice = AccentChoice.DEFAULT,
    val customColor: Int? = null,
)
