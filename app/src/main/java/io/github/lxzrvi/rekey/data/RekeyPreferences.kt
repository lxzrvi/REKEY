package io.github.lxzrvi.rekey.data

import android.content.Context
import androidx.core.content.edit

/** Lightweight local persistence; no network, account, or extra storage dependency. */
class RekeyPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rekey_local", Context.MODE_PRIVATE)

    fun loadAppearance(): AppearanceSettings {
        val defaults = AppearanceSettings()
        return AppearanceSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: defaults.themeMode.name) }.getOrDefault(defaults.themeMode),
            accent = runCatching { AccentPalette.valueOf(prefs.getString("accent", null) ?: defaults.accent.name) }.getOrDefault(defaults.accent),
            blurEnabled = prefs.getBoolean("blur", defaults.blurEnabled),
            blurRadius = prefs.getFloat("blur_radius", defaults.blurRadius),
            cornerRadius = prefs.getFloat("corners", defaults.cornerRadius),
            textScale = prefs.getFloat("text_scale", defaults.textScale),
            animationsEnabled = prefs.getBoolean("animations", defaults.animationsEnabled),
            highContrast = prefs.getBoolean("contrast", defaults.highContrast),
        ).normalized()
    }

    fun saveAppearance(value: AppearanceSettings) {
        val v = value.normalized()
        prefs.edit {
            putString("theme", v.themeMode.name)
            putString("accent", v.accent.name)
            putBoolean("blur", v.blurEnabled)
            putFloat("blur_radius", v.blurRadius)
            putFloat("corners", v.cornerRadius)
            putFloat("text_scale", v.textScale)
            putBoolean("animations", v.animationsEnabled)
            putBoolean("contrast", v.highContrast)
        }
    }

    fun loadCardCount(): Int = prefs.getInt("card_count", 2).coerceIn(2, 10_000)
    fun saveCardCount(count: Int) = prefs.edit { putInt("card_count", count.coerceIn(2, 10_000)) }
    fun loadSelectedCard(): Int = prefs.getInt("selected_card", 1).coerceAtLeast(0)
    fun saveSelectedCard(index: Int) = prefs.edit { putInt("selected_card", index.coerceAtLeast(0)) }
}
