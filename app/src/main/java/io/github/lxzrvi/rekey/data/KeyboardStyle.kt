package io.github.lxzrvi.rekey.data

import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/**
 * Everything about a layout that is not its base palette.
 *
 * Each section maps to one page of the editor wizard, so "reset this page" is
 * simply "replace this section with its default". Colours are nullable: null
 * means "inherit from the palette", which is what keeps a preset coherent when
 * the owner changes the base theme after styling individual parts.
 */
data class KeyboardStyle(
    val background: BackgroundStyle = BackgroundStyle(),
    val layout: LayoutStyle = LayoutStyle(),
    val keys: KeyStyle = KeyStyle(),
    val feel: FeelStyle = FeelStyle(),
    val suggestions: SuggestionStyle = SuggestionStyle(),
    val arrows: ArrowStyle = ArrowStyle(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("background", background.toJson())
        .put("layout", layout.toJson())
        .put("keys", keys.toJson())
        .put("feel", feel.toJson())
        .put("suggestions", suggestions.toJson())
        .put("arrows", arrows.toJson())

    companion object {
        fun fromJson(o: JSONObject?): KeyboardStyle {
            if (o == null) return KeyboardStyle()
            return KeyboardStyle(
                background = BackgroundStyle.fromJson(o.optJSONObject("background")),
                layout = LayoutStyle.fromJson(o.optJSONObject("layout")),
                keys = KeyStyle.fromJson(o.optJSONObject("keys")),
                feel = FeelStyle.fromJson(o.optJSONObject("feel")),
                suggestions = SuggestionStyle.fromJson(o.optJSONObject("suggestions")),
                arrows = ArrowStyle.fromJson(o.optJSONObject("arrows")),
            )
        }
    }
}

// ---- colour helpers: null means "inherit" ------------------------------------

internal fun JSONObject.putColor(key: String, c: Color?): JSONObject {
    if (c != null) put(key, c.value.toLong())
    return this
}

internal fun JSONObject.color(key: String): Color? =
    if (has(key) && !isNull(key)) Color(getLong(key).toULong()) else null

internal fun JSONObject.f(key: String, def: Float): Float =
    runCatching { getDouble(key).toFloat() }.getOrDefault(def)

/** Page 1 — what sits behind the keys. */
data class BackgroundStyle(
    val useImage: Boolean = false,
    val imageUri: String? = null,
    /** null falls back to the palette canvas. */
    val color: Color? = null,
    /** null means a flat fill rather than a gradient. */
    val gradient: Color? = null,
    val gradientAngle: Float = 90f,
    val blurDp: Float = 0f,
    val dim: Float = 0f,
    val imageScale: Float = 1f,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("useImage", useImage)
        .put("imageUri", imageUri ?: JSONObject.NULL)
        .putColor("color", color)
        .putColor("gradient", gradient)
        .put("gradientAngle", gradientAngle.toDouble())
        .put("blurDp", blurDp.toDouble())
        .put("dim", dim.toDouble())
        .put("imageScale", imageScale.toDouble())

    companion object {
        fun fromJson(o: JSONObject?): BackgroundStyle {
            val d = BackgroundStyle()
            if (o == null) return d
            return BackgroundStyle(
                useImage = o.optBoolean("useImage", d.useImage),
                imageUri = if (o.isNull("imageUri")) null else o.optString("imageUri", "").ifBlank { null },
                color = o.color("color"),
                gradient = o.color("gradient"),
                gradientAngle = o.f("gradientAngle", d.gradientAngle),
                blurDp = o.f("blurDp", d.blurDp),
                dim = o.f("dim", d.dim),
                imageScale = o.f("imageScale", d.imageScale),
            )
        }
    }
}

/** Page 2 — the size and spacing of the whole board. */
data class LayoutStyle(
    val heightScale: Float = 1f,
    val widthScale: Float = 1f,
    val padHorizontal: Float = 7f,
    val padTop: Float = 10f,
    val padBottom: Float = 11f,
    val rowGap: Float = 8f,
    val keyGap: Float = 5f,
    val cornerTopLeft: Float = 18f,
    val cornerTopRight: Float = 18f,
    val cornerBottomLeft: Float = 0f,
    val cornerBottomRight: Float = 0f,
    /** One slider drives all four corners while this is on. */
    val cornersLinked: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("cornersLinked", cornersLinked)
        .put("heightScale", heightScale.toDouble())
        .put("widthScale", widthScale.toDouble())
        .put("padHorizontal", padHorizontal.toDouble())
        .put("padTop", padTop.toDouble())
        .put("padBottom", padBottom.toDouble())
        .put("rowGap", rowGap.toDouble())
        .put("keyGap", keyGap.toDouble())
        .put("cornerTopLeft", cornerTopLeft.toDouble())
        .put("cornerTopRight", cornerTopRight.toDouble())
        .put("cornerBottomLeft", cornerBottomLeft.toDouble())
        .put("cornerBottomRight", cornerBottomRight.toDouble())

    companion object {
        fun fromJson(o: JSONObject?): LayoutStyle {
            val d = LayoutStyle()
            if (o == null) return d
            return LayoutStyle(
                heightScale = o.f("heightScale", d.heightScale),
                widthScale = o.f("widthScale", d.widthScale),
                padHorizontal = o.f("padHorizontal", d.padHorizontal),
                padTop = o.f("padTop", d.padTop),
                padBottom = o.f("padBottom", d.padBottom),
                rowGap = o.f("rowGap", d.rowGap),
                keyGap = o.f("keyGap", d.keyGap),
                cornerTopLeft = o.f("cornerTopLeft", d.cornerTopLeft),
                cornerTopRight = o.f("cornerTopRight", d.cornerTopRight),
                cornerBottomLeft = o.f("cornerBottomLeft", d.cornerBottomLeft),
                cornerBottomRight = o.f("cornerBottomRight", d.cornerBottomRight),
                cornersLinked = o.optBoolean("cornersLinked", d.cornersLinked),
            )
        }
    }
}

/** Page 3 — the keys themselves, including the special ones. */
data class KeyStyle(
    val radius: Float = 8f,
    val textSize: Float = 19f,
    val textColor: Color? = null,
    val keyColor: Color? = null,
    val specialColor: Color? = null,
    val shiftColor: Color? = null,
    val numberColor: Color? = null,
    val emojiColor: Color? = null,
    val enterColor: Color? = null,
    val enterTextColor: Color? = null,
    val borderWidth: Float = 1f,
    val borderColor: Color? = null,
    /** One slider drives all four key corners while this is on. */
    val cornersLinked: Boolean = true,
    val cornerTopLeft: Float = 8f,
    val cornerTopRight: Float = 8f,
    val cornerBottomLeft: Float = 8f,
    val cornerBottomRight: Float = 8f,
    /** A photo shown inside every letter key. */
    val imageUri: String? = null,
    val imageAlpha: Float = 1f,
    /** Per-key colour / image / sound / radius, keyed by the key's label. */
    val overrides: Map<String, KeyOverride> = emptyMap(),
) {
    /** Effective corner shape, honouring the link toggle. */
    fun shapeRadii(): FloatArray =
        if (cornersLinked) floatArrayOf(radius, radius, radius, radius)
        else floatArrayOf(cornerTopLeft, cornerTopRight, cornerBottomRight, cornerBottomLeft)

    fun toJson(): JSONObject = JSONObject()
        .put("cornersLinked", cornersLinked)
        .put("cornerTopLeft", cornerTopLeft.toDouble())
        .put("cornerTopRight", cornerTopRight.toDouble())
        .put("cornerBottomLeft", cornerBottomLeft.toDouble())
        .put("cornerBottomRight", cornerBottomRight.toDouble())
        .put("imageUri", imageUri ?: JSONObject.NULL)
        .put("imageAlpha", imageAlpha.toDouble())
        .put("overrides", JSONObject().apply { overrides.forEach { (k, v) -> put(k, v.toJson()) } })
        .put("radius", radius.toDouble())
        .put("textSize", textSize.toDouble())
        .putColor("textColor", textColor)
        .putColor("keyColor", keyColor)
        .putColor("specialColor", specialColor)
        .putColor("shiftColor", shiftColor)
        .putColor("numberColor", numberColor)
        .putColor("emojiColor", emojiColor)
        .putColor("enterColor", enterColor)
        .putColor("enterTextColor", enterTextColor)
        .put("borderWidth", borderWidth.toDouble())
        .putColor("borderColor", borderColor)

    companion object {
        fun fromJson(o: JSONObject?): KeyStyle {
            val d = KeyStyle()
            if (o == null) return d
            return KeyStyle(
                radius = o.f("radius", d.radius),
                textSize = o.f("textSize", d.textSize),
                textColor = o.color("textColor"),
                keyColor = o.color("keyColor"),
                specialColor = o.color("specialColor"),
                shiftColor = o.color("shiftColor"),
                numberColor = o.color("numberColor"),
                emojiColor = o.color("emojiColor"),
                enterColor = o.color("enterColor"),
                enterTextColor = o.color("enterTextColor"),
                borderWidth = o.f("borderWidth", d.borderWidth),
                borderColor = o.color("borderColor"),
                cornersLinked = o.optBoolean("cornersLinked", d.cornersLinked),
                cornerTopLeft = o.f("cornerTopLeft", d.radius),
                cornerTopRight = o.f("cornerTopRight", d.radius),
                cornerBottomLeft = o.f("cornerBottomLeft", d.radius),
                cornerBottomRight = o.f("cornerBottomRight", d.radius),
                imageUri = if (o.isNull("imageUri")) null else o.optString("imageUri", "").ifBlank { null },
                imageAlpha = o.f("imageAlpha", d.imageAlpha),
                overrides = o.optJSONObject("overrides")?.let { obj ->
                    buildMap {
                        obj.keys().forEach { k -> put(k, KeyOverride.fromJson(obj.optJSONObject(k))) }
                    }
                } ?: emptyMap(),
            )
        }
    }
}

/**
 * One key's own look, overriding the shared key style.
 *
 * Null means "inherit", so an override only ever stores what the owner actually
 * changed. Sound is an index into [KeySounds]; -1 means inherit.
 */
data class KeyOverride(
    val color: Color? = null,
    val textColor: Color? = null,
    val imageUri: String? = null,
    val imageAlpha: Float = 1f,
    val soundIndex: Int = -1,
    val radius: Float = -1f,
) {
    val isEmpty: Boolean
        get() = color == null && textColor == null && imageUri == null &&
            soundIndex < 0 && radius < 0f

    fun toJson(): JSONObject = JSONObject()
        .putColor("color", color)
        .putColor("textColor", textColor)
        .put("imageUri", imageUri ?: JSONObject.NULL)
        .put("imageAlpha", imageAlpha.toDouble())
        .put("soundIndex", soundIndex)
        .put("radius", radius.toDouble())

    companion object {
        fun fromJson(o: JSONObject?): KeyOverride {
            val d = KeyOverride()
            if (o == null) return d
            return KeyOverride(
                color = o.color("color"),
                textColor = o.color("textColor"),
                imageUri = if (o.isNull("imageUri")) null else o.optString("imageUri", "").ifBlank { null },
                imageAlpha = o.f("imageAlpha", d.imageAlpha),
                soundIndex = o.optInt("soundIndex", d.soundIndex),
                radius = o.f("radius", d.radius),
            )
        }
    }
}

/** The ten shipped key sounds, in the order the picker shows them. */
object KeySounds {
    val labels: List<String> = listOf(
        "Soft", "Mechanical", "Typewriter", "Bubble", "Glass",
        "Wood", "Click", "Pop", "Tactile", "Muted",
    )

    /** Resource names under res/raw; resolved by name so R ids stay out of the model. */
    val resources: List<String> = listOf(
        "key_soft", "key_mechanical", "key_typewriter", "key_bubble", "key_glass",
        "key_wood", "key_click", "key_pop", "key_tactile", "key_muted",
    )

    fun resource(index: Int): String = resources[index.coerceIn(0, resources.lastIndex)]
}

/** Page 4 — sound and vibration. */
data class FeelStyle(
    val soundOn: Boolean = false,
    val soundIndex: Int = 0,
    val soundVolume: Float = 0.6f,
    val hapticOn: Boolean = true,
    /** 1..255, fed to VibrationEffect as an amplitude. */
    val hapticStrength: Int = 110,
    /** Milliseconds. */
    val hapticDuration: Int = 18,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("soundOn", soundOn)
        .put("soundIndex", soundIndex)
        .put("soundVolume", soundVolume.toDouble())
        .put("hapticOn", hapticOn)
        .put("hapticStrength", hapticStrength)
        .put("hapticDuration", hapticDuration)

    companion object {
        fun fromJson(o: JSONObject?): FeelStyle {
            val d = FeelStyle()
            if (o == null) return d
            return FeelStyle(
                soundOn = o.optBoolean("soundOn", d.soundOn),
                soundIndex = o.optInt("soundIndex", d.soundIndex),
                soundVolume = o.f("soundVolume", d.soundVolume),
                hapticOn = o.optBoolean("hapticOn", d.hapticOn),
                hapticStrength = o.optInt("hapticStrength", d.hapticStrength),
                hapticDuration = o.optInt("hapticDuration", d.hapticDuration),
            )
        }
    }
}

/** Page 5 — the suggestion strip. */
data class SuggestionStyle(
    val enabled: Boolean = true,
    val columns: Int = 3,
    val height: Float = 34f,
    val radius: Float = 8f,
    val color: Color? = null,
    val textColor: Color? = null,
    val textSize: Float = 12f,
    /** The grab handle above the strip: swipe down closes, double tap floats. */
    val grabberEnabled: Boolean = true,
    val grabberWidth: Float = 46f,
    val grabberColor: Color? = null,
    /** The expanding action pill on the right of the strip. */
    val pillColor: Color? = null,
    val pillIconColor: Color? = null,
    val pillRadius: Float = 30f,
    val pillPaste: Boolean = true,
    val pillTranslate: Boolean = true,
    val pillTheme: Boolean = true,
    val pillLanguage: Boolean = true,
    val pillVoice: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("enabled", enabled)
        .put("columns", columns)
        .put("height", height.toDouble())
        .put("radius", radius.toDouble())
        .putColor("color", color)
        .putColor("textColor", textColor)
        .put("textSize", textSize.toDouble())
        .put("grabberEnabled", grabberEnabled)
        .put("grabberWidth", grabberWidth.toDouble())
        .putColor("grabberColor", grabberColor)
        .putColor("pillColor", pillColor)
        .putColor("pillIconColor", pillIconColor)
        .put("pillRadius", pillRadius.toDouble())
        .put("pillPaste", pillPaste)
        .put("pillTranslate", pillTranslate)
        .put("pillTheme", pillTheme)
        .put("pillLanguage", pillLanguage)
        .put("pillVoice", pillVoice)

    companion object {
        fun fromJson(o: JSONObject?): SuggestionStyle {
            val d = SuggestionStyle()
            if (o == null) return d
            return SuggestionStyle(
                enabled = o.optBoolean("enabled", d.enabled),
                columns = o.optInt("columns", d.columns).coerceIn(3, 5),
                height = o.f("height", d.height),
                radius = o.f("radius", d.radius),
                color = o.color("color"),
                textColor = o.color("textColor"),
                textSize = o.f("textSize", d.textSize),
                grabberEnabled = o.optBoolean("grabberEnabled", d.grabberEnabled),
                grabberWidth = o.f("grabberWidth", d.grabberWidth),
                grabberColor = o.color("grabberColor"),
                pillColor = o.color("pillColor"),
                pillIconColor = o.color("pillIconColor"),
                pillRadius = o.f("pillRadius", d.pillRadius),
                pillPaste = o.optBoolean("pillPaste", d.pillPaste),
                pillTranslate = o.optBoolean("pillTranslate", d.pillTranslate),
                pillTheme = o.optBoolean("pillTheme", d.pillTheme),
                pillLanguage = o.optBoolean("pillLanguage", d.pillLanguage),
                pillVoice = o.optBoolean("pillVoice", d.pillVoice),
            )
        }
    }
}

/** Page 6 — the editing / arrow bar shown above the suggestions. */
data class ArrowStyle(
    val enabled: Boolean = false,
    val height: Float = 38f,
    val radius: Float = 9f,
    val color: Color? = null,
    val iconColor: Color? = null,
    val selectAll: Boolean = true,
    val copy: Boolean = true,
    val cut: Boolean = true,
    val paste: Boolean = true,
    val homeEnd: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("enabled", enabled)
        .put("height", height.toDouble())
        .put("radius", radius.toDouble())
        .putColor("color", color)
        .putColor("iconColor", iconColor)
        .put("selectAll", selectAll)
        .put("copy", copy)
        .put("cut", cut)
        .put("paste", paste)
        .put("homeEnd", homeEnd)
        .put("bold", bold)
        .put("italic", italic)
        .put("underline", underline)

    companion object {
        fun fromJson(o: JSONObject?): ArrowStyle {
            val d = ArrowStyle()
            if (o == null) return d
            return ArrowStyle(
                enabled = o.optBoolean("enabled", d.enabled),
                height = o.f("height", d.height),
                radius = o.f("radius", d.radius),
                color = o.color("color"),
                iconColor = o.color("iconColor"),
                selectAll = o.optBoolean("selectAll", d.selectAll),
                copy = o.optBoolean("copy", d.copy),
                cut = o.optBoolean("cut", d.cut),
                paste = o.optBoolean("paste", d.paste),
                homeEnd = o.optBoolean("homeEnd", d.homeEnd),
                bold = o.optBoolean("bold", d.bold),
                italic = o.optBoolean("italic", d.italic),
                underline = o.optBoolean("underline", d.underline),
            )
        }
    }
}
