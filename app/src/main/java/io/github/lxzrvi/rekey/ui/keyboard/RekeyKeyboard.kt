package io.github.lxzrvi.rekey.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import io.github.lxzrvi.rekey.data.ArrowStyle
import io.github.lxzrvi.rekey.data.BackgroundStyle
import io.github.lxzrvi.rekey.data.KeyboardPalette
import io.github.lxzrvi.rekey.data.KeyboardStyle
import io.github.lxzrvi.rekey.data.SuggestionStyle
import io.github.lxzrvi.rekey.ui.brand.BrandTypography
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * The REKEY keyboard.
 *
 * Authored on a 440 dp canvas so a preset card and the real input method draw
 * exactly the same geometry. Everything the editor wizard can change arrives in
 * [KeyboardStyle]; the defaults reproduce the original board pixel for pixel.
 */
object KeyboardCanvas {
    const val WIDTH_VALUE = 440f
    val WIDTH: Dp = WIDTH_VALUE.dp

    /** Toolbar strip + four key rows + padding, at default style. */
    val HEIGHT: Dp = 265.dp
    val CARD_HEIGHT: Dp = 265.dp

    const val RATIO = 440f / 265f
    const val CARD_RATIO = 440f / 265f

    const val OUTER = 7f
}

private val Outer = KeyboardCanvas.OUTER.dp
val ImeToolbarHeight: Dp = 40.dp

private val Letters1 = "qwertyuiop".map { it.toString() }
private val Letters2 = "asdfghjkl".map { it.toString() }
private val Letters3 = "zxcvbnm".map { it.toString() }
private val Symbols1 = "1234567890".map { it.toString() }
private val Symbols2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
private val Symbols3 = listOf("*", "\"", "'", ":", ";", "!", "?")


/** Cards are previews, so their suggestion strip shows fixed specimen words. */
val KeyboardPreviewSuggestions: List<String> = listOf("Hello", "Design", "Today", "Great", "Thanks")

/** What the input method needs to know about a tap. */
enum class KeyAction {
    TEXT, SPACE, ENTER, BACKSPACE, LANGUAGE, PASTE, CYCLE_THEME,
    LEFT, RIGHT, UP, DOWN, SELECT_ALL, COPY, CUT, HOME, END,
    BOLD, ITALIC, UNDERLINE, HIDE, TOGGLE_FLOAT, SUGGEST,
}

private enum class ShiftState { OFF, ON, CAPS }

/** Everything a key needs, bundled so the row helpers stay short. */
private class Ui(
    val p: KeyboardPalette,
    val st: KeyboardStyle,
    val keyH: Dp,
    val fs: Float,
    val interactive: Boolean,
    val haptic: () -> Unit,
) {
    val keyShape: Shape get() = RoundedCornerShape(st.keys.radius.dp)
    val textColor: Color get() = st.keys.textColor ?: p.text
    val keyBg: Color get() = st.keys.keyColor ?: p.key
    val specialBg: Color get() = st.keys.specialColor ?: p.special
    val borderW: Dp get() = st.keys.borderWidth.dp
    val borderCol: Color?
        get() = if (st.keys.borderWidth <= 0f) null else (st.keys.borderColor ?: p.border)
}

