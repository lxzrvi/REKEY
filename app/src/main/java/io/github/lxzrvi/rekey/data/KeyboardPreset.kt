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
) {
    fun toJson(): String = listOf(
        tray, board, key, special, text, canvas, enter, enterText, icon,
    ).joinToString(",") { it.value.toString() }

    companion object {
        fun fromJson(raw: String): KeyboardPalette? {
            val parts = raw.split(",")
            if (parts.size != 9) return null
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
)

object KeyboardPalettes {

    /** The light layout exactly as specified. */
    val Paper = KeyboardPalette(
        tray = Color(0xFF85858A),
        board = Color(0xFFE9E9ED),
        key = Color(0xFFFFFFFF),
        special = Color(0xFFD9D9DF),
        text = Color(0xFF25252B),
        canvas = Color(0xFF85858A),
        enter = Color(0xFF5267A8),
        enterText = Color(0xFFFFFFFF),
        icon = Color(0xFF25252B),
    )

    /** Dark counterpart of Paper. */
    val Graphite = KeyboardPalette(
        tray = Color(0xFF0B0B0D),
        board = Color(0xFF1C1C20),
        key = Color(0xFF2E2E34),
        special = Color(0xFF242429),
        text = Color(0xFFECECF2),
        canvas = Color(0xFF0B0B0D),
        enter = Color(0xFF5267A8),
        enterText = Color(0xFFFFFFFF),
        icon = Color(0xFFECECF2),
    )

    /** Warm light. */
    val Ivory = KeyboardPalette(
        tray = Color(0xFF8C8377),
        board = Color(0xFFF3EDE3),
        key = Color(0xFFFFFCF6),
        special = Color(0xFFE2D9C9),
        text = Color(0xFF332C22),
        canvas = Color(0xFF8C8377),
        enter = Color(0xFF9A7B4F),
        enterText = Color(0xFFFFFCF6),
        icon = Color(0xFF332C22),
    )

    /** Deep blue dark. */
    val Midnight = KeyboardPalette(
        tray = Color(0xFF070B14),
        board = Color(0xFF121826),
        key = Color(0xFF1F2739),
        special = Color(0xFF182031),
        text = Color(0xFFE4EAF6),
        canvas = Color(0xFF070B14),
        enter = Color(0xFF4C6FBF),
        enterText = Color(0xFFFFFFFF),
        icon = Color(0xFFE4EAF6),
    )

    /** Used for a brand-new card so it matches the app's current theme. */
    fun forTheme(isDark: Boolean): KeyboardPalette = if (isDark) Graphite else Paper

    val defaults: List<KeyboardPreset> = listOf(
        KeyboardPreset(
            id = 1,
            name = "Paper",
            details = "Light keys on a soft grey board, with a slate blue return key.",
            palette = Paper,
        ),
        KeyboardPreset(
            id = 2,
            name = "Graphite",
            details = "Near-black board, raised charcoal keys and a slate blue return key.",
            palette = Graphite,
        ),
        KeyboardPreset(
            id = 3,
            name = "Ivory",
            details = "Warm cream keys on a sand board, finished with a bronze return key.",
            palette = Ivory,
        ),
        KeyboardPreset(
            id = 4,
            name = "Midnight",
            details = "Deep navy board with cool blue-grey keys and a bright blue return key.",
            palette = Midnight,
        ),
    )
}
