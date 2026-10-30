package io.github.lxzrvi.rekey.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun defaultsUsePureBlurAndSystemTheme() {
        val settings = AppearanceSettings()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.blurEnabled)
        assertEquals(28f, settings.blurRadius)
        assertTrue(settings.animationsEnabled)
    }

    @Test
    fun persistedValuesAreClampedToSafeRanges() {
        val settings = AppearanceSettings(blurRadius = 999f, cornerRadius = -4f, textScale = 4f).normalized()
        assertEquals(40f, settings.blurRadius)
        assertEquals(12f, settings.cornerRadius)
        assertEquals(1.2f, settings.textScale)
    }

    @Test
    fun resetReturnsToKnownDefaults() {
        val changed = AppearanceSettings(themeMode = ThemeMode.DARK, accent = AccentPalette.AMBER, blurEnabled = false)
        assertEquals(ThemeMode.DARK, changed.themeMode)
        assertEquals(AppearanceSettings(), AppearanceSettings().normalized())
    }
}
