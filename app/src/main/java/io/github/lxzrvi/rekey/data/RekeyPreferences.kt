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
            accent = runCatching { AccentPalette.valueOf(prefs.getString("accent_v2", null) ?: defaults.accent.name) }.getOrDefault(defaults.accent),
            customColor = if (prefs.contains("custom_color")) prefs.getInt("custom_color", 0) else null,
            textScale = prefs.getFloat("text_scale", defaults.textScale),
            animationsEnabled = prefs.getBoolean("animations", defaults.animationsEnabled),
            highContrast = prefs.getBoolean("contrast", defaults.highContrast),
        ).normalized()
    }

    fun saveAppearance(value: AppearanceSettings) {
        val v = value.normalized()
        prefs.edit {
            putString("theme", v.themeMode.name)
            putString("accent_v2", v.accent.name)
            if (v.customColor == null) remove("custom_color") else putInt("custom_color", v.customColor)
            putFloat("text_scale", v.textScale)
            putBoolean("animations", v.animationsEnabled)
            putBoolean("contrast", v.highContrast)
            // Retired controls: blur toggle, blur strength and corner radius.
            remove("blur"); remove("blur_radius"); remove("corners"); remove("accent")
        }
    }

    fun loadCardCount(): Int = prefs.getInt("card_count", 4).coerceIn(0, 10_000)
    fun saveCardCount(count: Int) = prefs.edit { putInt("card_count", count.coerceIn(0, 10_000)) }
    fun loadSelectedCard(): Int = prefs.getInt("selected_card", 0).coerceAtLeast(0)
    fun saveSelectedCard(index: Int) = prefs.edit { putInt("selected_card", index.coerceAtLeast(0)) }
}
