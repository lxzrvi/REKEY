package io.github.lxzrvi.rekey.ui.keyboard

import io.github.lxzrvi.rekey.ui.components.Glyph

/**
 * Emoji grouped the way the system picker groups them.
 *
 * Every entry is a single code point. Sequences joined with ZWJ or carrying a
 * variation selector are deliberately excluded: one KEYCODE_DEL removes half of
 * such a sequence and leaves a broken glyph in the field.
 */
internal data class EmojiGroup(val icon: Glyph, val title: String, val items: List<String>)

internal val EmojiGroups: List<EmojiGroup> = listOf(
    EmojiGroup(
        Glyph.SMILEY, "Smileys",
        listOf(
            "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06", "\uD83D\uDE05", "\uD83D\uDE02", "\uD83E\uDD23",
            "\uD83E\uDD72", "\uD83D\uDE0A", "\uD83D\uDE07", "\uD83D\uDE42", "\uD83D\uDE43", "\uD83D\uDE09", "\uD83D\uDE0C", "\uD83D\uDE0D",
            "\uD83E\uDD70", "\uD83D\uDE18", "\uD83D\uDE17", "\uD83D\uDE19", "\uD83D\uDE1A", "\uD83D\uDE0B", "\uD83D\uDE1B", "\uD83D\uDE1C",
            "\uD83E\uDD2A", "\uD83D\uDE1D", "\uD83E\uDD11", "\uD83E\uDD17", "\uD83E\uDD2D", "\uD83E\uDD2B", "\uD83E\uDD14", "\uD83E\uDD10",
            "\uD83E\uDD28", "\uD83D\uDE10", "\uD83D\uDE11", "\uD83D\uDE36", "\uD83D\uDE0F", "\uD83D\uDE12", "\uD83D\uDE44", "\uD83D\uDE2C",
            "\uD83E\uDD25", "\uD83D\uDE0E", "\uD83E\uDD13", "\uD83E\uDDD0", "\uD83D\uDE15", "\uD83D\uDE1F", "\uD83D\uDE41", "\uD83D\uDE2E",
            "\uD83D\uDE2F", "\uD83D\uDE32", "\uD83D\uDE33", "\uD83E\uDD7A", "\uD83D\uDE26", "\uD83D\uDE27", "\uD83D\uDE28", "\uD83D\uDE30",
            "\uD83D\uDE25", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE31", "\uD83D\uDE16", "\uD83D\uDE23", "\uD83D\uDE1E", "\uD83D\uDE13",
            "\uD83D\uDE29", "\uD83D\uDE2A", "\uD83E\uDD24", "\uD83D\uDE34", "\uD83D\uDE24", "\uD83D\uDE21", "\uD83D\uDE20", "\uD83E\uDD2C",
            "\uD83D\uDE08", "\uD83D\uDC7F", "\uD83D\uDC80", "\uD83D\uDC7B", "\uD83D\uDC7D", "\uD83E\uDD16", "\uD83E\uDD73", "\uD83E\uDD2F",
        ),
    ),
    EmojiGroup(
        Glyph.CAPS_ON, "Gestures",
        listOf(
            "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4A", "\uD83D\uDC4F", "\uD83D\uDE4C", "\uD83D\uDC50", "\uD83E\uDD32", "\uD83E\uDD1D",
            "\uD83D\uDE4F", "\uD83D\uDC4C", "\uD83E\uDD0C", "\uD83E\uDD0F", "\uD83E\uDD1E", "\uD83E\uDD1F", "\uD83E\uDD18", "\uD83E\uDD19",
            "\uD83E\uDD1A", "\uD83D\uDD90", "\uD83D\uDC4B", "\uD83E\uDD1A", "\uD83D\uDD96", "\uD83D\uDC48", "\uD83D\uDC49", "\uD83D\uDC46",
            "\uD83D\uDC47", "\uD83D\uDCAA", "\uD83E\uDDBE", "\uD83E\uDDBF", "\uD83D\uDC40", "\uD83D\uDC41", "\uD83E\uDDE0", "\uD83D\uDDE3",
            "\uD83D\uDC64", "\uD83D\uDC65", "\uD83E\uDEC2", "\uD83D\uDC76", "\uD83E\uDDD2", "\uD83D\uDC66", "\uD83D\uDC67", "\uD83E\uDDD1",
            "\uD83D\uDC68", "\uD83D\uDC69", "\uD83E\uDDD3", "\uD83D\uDC74", "\uD83D\uDC75", "\uD83D\uDE4B", "\uD83D\uDE4E", "\uD83D\uDE45",
        ),
    ),
    EmojiGroup(
        Glyph.PALETTE, "Hearts",
        listOf(
            "\uD83D\uDC8B", "\uD83D\uDC8C", "\uD83D\uDC98", "\uD83D\uDC9D", "\uD83D\uDC96", "\uD83D\uDC97", "\uD83D\uDC93", "\uD83D\uDC9E",
            "\uD83D\uDC95", "\uD83D\uDC9F", "\uD83D\uDC94", "\uD83E\uDDE1", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99", "\uD83D\uDC9C",
            "\uD83E\uDD0E", "\uD83D\uDDA4", "\uD83E\uDD0D", "\uD83D\uDCAF", "\uD83D\uDCA2", "\uD83D\uDCA5", "\uD83D\uDCAB", "\uD83D\uDCA6",
            "\uD83D\uDCA8", "\uD83D\uDCAC", "\uD83D\uDCAD", "\uD83D\uDCA4", "\u2728", "\uD83C\uDF1F", "\uD83C\uDF20", "\uD83D\uDD25",
        ),
    ),
    EmojiGroup(
        Glyph.GLOBE, "Animals",
        listOf(
            "\uD83D\uDC35", "\uD83D\uDC12", "\uD83E\uDD8D", "\uD83E\uDDA7", "\uD83D\uDC36", "\uD83D\uDC15", "\uD83E\uDDAE", "\uD83D\uDC29",
            "\uD83D\uDC3A", "\uD83E\uDD8A", "\uD83E\uDD9D", "\uD83D\uDC31", "\uD83D\uDC08", "\uD83E\uDD81", "\uD83D\uDC2F", "\uD83D\uDC05",
            "\uD83D\uDC06", "\uD83D\uDC34", "\uD83D\uDC0E", "\uD83E\uDD84", "\uD83E\uDD93", "\uD83E\uDD8C", "\uD83D\uDC2E", "\uD83D\uDC02",
            "\uD83D\uDC37", "\uD83D\uDC16", "\uD83D\uDC17", "\uD83D\uDC3D", "\uD83D\uDC0F", "\uD83D\uDC11", "\uD83D\uDC10", "\uD83D\uDC2A",
            "\uD83D\uDC2B", "\uD83E\uDD99", "\uD83E\uDD92", "\uD83D\uDC18", "\uD83E\uDD8F", "\uD83E\uDD9B", "\uD83D\uDC2D", "\uD83D\uDC01",
            "\uD83D\uDC39", "\uD83D\uDC30", "\uD83D\uDC07", "\uD83E\uDD94", "\uD83E\uDD87", "\uD83D\uDC3B", "\uD83D\uDC28", "\uD83D\uDC3C",
            "\uD83E\uDDA5", "\uD83E\uDDA6", "\uD83E\uDDA8", "\uD83E\uDD98", "\uD83E\uDDA1", "\uD83D\uDC3E", "\uD83D\uDC14", "\uD83D\uDC13",
            "\uD83D\uDC23", "\uD83D\uDC24", "\uD83D\uDC26", "\uD83D\uDC27", "\uD83E\uDD85", "\uD83E\uDD86", "\uD83E\uDD89", "\uD83E\uDD9A",
            "\uD83D\uDC38", "\uD83D\uDC0A", "\uD83D\uDC22", "\uD83E\uDD8E", "\uD83D\uDC0D", "\uD83D\uDC32", "\uD83E\uDD95", "\uD83E\uDD96",
            "\uD83D\uDC33", "\uD83D\uDC0B", "\uD83D\uDC2C", "\uD83D\uDC1F", "\uD83D\uDC20", "\uD83E\uDD88", "\uD83D\uDC19", "\uD83E\uDD80",
        ),
    ),
    EmojiGroup(
        Glyph.KEYBOARD_TEXT, "Food",
        listOf(
            "\uD83C\uDF47", "\uD83C\uDF48", "\uD83C\uDF49", "\uD83C\uDF4A", "\uD83C\uDF4B", "\uD83C\uDF4C", "\uD83C\uDF4D", "\uD83E\uDD6D",
            "\uD83C\uDF4E", "\uD83C\uDF4F", "\uD83C\uDF50", "\uD83C\uDF51", "\uD83C\uDF52", "\uD83C\uDF53", "\uD83E\uDD5D", "\uD83C\uDF45",
            "\uD83E\uDD65", "\uD83E\uDD51", "\uD83E\uDD54", "\uD83E\uDD55", "\uD83C\uDF3D", "\uD83E\uDD52", "\uD83E\uDD6C", "\uD83E\uDD66",
            "\uD83C\uDF5E", "\uD83E\uDD50", "\uD83E\uDD68", "\uD83E\uDD6F", "\uD83E\uDD5E", "\uD83E\uDDC7", "\uD83E\uDDC0", "\uD83C\uDF56",
            "\uD83C\uDF57", "\uD83E\uDD69", "\uD83E\uDD53", "\uD83C\uDF54", "\uD83C\uDF5F", "\uD83C\uDF55", "\uD83C\uDF2D", "\uD83E\uDD6A",
            "\uD83C\uDF2E", "\uD83C\uDF2F", "\uD83E\uDD59", "\uD83E\uDDC6", "\uD83E\uDD5A", "\uD83C\uDF73", "\uD83C\uDF72", "\uD83E\uDD63",
            "\uD83C\uDF5C", "\uD83C\uDF5D", "\uD83C\uDF5B", "\uD83C\uDF63", "\uD83C\uDF71", "\uD83E\uDD5F", "\uD83C\uDF64", "\uD83C\uDF59",
            "\uD83C\uDF70", "\uD83C\uDF82", "\uD83E\uDDC1", "\uD83E\uDD67", "\uD83C\uDF6B", "\uD83C\uDF6C", "\uD83C\uDF6D", "\uD83C\uDF6E",
            "\uD83C\uDF6F", "\uD83C\uDF7C", "\uD83E\uDD5B", "\uD83C\uDF75", "\uD83C\uDF76", "\uD83C\uDF7A", "\uD83C\uDF7B", "\uD83E\uDD42",
        ),
    ),
    EmojiGroup(
        Glyph.TRANSLATE, "Activity",
        listOf(
            "\uD83C\uDF83", "\uD83C\uDF84", "\uD83C\uDF86", "\uD83C\uDF87", "\uD83E\uDDE8", "\uD83C\uDF88", "\uD83C\uDF89", "\uD83C\uDF8A",
            "\uD83C\uDF8B", "\uD83C\uDF8D", "\uD83C\uDF8E", "\uD83C\uDF8F", "\uD83C\uDF90", "\uD83C\uDF91", "\uD83E\uDDE7", "\uD83C\uDF80",
            "\uD83C\uDF81", "\uD83C\uDF96", "\uD83C\uDF97", "\uD83C\uDFAB", "\uD83C\uDFC6", "\uD83C\uDFC5", "\uD83E\uDD47", "\uD83E\uDD48",
            "\uD83E\uDD49", "\uD83D\uDC7E", "\uD83C\uDFAF", "\uD83C\uDFB2", "\uD83E\uDDE9", "\uD83C\uDFB3", "\uD83C\uDFAE", "\uD83C\uDFB0",
            "\uD83C\uDFBD", "\uD83C\uDFBF", "\uD83C\uDFC0", "\uD83C\uDFD0", "\uD83C\uDFC8", "\uD83C\uDFC9", "\uD83C\uDFBE", "\uD83E\uDD4F",
            "\uD83C\uDFB1", "\uD83C\uDFD3", "\uD83C\uDFF8", "\uD83C\uDFD2", "\uD83C\uDFD1", "\uD83E\uDD4D", "\uD83C\uDFA3", "\uD83E\uDD4A",
        ),
    ),
    EmojiGroup(
        Glyph.CHEVRON_RIGHT, "Travel",
        listOf(
            "\uD83C\uDF0D", "\uD83C\uDF0E", "\uD83C\uDF0F", "\uD83C\uDF0B", "\uD83C\uDFD4", "\uD83C\uDFD5", "\uD83C\uDFD6", "\uD83C\uDFDC",
            "\uD83C\uDFDD", "\uD83C\uDFDE", "\uD83C\uDFDF", "\uD83C\uDFDB", "\uD83C\uDFD7", "\uD83C\uDFD8", "\uD83C\uDFDA", "\uD83C\uDFE0",
            "\uD83C\uDFE1", "\uD83C\uDFE2", "\uD83C\uDFE3", "\uD83C\uDFE4", "\uD83C\uDFE5", "\uD83C\uDFE6", "\uD83C\uDFE8", "\uD83C\uDFE9",
            "\uD83D\uDC92", "\uD83C\uDFEA", "\uD83C\uDFEB", "\uD83C\uDFED", "\uD83C\uDFEF", "\uD83C\uDFF0", "\uD83D\uDDFC", "\uD83D\uDDFD",
            "\uD83D\uDE97", "\uD83D\uDE95", "\uD83D\uDE8C", "\uD83D\uDE8E", "\uD83D\uDE93", "\uD83D\uDE91", "\uD83D\uDE92", "\uD83D\uDE9B",
            "\uD83D\uDEF5", "\uD83D\uDE82", "\uD83D\uDE84", "\uD83D\uDE85", "\uD83D\uDE86", "\uD83D\uDE87", "\uD83D\uDE88", "\uD83D\uDE89",
            "\uD83D\uDEEB", "\uD83D\uDEEC", "\uD83D\uDE80", "\uD83D\uDEF8", "\uD83D\uDEA2", "\uD83D\uDEF6", "\uD83D\uDEA4", "\uD83D\uDEF3",
        ),
    ),
    EmojiGroup(
        Glyph.PASTE, "Objects",
        listOf(
            "\uD83D\uDCF1", "\uD83D\uDCBB", "\uD83D\uDDA5", "\uD83D\uDDA8", "\uD83D\uDCF7", "\uD83D\uDCF8", "\uD83D\uDCF9", "\uD83D\uDCFA",
            "\uD83D\uDCFB", "\uD83C\uDFA4", "\uD83C\uDFA7", "\uD83C\uDFB5", "\uD83C\uDFB6", "\uD83C\uDFB9", "\uD83C\uDFB8", "\uD83C\uDFBB",
            "\uD83D\uDC8E", "\uD83D\uDC8D", "\uD83D\uDC51", "\uD83D\uDC53", "\uD83D\uDC55", "\uD83D\uDC56", "\uD83D\uDC5F", "\uD83E\uDDE2",
            "\uD83D\uDCD6", "\uD83D\uDCDA", "\uD83D\uDCDD", "\uD83D\uDCBC", "\uD83D\uDCC1", "\uD83D\uDCC5", "\uD83D\uDCCC", "\uD83D\uDCCE",
            "\uD83D\uDD11", "\uD83D\uDD12", "\uD83D\uDD13", "\uD83D\uDD28", "\uD83D\uDD27", "\uD83D\uDD29", "\uD83D\uDCA1", "\uD83D\uDD26",
            "\uD83D\uDCB0", "\uD83D\uDCB3", "\uD83D\uDCB8", "\uD83E\uDDFE", "\uD83D\uDD14", "\uD83D\uDD15", "\uD83C\uDFA8", "\uD83C\uDFAD",
        ),
    ),
    EmojiGroup(
        Glyph.CHECK, "Symbols",
        listOf(
            "\u2705", "\u274C", "\u2757", "\u2753", "\u2755", "\u2795", "\u2796", "\u27B0",
            "\uD83D\uDD34", "\uD83D\uDFE0", "\uD83D\uDFE1", "\uD83D\uDFE2", "\uD83D\uDD35", "\uD83D\uDFE3", "\uD83D\uDFE4", "\u26AB",
            "\u26AA", "\uD83D\uDD3A", "\uD83D\uDD3B", "\uD83D\uDD36", "\uD83D\uDD37", "\uD83D\uDD38", "\uD83D\uDD39", "\uD83D\uDD33",
            "\uD83D\uDD32", "\uD83D\uDD1F", "\uD83D\uDD20", "\uD83D\uDD21", "\uD83D\uDD22", "\uD83D\uDD23", "\uD83D\uDD24", "\uD83C\uDD70",
            "\uD83C\uDD91", "\uD83C\uDD92", "\uD83C\uDD93", "\uD83C\uDD95", "\uD83C\uDD96", "\uD83C\uDD97", "\uD83C\uDD98", "\uD83C\uDD99",
            "\uD83D\uDD1D", "\uD83D\uDD19", "\uD83D\uDD1A", "\uD83D\uDD1B", "\uD83D\uDD1C", "\uD83D\uDD00", "\uD83D\uDD01", "\uD83D\uDD02",
        ),
    ),
)