@Composable
fun RekeyKeyboard(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    style: KeyboardStyle = KeyboardStyle(),
    backgroundImage: ImageBitmap? = null,
    showToolbar: Boolean = true,
    bottomInsetPx: Int = 0,
    leftInsetPx: Int = 0,
    rightInsetPx: Int = 0,
    interactive: Boolean = true,
    systemHaptic: Boolean = true,
    floating: Boolean = false,
    onFloatingDrag: (Float) -> Unit = {},
    suggestions: List<String> = emptyList(),
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
) {
    var open by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(ShiftState.OFF) }
    var lastShiftTap by remember { mutableLongStateOf(0L) }
    var symbols by remember { mutableStateOf(false) }

    val view = LocalView.current
    val haptic: () -> Unit = remember(view, interactive, systemHaptic) {
        {
            if (interactive && systemHaptic) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    // Theme colours cross-fade in the IME; a static card just uses them directly.
    val p = if (interactive) animatedPalette(palette) else palette

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        val lay = style.layout
        val kbWidth = minOf(maxWidth, KeyboardCanvas.WIDTH) * lay.widthScale
        val compact = kbWidth < 350.dp
        val gap = if (compact) (lay.keyGap * 0.6f).dp else lay.keyGap.dp
        val rowGap = if (compact) (lay.rowGap * 0.875f).dp else lay.rowGap.dp
        val padStart0 = lay.padHorizontal.dp
        val keyH = (if (compact) 41f else 43f) * lay.heightScale
        val ui = Ui(
            p = p,
            st = style,
            keyH = keyH.dp,
            fs = if (compact) style.keys.textSize * 0.895f else style.keys.textSize,
            interactive = interactive,
            haptic = haptic,
        )

        // Unexpected-Keyboard's rule: the bottom inset is ADDITIVE so the keys
        // clear the navigation bar, while the side insets only widen the gutter
        // when the device is cut out further than the design margin.
        val density = LocalDensity.current
        val navBottom = with(density) { bottomInsetPx.toDp() }
        val padStart = maxOf(padStart0, with(density) { leftInsetPx.toDp() })
        val padEnd = maxOf(padStart0, with(density) { rightInsetPx.toDp() })
        val inner = kbWidth - padStart - padEnd

        fun emit(action: KeyAction, text: String = "") {
            if (!interactive) return
            onKey(action, text)
        }

        fun label(c: String) = if (!symbols && shift != ShiftState.OFF) c.uppercase() else c

        fun onChar(c: String) {
            if (!interactive) return
            onKey(KeyAction.TEXT, label(c))
            if (shift == ShiftState.ON) shift = ShiftState.OFF
        }

        val boardShape = RoundedCornerShape(
            topStart = lay.cornerTopLeft.dp,
            topEnd = lay.cornerTopRight.dp,
            bottomStart = lay.cornerBottomLeft.dp,
            bottomEnd = lay.cornerBottomRight.dp,
        )

        Box(Modifier.width(kbWidth).clip(boardShape)) {
            KeyboardBackground(Modifier.matchParentSize(), style.background, p.canvas, backgroundImage)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = padStart,
                        end = padEnd,
                        top = lay.padTop.dp,
                        bottom = lay.padBottom.dp + navBottom,
                    ),
            ) {
                if (style.suggestions.grabberEnabled) {
                    Grabber(
                        spec = style.suggestions,
                        ui = ui,
                        floating = floating,
                        onHide = { emit(KeyAction.HIDE) },
                        onFloat = { emit(KeyAction.TOGGLE_FLOAT) },
                        onDrag = onFloatingDrag,
                    )
                }
                if (style.arrows.enabled) {
                    ArrowBar(style.arrows, ui, gap) { emit(it) }
                    Spacer(Modifier.height(rowGap))
                }
                if (showToolbar) {
                    KeyboardToolbar(
                        p = p,
                        ui = ui,
                        spec = style.suggestions,
                        inner = inner,
                        gap = gap,
                        compact = compact,
                        open = open,
                        suggestions = suggestions,
                        onToggle = { open = !open },
                        onSuggestion = { emit(KeyAction.SUGGEST, it) },
                        onPaste = { if (open) emit(KeyAction.PASTE) },
                        onTheme = { if (open) emit(KeyAction.CYCLE_THEME) },
                        onLanguage = { if (open) emit(KeyAction.LANGUAGE) },
                    )
                    Spacer(Modifier.height(rowGap))
                }

                if (emoji) {
                    EmojiPanel(ui, p, gap, rowGap, { emit(KeyAction.TEXT, it) }, { emit(KeyAction.BACKSPACE) })
                } else {
                    /* ROW 1 */
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        (if (symbols) Symbols1 else Letters1).forEach { c ->
                            CharKey(ui, label(c)) { onChar(c) }
                        }
                    }
                    Spacer(Modifier.height(rowGap))

                    /* ROW 2 — inset 5% of the inner width */
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = inner * 0.05f),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        (if (symbols) Symbols2 else Letters2).forEach { c ->
                            CharKey(ui, label(c)) { onChar(c) }
                        }
                    }
                    Spacer(Modifier.height(rowGap))

                    /* ROW 3 */
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        if (symbols) {
                            SpecialKey(ui, 1.45f, bg = ui.st.keys.numberColor, onPress = { symbols = false }) {
                                SpecialText(ui, "=<")
                            }
                        } else {
                            SpecialKey(ui, 1.45f, bg = ui.st.keys.shiftColor, onPress = {
                                if (!interactive) return@SpecialKey
                                haptic()
                                val now = System.currentTimeMillis()
                                shift = when {
                                    shift == ShiftState.CAPS -> ShiftState.OFF
                                    shift == ShiftState.ON && now - lastShiftTap < 300 -> ShiftState.CAPS
                                    shift == ShiftState.ON -> ShiftState.OFF
                                    else -> ShiftState.ON
                                }
                                lastShiftTap = now
                            }) {
                                RekeyGlyph(
                                    glyph = when (shift) {
                                        ShiftState.OFF -> Glyph.SHIFT
                                        ShiftState.ON -> Glyph.CAPS_ON
                                        ShiftState.CAPS -> Glyph.CAPS_LOCK
                                    },
                                    color = if (shift == ShiftState.OFF) ui.textColor else p.enter,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                        (if (symbols) Symbols3 else Letters3).forEach { c ->
                            CharKey(ui, label(c)) { onChar(c) }
                        }
                        SpecialKey(ui, 1.45f, repeat = true, onPress = { emit(KeyAction.BACKSPACE) }) {
                            RekeyGlyph(Glyph.BACKSPACE, ui.textColor, Modifier.size(19.dp))
                        }
                    }
                }
                Spacer(Modifier.height(rowGap))

                /* ROW 4 */
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    SpecialKey(ui, 1.45f, bg = ui.st.keys.numberColor, onPress = {
                        if (interactive) {
                            haptic()
                            when {
                                emoji -> emoji = false
                                symbols -> symbols = false
                                else -> symbols = true
                            }
                            shift = ShiftState.OFF
                        }
                    }) { SpecialText(ui, if (symbols || emoji) "ABC" else "?123") }
                    SpecialKey(ui, 1f, bg = ui.st.keys.emojiColor, onPress = {
                        if (interactive) {
                            haptic()
                            emoji = !emoji
                            if (emoji) {
                                symbols = false
                                shift = ShiftState.OFF
                            }
                        }
                    }) {
                        RekeyGlyph(Glyph.SMILEY, if (emoji) p.enter else ui.textColor, Modifier.size(19.dp))
                    }
                    KeyBox(
                        modifier = Modifier.weight(4.5f).height(ui.keyH),
                        bg = ui.keyBg,
                        pressedBg = ui.specialBg,
                        border = ui.borderCol,
                        borderWidth = ui.borderW,
                        shape = ui.keyShape,
                        interactive = interactive,
                        onHaptic = haptic,
                        onPress = { emit(KeyAction.SPACE, " ") },
                    ) {
                        Text(
                            text = "REKEY",
                            color = p.muted,
                            style = BrandTypography.wordmark.copy(
                                fontSize = if (compact) 10.sp else 11.sp,
                                lineHeight = if (compact) 13.sp else 14.sp,
                                letterSpacing = 1.2.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                    SpecialKey(ui, 1f, onPress = { emit(KeyAction.TEXT, ".") }) { SpecialText(ui, ".") }
                    KeyBox(
                        modifier = Modifier.weight(1.45f).height(ui.keyH),
                        bg = ui.st.keys.enterColor ?: p.enter,
                        pressedBg = ui.specialBg,
                        border = ui.borderCol,
                        borderWidth = ui.borderW,
                        shape = ui.keyShape,
                        interactive = interactive,
                        onHaptic = haptic,
                        onPress = { emit(KeyAction.ENTER) },
                    ) {
                        RekeyGlyph(
                            glyph = Glyph.ENTER,
                            color = ui.st.keys.enterTextColor ?: p.enterText,
                            modifier = Modifier.size(if (compact) 25.dp else 27.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page 1's result: a flat colour, a two-stop gradient, or a photo, optionally
 * blurred and dimmed. The keys are drawn on top and stay solid.
 *
 * [modifier] MUST be Modifier.matchParentSize(). The board Box wraps its
 * content, so a fillMaxSize child would expand to the whole incoming height —
 * in an IME that is the entire window, which is what stretched the keyboard to
 * the status bar. matchParentSize takes the size the keys decided instead.
 */
@Composable
private fun KeyboardBackground(
    modifier: Modifier,
    spec: BackgroundStyle,
    fallback: Color,
    image: ImageBitmap?,
) {
    val base = spec.color ?: fallback
    if (spec.useImage && image != null) {
        Box(modifier.clipToBounds()) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = spec.imageScale
                        scaleY = spec.imageScale
                    }
                    .then(if (spec.blurDp > 0f) Modifier.blur(spec.blurDp.dp) else Modifier),
            )
            if (spec.dim > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = spec.dim)))
            }
        }
    } else {
        val brush = if (spec.gradient != null) {
            val rad = Math.toRadians(spec.gradientAngle.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            Brush.linearGradient(
                colors = listOf(base, spec.gradient),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(1000f * dx + 500f, 1000f * dy + 500f),
            )
        } else {
            Brush.linearGradient(listOf(base, base))
        }
        Box(modifier.background(brush))
        if (spec.dim > 0f) {
            Box(modifier.background(Color.Black.copy(alpha = spec.dim)))
        }
    }
}

/**
 * The handle above the suggestion strip.
 *
 * Swipe down closes the keyboard, double tap toggles floating mode, and while
 * floating a drag moves the board. It is deliberately a separate composable so
 * its gestures never reach the keys underneath.
 */
@Composable
private fun Grabber(
    spec: SuggestionStyle,
    ui: Ui,
    floating: Boolean,
    onHide: () -> Unit,
    onFloat: () -> Unit,
    onDrag: (Float) -> Unit,
) {
    val tint = spec.grabberColor ?: ui.p.muted
    Box(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
            .pointerInput(floating) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        dragged += dy
                        if (floating) onDrag(dy)
                    },
                    onDragEnd = { if (!floating && dragged > 24f) onHide() },
                    onDragCancel = { },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onFloat() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(spec.grabberWidth.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(tint.copy(alpha = 0.55f)),
        )
    }
}

/** Page 6's result: cursor keys plus whichever editing tools were switched on. */
@Composable
private fun ArrowBar(
    spec: ArrowStyle,
    ui: Ui,
    gap: Dp,
    onAction: (KeyAction) -> Unit,
) {
    val bg = spec.color ?: ui.p.special
    val tint = spec.iconColor ?: ui.textColor
    val shape = RoundedCornerShape(spec.radius.dp)
    val keyW = 36.dp
    Row(
        Modifier.fillMaxWidth().height(spec.height.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        // Tools scroll; the four cursor keys never do. Giving every button an
        // equal weight squeezed twelve of them into one row, which is why the
        // bar looked broken.
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            @Composable
            fun tool(glyph: Glyph, action: KeyAction) {
                KeyBox(
                    modifier = Modifier.width(keyW).height(spec.height.dp),
                    bg = bg,
                    pressedBg = ui.specialBg,
                    shape = shape,
                    interactive = ui.interactive,
                    onHaptic = ui.haptic,
                    onPress = { onAction(action) },
                ) { RekeyGlyph(glyph, tint, Modifier.size(18.dp)) }
            }
            if (spec.selectAll) tool(Glyph.SELECT_ALL, KeyAction.SELECT_ALL)
            if (spec.copy) tool(Glyph.COPY, KeyAction.COPY)
            if (spec.cut) tool(Glyph.CUT, KeyAction.CUT)
            if (spec.paste) tool(Glyph.PASTE, KeyAction.PASTE)
            if (spec.bold) tool(Glyph.BOLD, KeyAction.BOLD)
            if (spec.italic) tool(Glyph.ITALIC, KeyAction.ITALIC)
            if (spec.underline) tool(Glyph.UNDERLINE, KeyAction.UNDERLINE)
            if (spec.homeEnd) {
                tool(Glyph.HOME, KeyAction.HOME)
                tool(Glyph.END, KeyAction.END)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            listOf(
                Glyph.CHEVRON_LEFT to KeyAction.LEFT,
                Glyph.CHEVRON_UP to KeyAction.UP,
                Glyph.CHEVRON_DOWN to KeyAction.DOWN,
                Glyph.CHEVRON_RIGHT to KeyAction.RIGHT,
            ).forEach { (g, a) ->
                KeyBox(
                    modifier = Modifier.width(keyW).height(spec.height.dp),
                    bg = bg,
                    pressedBg = ui.specialBg,
                    shape = shape,
                    interactive = ui.interactive,
                    onHaptic = ui.haptic,
                    onPress = { onAction(a) },
                ) { RekeyGlyph(g, tint, Modifier.size(18.dp)) }
            }
        }
    }
}

/**
 * Grouped emoji: a category rail, a scrolling grid, and a backspace.
 *
 * The whole panel occupies exactly the height of the three letter rows, so
 * switching to emoji never resizes the keyboard or moves the keys under the
 * owner's thumbs.
 */
@Composable
private fun EmojiPanel(
    ui: Ui,
    p: KeyboardPalette,
    gap: Dp,
    rowGap: Dp,
    onPick: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    var group by remember { mutableIntStateOf(0) }
    val panelH = ui.keyH * 3 + rowGap * 2
    val railH = 30.dp
    Column(Modifier.fillMaxWidth().height(panelH)) {
        Row(
            Modifier.fillMaxWidth().height(railH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                EmojiGroups.forEachIndexed { i, g ->
                    val on = i == group
                    KeyBox(
                        modifier = Modifier.width(38.dp).height(railH),
                        bg = if (on) ui.specialBg else Color.Transparent,
                        pressedBg = ui.specialBg,
                        shape = RoundedCornerShape(8.dp),
                        interactive = ui.interactive,
                        onHaptic = ui.haptic,
                        onPress = { group = i },
                    ) {
                        RekeyGlyph(g.icon, if (on) p.enter else ui.textColor, Modifier.size(16.dp))
                    }
                }
            }
            KeyBox(
                modifier = Modifier.width(40.dp).height(railH),
                bg = ui.specialBg,
                pressedBg = ui.specialBg,
                shape = RoundedCornerShape(8.dp),
                repeatOnHold = true,
                interactive = ui.interactive,
                onHaptic = ui.haptic,
                onPress = onBackspace,
            ) { RekeyGlyph(Glyph.BACKSPACE, ui.textColor, Modifier.size(17.dp)) }
        }
        Spacer(Modifier.height(rowGap))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(remember(group) { ScrollState(0) }),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            EmojiGroups[group].items.chunked(8).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { e ->
                        KeyBox(
                            modifier = Modifier.weight(1f).height(ui.keyH),
                            bg = Color.Transparent,
                            pressedBg = ui.specialBg,
                            shape = ui.keyShape,
                            interactive = ui.interactive,
                            onHaptic = ui.haptic,
                            onPress = { onPick(e) },
                        ) { Text(text = e, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 1) }
                    }
                    repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Toolbar: suggestion strip on the left, expanding action pill on the right. */
@Composable
private fun KeyboardToolbar(
    p: KeyboardPalette,
    ui: Ui,
    spec: SuggestionStyle,
    inner: Dp,
    gap: Dp,
    compact: Boolean,
    open: Boolean,
    suggestions: List<String>,
    onToggle: () -> Unit,
    onSuggestion: (String) -> Unit,
    onPaste: () -> Unit,
    onTheme: () -> Unit,
    onLanguage: () -> Unit,
) {
    val pillTarget = (inner * 0.53f).coerceIn(175.dp, 211.dp)
    val pillWidth by animateDpAsState(
        targetValue = if (open) pillTarget else 37.dp,
        animationSpec = tween(350, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)),
        label = "pill",
    )
    val optAlpha by animateFloatAsState(if (open) 1f else 0f, tween(200), label = "oa")
    val optShift by animateDpAsState(if (open) 0.dp else 12.dp, tween(250), label = "os")
    val arrowRot by animateFloatAsState(if (open) 180f else 0f, tween(300), label = "ar")
    val barH = (if (compact) spec.height * 0.95f else spec.height).dp
    val sugShape = RoundedCornerShape(spec.radius.dp)

    Row(
        Modifier.fillMaxWidth().height(barH + 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            Modifier.weight(1f).clipToBounds(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            if (spec.enabled) {
                suggestions.take(spec.columns).forEach { w ->
                    KeyBox(
                        modifier = Modifier.weight(1f).height(barH),
                        bg = spec.color ?: ui.keyBg,
                        pressedBg = ui.specialBg,
                        border = ui.borderCol,
                        borderWidth = ui.borderW,
                        shape = sugShape,
                        interactive = ui.interactive,
                        onHaptic = ui.haptic,
                        onPress = { onSuggestion(w) },
                    ) {
                        Text(
                            text = w,
                            color = spec.textColor ?: ui.textColor,
                            fontSize = (if (compact) spec.textSize * 0.85f else spec.textSize).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 5.dp),
                        )
                    }
                }
            }
        }

        val pillShape = RoundedCornerShape(spec.pillRadius.dp)
        Row(
            Modifier
                .width(pillWidth).height(37.dp)
                .clip(pillShape).background(spec.pillColor ?: spec.color ?: ui.keyBg)
                .border(1.dp, ui.borderCol ?: p.border, pillShape)
                .padding(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clipToBounds().graphicsLayer {
                    alpha = optAlpha
                    translationX = optShift.toPx()
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val iconTint = spec.pillIconColor ?: ui.textColor
                if (spec.pillPaste) ToolBtn(ui, Glyph.PASTE, iconTint, onPaste)
                if (spec.pillTranslate) ToolBtn(ui, Glyph.TRANSLATE, iconTint) {}
                if (spec.pillTheme) ToolBtn(ui, Glyph.PALETTE, iconTint, onTheme)
                if (spec.pillLanguage) ToolBtn(ui, Glyph.GLOBE, iconTint, onLanguage)
            }
            KeyBox(
                modifier = Modifier.size(35.dp),
                bg = Color.Transparent,
                shape = CircleShape,
                pressedScale = .84f,
                interactive = ui.interactive,
                onHaptic = ui.haptic,
                onPress = onToggle,
            ) {
                RekeyGlyph(
                    glyph = Glyph.CHEVRON_LEFT,
                    color = ui.textColor,
                    modifier = Modifier.size(21.dp).rotate(arrowRot),
                )
            }
        }
    }
}

/** Cross-fades every palette colour, matching `transition: background .25s`. */
@Composable
private fun animatedPalette(t: KeyboardPalette): KeyboardPalette {
    val spec = tween<Color>(250)
    return KeyboardPalette(
        tray = animateColorAsState(t.tray, spec, label = "tray").value,
        board = animateColorAsState(t.board, spec, label = "board").value,
        key = animateColorAsState(t.key, spec, label = "key").value,
        special = animateColorAsState(t.special, spec, label = "special").value,
        text = animateColorAsState(t.text, spec, label = "text").value,
        canvas = animateColorAsState(t.canvas, spec, label = "canvas").value,
        enter = animateColorAsState(t.enter, spec, label = "enter").value,
        enterText = animateColorAsState(t.enterText, spec, label = "enterText").value,
        icon = animateColorAsState(t.icon, spec, label = "icon").value,
        muted = animateColorAsState(t.muted, spec, label = "muted").value,
        border = animateColorAsState(t.border, spec, label = "border").value,
    )
}

@Composable
private fun RowScope.CharKey(ui: Ui, label: String, onPress: () -> Unit) {
    KeyBox(
        modifier = Modifier.weight(1f).height(ui.keyH),
        bg = ui.keyBg,
        pressedBg = ui.specialBg,
        border = ui.borderCol,
        borderWidth = ui.borderW,
        shape = ui.keyShape,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onPress,
    ) {
        Text(
            text = label,
            color = ui.textColor,
            fontSize = ui.fs.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.SpecialKey(
    ui: Ui,
    weight: Float,
    repeat: Boolean = false,
    bg: Color? = null,
    onPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    KeyBox(
        modifier = Modifier.weight(weight).height(ui.keyH),
        bg = bg ?: ui.specialBg,
        pressedBg = ui.specialBg,
        border = ui.borderCol,
        borderWidth = ui.borderW,
        shape = ui.keyShape,
        repeatOnHold = repeat,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onPress,
        content = content,
    )
}

@Composable
private fun SpecialText(ui: Ui, s: String) {
    Text(s, color = ui.textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

@Composable
private fun ToolBtn(ui: Ui, glyph: Glyph, tint: Color, onPress: () -> Unit) {
    KeyBox(
        modifier = Modifier.size(width = 31.dp, height = 34.dp),
        bg = Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        pressedScale = .84f,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onPress,
    ) {
        RekeyGlyph(glyph, tint, Modifier.size(21.dp))
    }
}

/**
 * Base key: fires on press-down (not on release) so typing feels immediate,
 * scales to .96 over 90 ms, and optionally repeats while held (backspace).
 *
 * A non-interactive card skips the pointer input, the coroutine scope and both
 * animations entirely — a preview has forty of these, so that matters.
 */
@Composable
private fun KeyBox(
    modifier: Modifier = Modifier,
    bg: Color,
    pressedBg: Color = bg,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    pressedScale: Float = .96f,
    repeatOnHold: Boolean = false,
    interactive: Boolean = true,
    onHaptic: () -> Unit = {},
    onPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!interactive) {
        Box(
            modifier
                .clip(shape)
                .background(bg)
                .then(
                    if (border != null && borderWidth > 0.dp) {
                        Modifier.border(borderWidth, border, shape)
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    } else {
        InteractiveKey(
            modifier, bg, pressedBg, border, borderWidth, shape, pressedScale,
            repeatOnHold, onHaptic, onPress, content,
        )
    }
}

@Composable
private fun InteractiveKey(
    modifier: Modifier,
    bg: Color,
    pressedBg: Color,
    border: Color?,
    borderWidth: Dp,
    shape: Shape,
    pressedScale: Float,
    repeatOnHold: Boolean,
    onHaptic: () -> Unit,
    onPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentPress by rememberUpdatedState(onPress)
    val currentHaptic by rememberUpdatedState(onHaptic)
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, tween(90), label = "scale")
    val color by animateColorAsState(
        targetValue = if (pressed) pressedBg else bg,
        animationSpec = tween(if (pressed) 90 else 200),
        label = "keybg",
    )

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(color)
            .then(
                if (border != null && borderWidth > 0.dp) {
                    Modifier.border(borderWidth, border, shape)
                } else Modifier,
            )
            .pointerInput(repeatOnHold) {
                detectTapGestures(onPress = {
                    pressed = true
                    currentHaptic()
                    currentPress()
                    val job = if (repeatOnHold) {
                        scope.launch {
                            delay(400)
                            while (isActive) {
                                currentPress()
                                delay(50)
                            }
                        }
                    } else null
                    tryAwaitRelease()
                    job?.cancel()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Draws the keyboard edge-to-edge at whatever width it is given.
 *
 * The canvas is authored at 440 dp. Rather than drawing it oversized and scaling
 * the render layer — which leaves the layout box wider than the pixels and makes
 * the keyboard drift sideways — this scales the *density*. The keyboard then
 * measures exactly the available physical width, so its edges land on the
 * container's edges by construction and text stays vector-crisp.
 */
@Composable
fun ScaledKeyboard(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    style: KeyboardStyle = KeyboardStyle(),
    backgroundImage: ImageBitmap? = null,
    showToolbar: Boolean = true,
    interactive: Boolean = true,
    suggestions: List<String> = emptyList(),
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
) {
    BoxWithConstraints(modifier) {
        val base = LocalDensity.current
        val scale = maxWidth.value / KeyboardCanvas.WIDTH_VALUE
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, base.fontScale),
        ) {
            RekeyKeyboard(
                palette = palette,
                style = style,
                backgroundImage = backgroundImage,
                showToolbar = showToolbar,
                interactive = interactive,
                suggestions = suggestions,
                onKey = onKey,
            )
        }
    }
}
