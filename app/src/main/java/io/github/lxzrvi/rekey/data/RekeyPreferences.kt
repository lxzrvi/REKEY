package io.github.lxzrvi.rekey.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** One row of the in-keyboard clipboard history. */
data class ClipEntry(val text: String, val pinned: Boolean)

/** Lightweight local persistence; no network, account, or extra storage dependency. */
class RekeyPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rekey_local", Context.MODE_PRIVATE)

    fun loadAppearance(): AppearanceSettings {
        val defaults = AppearanceSettings()
        val accent = runCatching {
            AccentChoice.valueOf(prefs.getString("accent_v3", null) ?: defaults.accent.name)
        }.getOrDefault(defaults.accent)
        return AppearanceSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: defaults.themeMode.name) }.getOrDefault(defaults.themeMode),
            accent = accent,
            customColor = if (prefs.contains("custom_color")) prefs.getInt("custom_color", 0) else null,
        )
    }

    fun saveAppearance(value: AppearanceSettings) {
        prefs.edit {
            putString("theme", value.themeMode.name)
            putString("accent_v3", value.accent.name)
            if (value.customColor == null) remove("custom_color") else putInt("custom_color", value.customColor)
            // Retired controls: blur, corners, the old palette, motion, contrast, text size.
            remove("blur"); remove("blur_radius"); remove("corners")
            remove("accent"); remove("accent_v2")
            remove("animations"); remove("contrast"); remove("text_scale")
            remove("card_count")
        }
    }

    // ---- First-run setup -------------------------------------------------

    fun isSetupDone(): Boolean = prefs.getBoolean("setup_done", false)
    fun setSetupDone(done: Boolean) = prefs.edit { putBoolean("setup_done", done) }

    // ---- Keyboard layouts ------------------------------------------------

    /** Four shipped layouts on a clean install; the user's own list after that. */
    fun loadPresets(): List<KeyboardPreset> {
        val raw = prefs.getString("presets_v1", null) ?: return KeyboardPalettes.defaults
        return runCatching {
            val arr = JSONArray(raw)
            val out = ArrayList<KeyboardPreset>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val palette = KeyboardPalette.fromJson(o.getString("palette"))
                    ?: continue
                out.add(
                    KeyboardPreset(
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        details = o.optString("details", ""),
                        palette = palette,
                        placeholder = o.optBoolean("placeholder", false),
                        style = KeyboardStyle.fromJson(o.optJSONObject("style")),
                    ),
                )
            }
            out
        }.getOrDefault(KeyboardPalettes.defaults)
    }

    fun savePresets(list: List<KeyboardPreset>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("details", p.details)
                    .put("palette", p.palette.toJson())
                    .put("placeholder", p.placeholder)
                    .put("style", p.style.toJson()),
            )
        }
        prefs.edit { putString("presets_v1", arr.toString()) }
    }

    fun nextPresetId(): Int = prefs.getInt("preset_next_id", 5)
    fun saveNextPresetId(id: Int) = prefs.edit { putInt("preset_next_id", id.coerceAtLeast(5)) }

    fun loadSelectedCard(): Int = prefs.getInt("selected_card", 0).coerceAtLeast(0)
    fun saveSelectedCard(index: Int) = prefs.edit { putInt("selected_card", index.coerceAtLeast(0)) }

    /** Layout the input method should paint with. */
    /** Language chosen for the keyboard, independent of the app's own locale. */
    fun loadKeyboardLanguage(): String = prefs.getString("lang_keyboard", "en") ?: "en"
    fun saveKeyboardLanguage(tag: String) = prefs.edit { putString("lang_keyboard", tag) }

    fun loadAppLanguage(): String = prefs.getString("lang_app", "en") ?: "en"
    fun saveAppLanguage(tag: String) = prefs.edit { putString("lang_app", tag) }

    fun loadActivePresetId(): Int = prefs.getInt("active_preset", 1)
    fun saveActivePresetId(id: Int) = prefs.edit { putInt("active_preset", id) }

    // ---- In-keyboard clipboard history --------------------------------

    fun loadClipHistory(): List<ClipEntry> {
        val raw = prefs.getString("clip_history", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val t = o.optString("t", "")
                    if (t.isNotBlank()) add(ClipEntry(t, o.optBoolean("p", false)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveClipHistory(list: List<ClipEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("t", it.text).put("p", it.pinned)) }
        prefs.edit { putString("clip_history", arr.toString()) }
    }

    // ---- Floating keyboard permission prompt --------------------------

    fun loadFloatAsked(): Boolean = prefs.getBoolean("float_asked", false)
    fun saveFloatAsked() = prefs.edit { putBoolean("float_asked", true) }
}
