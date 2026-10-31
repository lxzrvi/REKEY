package io.github.lxzrvi.rekey.data

import androidx.compose.ui.graphics.Color

/**
 * The colours a single keyboard layout paints with.
 *
 * Kept as plain [Color] values (not resource ids) so a layout can be stored,
 * edited and handed to the input method without a theme lookup.
 */
data class KeyboardPalette(
    val tray: Color,
    val board: Color,
    val key: Color,
    val special: Color,
    val text: Color,
    val canvas: Color,
    val enter: Color,
    val enterText: Color,
    val icon: Color,
    /** Space-bar wordmark and other secondary labels. */
    val muted: Color,
    /** Hairline around every key. */
    val border: Color,
) {
    fun toJson(): String = listOf(
        tray, board, key, special, text, canvas, enter, enterText, icon, muted, border,
    ).joinToString(",") { it.value.toString() }

    companion object {
        fun fromJson(raw: String): KeyboardPalette? {
            val parts = raw.split(",")
            // Layouts saved before the space-bar wordmark existed carry 9 colours.
            if (parts.size != 9 && parts.size != 11) return null
            val c = parts.map { it.toULongOrNull() ?: return null }
            return KeyboardPalette(
                tray = Color(c[0]),
                board = Color(c[1]),
                key = Color(c[2]),
                special = Color(c[3]),
                text = Color(c[4]),
                canvas = Color(c[5]),
                enter = Color(c[6]),
                enterText = Color(c[7]),
                icon = Color(c[8]),
                muted = if (c.size == 11) Color(c[9]) else Color(c[4]).copy(alpha = 0.55f),
                border = if (c.size == 11) Color(c[10]) else Color(c[4]).copy(alpha = 0.07f),
            )
        }
    }
}

/**
 * One saved keyboard layout.
 *
 * [placeholder] marks a freshly added card: it shows "No name" / "Tap to edit"
 * until the owner names it in the editor.
 */
data class KeyboardPreset(
    val id: Int,
    val name: String,
    val details: String,
    val palette: KeyboardPalette,
    val placeholder: Boolean = false,
    /** Everything the wizard edits. Defaults keep old saves looking identical. */
    val style: KeyboardStyle = KeyboardStyle(),
)

object KeyboardPalettes {

    /** Light. */
    val Paper = KeyboardPalette(
        tray = Color(0xFFE9E9ED),
        board = Color(0xFFE9E9ED),
        key = Color(0xFFFFFFFF),
        special = Color(0xFFD9D9DF),
        text = Color(0xFF25252B),
        canvas = Color(0xFFE9E9ED),
        enter = Color(0xFF5267A8),
        enterText = Color(0xFFFFFFFF),
        icon = Color(0xFF25252B),
        muted = Color(0xFF777780),
        border = Color(0x12000000),
    )

    /** Dark. */
    val Graphite = KeyboardPalette(
        tray = Color(0xFF303036),
        board = Color(0xFF303036),
        key = Color(0xFF45454D),
        special = Color(0xFF55555E),
        text = Color(0xFFF5F5F7),
        canvas = Color(0xFF303036),
        enter = Color(0xFF8299EF),
        enterText = Color(0xFFFFFFFF),
        icon = Color(0xFFF5F5F7),
        muted = Color(0xFFB0B0BA),
        border = Color(0x0FFFFFFF),
    )

    /** Cream. */
    val Ivory = KeyboardPalette(
        tray = Color(0xFFEEE6D8),
        board = Color(0xFFEEE6D8),
        key = Color(0xFFFFFAF0),
        special = Color(0xFFDED3C0),
        text = Color(0xFF3D352C),
        canvas = Color(0xFFEEE6D8),
        enter = Color(0xFF9B7049),
        enterText = Color(0xFFFFFAF0),
        icon = Color(0xFF3D352C),
        muted = Color(0xFF897D6E),
        border = Color(0x14462F1E),
    )

    /** AMOLED. */
    val Midnight = KeyboardPalette(
        tray = Color(0xFF000000),
        board = Color(0xFF000000),
        key = Color(0xFF191919),
        special = Color(0xFF292929),
        text = Color(0xFFF5F5F5),
        canvas = Color(0xFF000000),
        enter = Color(0xFF777777),
        enterText = Color(0xFFFFFFFF),
        icon = Color(0xFFF5F5F5),
        muted = Color(0xFF999999),
        border = Color(0x12FFFFFF),
    )

    /** Used for a brand-new card so it matches the app's current theme. */
    fun forTheme(isDark: Boolean): KeyboardPalette = if (isDark) Graphite else Paper

    /**
     * The four shipped layouts. They can be renamed, re-described and used, but
     * never removed — a keyboard must always have something to fall back to.
     * Custom layouts start at id 5, so this range is stable across saves.
     */
    val defaultIds: IntRange = 1..4

    fun isDefault(preset: KeyboardPreset): Boolean = preset.id in defaultIds

    val defaults: List<KeyboardPreset> = listOf(
        KeyboardPreset(
            id = 1,
            name = "Paper",
            details = "White keys on soft grey, with a slate blue return key.",
            palette = Paper,
        ),
        KeyboardPreset(
            id = 2,
            name = "Graphite",
            details = "Charcoal keys on grey, with a periwinkle return key.",
            palette = Graphite,
        ),
        KeyboardPreset(
            id = 3,
            name = "Ivory",
            details = "Warm cream keys on sand, finished with a bronze return key.",
            palette = Ivory,
        ),
        KeyboardPreset(
            id = 4,
            name = "Midnight",
            details = "True black AMOLED with near-black keys and a grey return key.",
            palette = Midnight,
        ),
    )
}
