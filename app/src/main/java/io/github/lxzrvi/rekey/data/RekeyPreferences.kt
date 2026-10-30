package io.github.lxzrvi.rekey.data

import android.content.Context
import androidx.core.content.edit

/** Lightweight local persistence; no network, account, or extra storage dependency. */
class RekeyPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rekey_local", Context.MODE_PRIVATE)

    fun loadAppearance(): AppearanceSettings {
        val defaults = AppearanceSettings()
        // accent_v3 only knows DEFAULT / CUSTOM. Older installs fall back to DEFAULT.
        val accent = runCatching {
            AccentChoice.valueOf(prefs.getString("accent_v3", null) ?: defaults.accent.name)
        }.getOrDefault(defaults.accent)
        return AppearanceSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: defaults.themeMode.name) }.getOrDefault(defaults.themeMode),
            accent = accent,
            customColor = if (prefs.contains("custom_color")) prefs.getInt("custom_color", 0) else null,
            textScale = prefs.getFloat("text_scale", defaults.textScale),
        ).normalized()
    }

    fun saveAppearance(value: AppearanceSettings) {
        val v = value.normalized()
        prefs.edit {
            putString("theme", v.themeMode.name)
            putString("accent_v3", v.accent.name)
            if (v.customColor == null) remove("custom_color") else putInt("custom_color", v.customColor)
            putFloat("text_scale", v.textScale)
            // Retired controls: blur, corners, the old palette, motion and contrast.
            remove("blur"); remove("blur_radius"); remove("corners")
            remove("accent"); remove("accent_v2")
            remove("animations"); remove("contrast")
        }
    }

    fun loadCardCount(): Int = prefs.getInt("card_count", 4).coerceIn(0, 10_000)
    fun saveCardCount(count: Int) = prefs.edit { putInt("card_count", count.coerceIn(0, 10_000)) }
    fun loadSelectedCard(): Int = prefs.getInt("selected_card", 0).coerceAtLeast(0)
    fun saveSelectedCard(index: Int) = prefs.edit { putInt("selected_card", index.coerceAtLeast(0)) }
}
