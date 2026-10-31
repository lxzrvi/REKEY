package io.github.lxzrvi.rekey.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.data.KeyboardPalette
import io.github.lxzrvi.rekey.ui.brand.BrandTypography
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The REKEY keyboard.
 *
 * Authored on a 440 dp canvas so a preset card and the real input method draw
 * exactly the same geometry. [KeyboardCanvas.OUTER] is the 7 dp side padding;
 * everything else is measured from the inner width, so the layout stays
 * proportional at every size.
 */
object KeyboardCanvas {
    const val WIDTH_VALUE = 440f
    val WIDTH: Dp = WIDTH_VALUE.dp

    /** Toolbar strip + four key rows + padding. */
    val HEIGHT: Dp = 265.dp

    /** Taller strip for the card's name block, same key board. */
    val CARD_HEIGHT: Dp = 297.dp

    const val RATIO = 440f / 265f
    const val CARD_RATIO = 440f / 297f

    const val OUTER = 7f
}

private val Outer = KeyboardCanvas.OUTER.dp
private val PadTop = 10.dp
private val PadBottom = 11.dp
val ImeToolbarHeight: Dp = 40.dp
val CardHeaderHeight: Dp = 72.dp

private val Letters1 = "qwertyuiop".map { it.toString() }
private val Letters2 = "asdfghjkl".map { it.toString() }
private val Letters3 = "zxcvbnm".map { it.toString() }
private val Symbols1 = "1234567890".map { it.toString() }
private val Symbols2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
private val Symbols3 = listOf("*", "\"", "'", ":", ";", "!", "?")

/** What the input method needs to know about a tap. */
enum class KeyAction { TEXT, SPACE, ENTER, BACKSPACE, LANGUAGE, PASTE, CYCLE_THEME }

private enum class ShiftState { OFF, ON, CAPS }

/** Everything a key needs, bundled so the row helpers stay short. */
private class Ui(
    val p: KeyboardPalette,
    val keyH: Dp,
    val fs: Float,
    val interactive: Boolean,
    val haptic: () -> Unit,
)

