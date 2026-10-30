package io.github.lxzrvi.rekey.data

/**
 * The 0.5.0 premium palette, kept on purpose for a later release.
 *
 * Nothing in the UI reads this today — the drawer only offers Default and Custom.
 * Values are ARGB longs so they can be dropped straight back into a ColorScheme
 * when the palette returns. Light tone first, dark tone second.
 */
object ParkedAccents {
    data class Pair(val name: String, val light: Long, val dark: Long)

    val PALETTE: List<Pair> = listOf(
        Pair("Platinum", 0xFF3D5A73, 0xFFB8D4E8),
        Pair("Amethyst", 0xFF6B4BB8, 0xFFC7B5FF),
        Pair("Jade", 0xFF1F7A5C, 0xFF8FE6C4),
        Pair("Aurum", 0xFF8A6410, 0xFFF2CE7B),
        Pair("Rose", 0xFFA8365A, 0xFFFFAFC6),
        Pair("Sapphire", 0xFF2547A8, 0xFFA6C0FF),
        Pair("Copper", 0xFF8A4B2A, 0xFFF0B48A),
    )

    /** The old eighth swatch: a sweep-gradient rainbow that opened the colour wheel. */
    val RAINBOW: List<Long> = listOf(
        0xFFFF5F6D, 0xFFFFC371, 0xFFF9F871, 0xFF4FD1A5,
        0xFF4FA8FF, 0xFF9B6BFF, 0xFFFF6BD6, 0xFFFF5F6D,
    )
}