@Composable
fun RekeyKeyboard(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    headerHeight: Dp = ImeToolbarHeight,
    interactive: Boolean = true,
    suggestions: List<String> = emptyList(),
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
) {
    var open by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(ShiftState.OFF) }
    var lastShiftTap by remember { mutableLongStateOf(0L) }
    var symbols by remember { mutableStateOf(false) }

    val view = LocalView.current
    val haptic: () -> Unit = remember(view, interactive) {
        { if (interactive) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }

    // Theme colours cross-fade in the IME; a static card just uses them directly.
    val p = if (interactive) animatedPalette(palette) else palette

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        val kbWidth = minOf(maxWidth, KeyboardCanvas.WIDTH)
        val inner = kbWidth - Outer * 2
        val compact = kbWidth < 350.dp
        val gap = if (compact) 3.dp else 5.dp
        val rowGap = if (compact) 7.dp else 8.dp
        val ui = Ui(p, if (compact) 41.dp else 43.dp, if (compact) 17f else 19f, interactive, haptic)

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

        Column(
            Modifier
                .width(kbWidth)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(p.canvas)
                .padding(start = Outer, end = Outer, top = PadTop, bottom = PadBottom),
        ) {
            if (showToolbar) {
                KeyboardToolbar(
                    p = p,
                    ui = ui,
                    inner = inner,
                    gap = gap,
                    compact = compact,
                    open = open,
                    suggestions = suggestions,
                    onToggle = { open = !open },
                    onSuggestion = { emit(KeyAction.TEXT, "$it ") },
                    onPaste = { if (open) emit(KeyAction.PASTE) },
                    onTheme = { if (open) emit(KeyAction.CYCLE_THEME) },
                )
            } else {
                Box(Modifier.fillMaxWidth().height(headerHeight))
            }
            Spacer(Modifier.height(rowGap))

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
                    SpecialKey(ui, 1.45f, onPress = { symbols = false }) { SpecialText(ui, "=<") }
                } else {
                    SpecialKey(ui, 1.45f, onPress = {
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
                            color = if (shift == ShiftState.OFF) p.text else p.enter,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                (if (symbols) Symbols3 else Letters3).forEach { c ->
                    CharKey(ui, label(c)) { onChar(c) }
                }
                SpecialKey(ui, 1.45f, repeat = true, onPress = { emit(KeyAction.BACKSPACE) }) {
                    RekeyGlyph(Glyph.BACKSPACE, p.text, Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(rowGap))

            /* ROW 4 */
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                SpecialKey(ui, 1.45f, onPress = {
                    if (interactive) {
                        haptic()
                        symbols = !symbols
                        shift = ShiftState.OFF
                    }
                }) { SpecialText(ui, if (symbols) "ABC" else "?123") }
                SpecialKey(ui, 1f, onPress = { emit(KeyAction.LANGUAGE) }) {
                    RekeyGlyph(Glyph.GLOBE, p.text, Modifier.size(19.dp))
                }
                KeyBox(
                    modifier = Modifier.weight(4.5f).height(ui.keyH),
                    bg = p.key,
                    pressedBg = p.special,
                    border = p.border,
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
                    bg = p.enter,
                    pressedBg = p.special,
                    border = p.border,
                    interactive = interactive,
                    onHaptic = haptic,
                    onPress = { emit(KeyAction.ENTER) },
                ) {
                    RekeyGlyph(
                        glyph = Glyph.ENTER,
                        color = p.enterText,
                        modifier = Modifier.size(if (compact) 25.dp else 27.dp),
                    )
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
    inner: Dp,
    gap: Dp,
    compact: Boolean,
    open: Boolean,
    suggestions: List<String>,
    onToggle: () -> Unit,
    onSuggestion: (String) -> Unit,
    onPaste: () -> Unit,
    onTheme: () -> Unit,
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

    Row(
        Modifier.fillMaxWidth().height(if (compact) 38.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            Modifier.weight(1f).clipToBounds(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            suggestions.forEach { w ->
                KeyBox(
                    modifier = Modifier.weight(1f).height(if (compact) 33.dp else 34.dp),
                    bg = p.key,
                    pressedBg = p.special,
                    border = p.border,
                    interactive = ui.interactive,
                    onHaptic = ui.haptic,
                    onPress = { onSuggestion(w) },
                ) {
                    Text(
                        text = w,
                        color = p.text,
                        fontSize = if (compact) 10.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 5.dp),
                    )
                }
            }
        }

        val pillShape = RoundedCornerShape(30.dp)
        Row(
            Modifier
                .width(pillWidth).height(37.dp)
                .clip(pillShape).background(p.key)
                .border(1.dp, p.border, pillShape)
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
                ToolBtn(ui, Glyph.PASTE, onPaste)
                ToolBtn(ui, Glyph.TRANSLATE) {}
                ToolBtn(ui, Glyph.PALETTE, onTheme)
                ToolBtn(ui, Glyph.MORE) {}
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
                    color = p.text,
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
        bg = ui.p.key,
        pressedBg = ui.p.special,
        border = ui.p.border,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onPress,
    ) {
        Text(
            text = label,
            color = ui.p.text,
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
    onPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    KeyBox(
        modifier = Modifier.weight(weight).height(ui.keyH),
        bg = ui.p.special,
        pressedBg = ui.p.special,
        border = ui.p.border,
        repeatOnHold = repeat,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onPress,
        content = content,
    )
}

@Composable
private fun SpecialText(ui: Ui, s: String) {
    Text(s, color = ui.p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

@Composable
private fun ToolBtn(ui: Ui, glyph: Glyph, onPress: () -> Unit) {
    KeyBox(
        modifier = Modifier.size(width = 31.dp, height = 34.dp),
        bg = Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        pressedScale = .84f,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onPress,
    ) {
        RekeyGlyph(glyph, ui.p.text, Modifier.size(21.dp))
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
                .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier),
            contentAlignment = Alignment.Center,
            content = content,
        )
    } else {
        InteractiveKey(
            modifier, bg, pressedBg, border, shape, pressedScale,
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
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
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
    showToolbar: Boolean = true,
    headerHeight: Dp = ImeToolbarHeight,
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
                showToolbar = showToolbar,
                headerHeight = headerHeight,
                interactive = interactive,
                suggestions = suggestions,
                onKey = onKey,
            )
        }
    }
}
