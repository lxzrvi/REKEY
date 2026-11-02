package io.github.lxzrvi.rekey.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import android.view.HapticFeedbackConstants
import io.github.lxzrvi.rekey.data.ArrowStyle
import io.github.lxzrvi.rekey.data.ClipEntry
import io.github.lxzrvi.rekey.data.BackgroundStyle
import io.github.lxzrvi.rekey.data.KeyboardPalette
import io.github.lxzrvi.rekey.data.KeyboardStyle
import io.github.lxzrvi.rekey.data.SuggestionStyle
import io.github.lxzrvi.rekey.ui.brand.BrandTypography
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.rememberUriImage
import io.github.lxzrvi.rekey.ime.EmojiCombiner
import io.github.lxzrvi.rekey.ime.EmojiEntry
import io.github.lxzrvi.rekey.ime.EmojiRepository
import io.github.lxzrvi.rekey.ime.MashupStyle
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import io.github.lxzrvi.rekey.ui.home.TopLanguages
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
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

    /**
     * The height the board actually wants, in design dp.
     *
     * A preview box cannot assume a fixed ratio: the owner can change key
     * height, padding, the suggestion strip and the grab handle. Squeezing the
     * board into a stale ratio is what flattened the bottom row.
     */
    fun contentHeight(style: KeyboardStyle, showToolbar: Boolean, compact: Boolean): Float {
        val lay = style.layout
        val rowGap = if (compact) lay.rowGap * 0.875f else lay.rowGap
        val keyH = (if (compact) 41f else 43f) * lay.heightScale
        var h = lay.padTop + lay.padBottom + keyH * 4f + rowGap * 3f
        if (style.suggestions.grabberEnabled) h += 18f
        if (style.arrows.enabled) h += style.arrows.height + rowGap
        if (showToolbar) {
            h += (if (compact) style.suggestions.height * 0.95f else style.suggestions.height) + 6f + rowGap
        }
        return h.coerceAtLeast(120f)
    }
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
    BOLD, ITALIC, UNDERLINE, HIDE, TOGGLE_FLOAT, SUGGEST, SWITCH_KEYBOARD, LONG_RELEASE, CURSOR, CLIPBOARD,
    CLIP_PICK, CLIP_PIN, CLIP_REMOVE, SET_LANGUAGE, VOICE_TOGGLE, COMBINE_SEND, FLOAT_PERMISSION,
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
    onFloatingDrag: (Float, Float) -> Unit = { _, _ -> },
    onFloatResize: (Float) -> Unit = {},
    suggestions: List<String> = emptyList(),
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
    /** Bumped by the IME on every new field: reopening lands on the text page. */
    resetToken: Int = 0,
    voiceActive: Boolean = false,
    voiceLevel: Float = 0f,
    clipHistory: List<ClipEntry> = emptyList(),
    keyboardLanguage: String = "en",
    floatNeedsPermission: Boolean = false,
    onFloatPermission: () -> Unit = {},
    onFloatPromptDismiss: () -> Unit = {},
    onCombineSend: (String, String, String) -> Unit = { _, _, _ -> },
) {
    var open by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(ShiftState.OFF) }
    var lastShiftTap by remember { mutableLongStateOf(0L) }
    var symbols by remember { mutableStateOf(false) }
    var symbolPage by remember { mutableIntStateOf(0) }
    var toolsOpen by remember { mutableStateOf(false) }
    var langOpen by remember { mutableStateOf(false) }
    var clipOpen by remember { mutableStateOf(false) }
    var closeY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var floatPromptDone by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<HintOpen?>(null) }
    var hintQuad by remember { mutableIntStateOf(-1) }
    var clipBtnBounds by remember { mutableStateOf(Rect.Zero) }
    var langBtnBounds by remember { mutableStateOf(Rect.Zero) }
    var spaceBounds by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()

    // A reopened field always starts on the text page: no emoji, no symbols,
    // no open tray, no half-dragged close.
    LaunchedEffect(resetToken) {
        emoji = false
        symbols = false
        symbolPage = 0
        shift = ShiftState.OFF
        open = false
        toolsOpen = false
        langOpen = false
        clipOpen = false
        closeY = 0f
        hint = null
        hintQuad = -1
        floatPromptDone = false
    }

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
            // Every tray is momentary: typing or acting anywhere dismisses the
            // pill, the tools, the sheets. Icons go through emitRaw so they
            // can be used repeatedly.
            open = false
            toolsOpen = false
            langOpen = false
            clipOpen = false
            onKey(action, text)
        }

        fun emitRaw(action: KeyAction, text: String = "") {
            if (!interactive) return
            onKey(action, text)
        }

        // Long-press a letter: the board frosts, the key grows, a slide picks
        // the corner, and the release commits it.
        val hintStartFn: (String, Array<String?>, Rect) -> Unit = { l, o, r ->
            hint = HintOpen(l, o, r)
            hintQuad = -1
        }
        val hintSlideFn: (Int) -> Unit = { q ->
            hintQuad = q
            haptic()
        }
        val hintCommitFn: (Int) -> Unit = { q ->
            val h = hint
            hint = null
            hintQuad = -1
            if (h != null) {
                val pick = (if (q in 0..3) h.options[q] else null) ?: h.options[1]
                if (pick != null) {
                    haptic()
                    emit(KeyAction.TEXT, pick)
                }
            }
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

        val boardSize = remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
        Box(
            Modifier
                .offset { IntOffset(0, closeY.roundToInt()) }
                .width(kbWidth)
                .animateContentSize(
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 320f),
                    label = "boardSize",
                )
                .clip(boardShape)
                .onGloballyPositioned { c ->
                    boardSize.value = c.boundsInWindow()
                },
        ) {
            KeyboardBackground(Modifier.matchParentSize(), style.background, p.canvas, backgroundImage)
            Column(
                Modifier
                    .fillMaxWidth()
                    // One blur over the whole board: every key frosts while a
                    // corner hint is open, and the grown key is redrawn crisp
                    // in the overlay above this layer.
                    .then(if (hint != null) Modifier.blur(6.dp) else Modifier)
                    .padding(
                        start = padStart,
                        end = padEnd,
                        top = lay.padTop.dp,
                        bottom = lay.padBottom.dp + navBottom,
                    ),
            ) {
                if (floating && floatNeedsPermission && !floatPromptDone) {
                    FloatPermissionBar(
                        onAllow = { floatPromptDone = true; onFloatPermission() },
                        onDismiss = { floatPromptDone = true; onFloatPromptDismiss() },
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (style.suggestions.grabberEnabled) {
                    val thresholdPx = with(density) { 64.dp.toPx() }
                    Grabber(
                        spec = style.suggestions,
                        ui = ui,
                        floating = floating,
                        onFloat = { emit(KeyAction.TOGGLE_FLOAT) },
                        onDrag = { dy ->
                            if (!interactive) return@Grabber
                            if (floating) onFloatingDrag(0f, dy)
                            else closeY = (closeY + dy).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (!interactive || floating) return@Grabber
                            if (closeY > thresholdPx) {
                                emit(KeyAction.HIDE)
                            } else {
                                val start = closeY
                                scope.launch {
                                    animate(
                                        initialValue = start,
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 260f),
                                    ) { v, _ -> closeY = v }
                                }
                            }
                        },
                    )
                }
                if (style.arrows.enabled) {
                    ArrowBar(style.arrows, ui, gap) { emit(it) }
                    Spacer(Modifier.height(rowGap))
                }
                if (showToolbar && !emoji) {
                    KeyboardToolbar(
                        p = p,
                        ui = ui,
                        spec = style.suggestions,
                        inner = inner,
                        gap = gap,
                        compact = compact,
                        open = open,
                        voiceActive = voiceActive,
                        voiceLevel = voiceLevel,
                        suggestions = suggestions,
                        onToggle = { open = !open },
                        onSuggestion = { emit(KeyAction.SUGGEST, it) },
                        onClipboard = {
                            if (open || clipOpen) { clipOpen = !clipOpen; open = false }
                        },
                        onTheme = { if (open) emitRaw(KeyAction.CYCLE_THEME) },
                        onLanguage = {
                            if (open || langOpen) { langOpen = !langOpen; open = false }
                        },
                        onSwitchKeyboard = { if (open) emitRaw(KeyAction.SWITCH_KEYBOARD) },
                        onVoice = { if (open || voiceActive) emitRaw(KeyAction.VOICE_TOGGLE) },
                        onClipBounds = { clipBtnBounds = it },
                        onLangBounds = { langBtnBounds = it },
                    )
                    Spacer(Modifier.height(rowGap))
                    if (clipOpen && clipBtnBounds != Rect.Zero) {
                        ClipPanelPopup(
                            anchor = clipBtnBounds,
                            items = clipHistory,
                            onPick = { emit(KeyAction.CLIP_PICK, it) },
                            onPin = { emitRaw(KeyAction.CLIP_PIN, it) },
                            onRemove = { emitRaw(KeyAction.CLIP_REMOVE, it) },
                            onDismiss = { clipOpen = false },
                        )
                    }
                    if (langOpen && langBtnBounds != Rect.Zero) {
                        LanguagePopup(
                            anchor = langBtnBounds,
                            current = keyboardLanguage,
                            onPick = { emit(KeyAction.SET_LANGUAGE, it) },
                            onDismiss = { langOpen = false },
                        )
                    }
                }

                // The letter board crossfades with the emoji panel while the
                // container height springs between three and five rows, so the
                // grow and the shrink are one movement in each direction.
                val fade by animateFloatAsState(if (emoji) 1f else 0f, tween(240), label = "emojiFade")
                val lettersH = ui.keyH * 3 + rowGap * 2
                val emojiH = ui.keyH * 5 + rowGap * 4
                val contentH by animateDpAsState(if (emoji) emojiH else lettersH, tween(240), label = "emojiGrow")
                Box(Modifier.fillMaxWidth().height(contentH)) {
                  if (!emoji || fade < 1f) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (emoji) 1f - fade else 1f },
                    ) {
                    /* ROW 1 */
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        (if (symbols) SymbolPages.pages[symbolPage][0] else Letters1).forEach { c ->
                            CharKey(
                                ui, label(c),
                                onPress = { onChar(c) },
                                onAlternate = { emit(KeyAction.TEXT, it) },
                                hintStart = hintStartFn,
                                hintSlide = hintSlideFn,
                                hintCommit = hintCommitFn,
                            )
                        }
                    }
                    Spacer(Modifier.height(rowGap))

                    /* ROW 2 — inset 5% of the inner width */
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = inner * 0.05f),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        (if (symbols) SymbolPages.pages[symbolPage][1] else Letters2).forEach { c ->
                            CharKey(
                                ui, label(c),
                                onPress = { onChar(c) },
                                onAlternate = { emit(KeyAction.TEXT, it) },
                                hintStart = hintStartFn,
                                hintSlide = hintSlideFn,
                                hintCommit = hintCommitFn,
                            )
                        }
                    }
                    Spacer(Modifier.height(rowGap))

                    /* ROW 3 */
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        if (symbols) {
                            SpecialKey(
                                ui, 1.45f, bg = ui.st.keys.numberColor,
                                onPress = { symbolPage = (symbolPage + 1) % SymbolPages.PAGE_COUNT },
                                onLongPress = {
                                    symbolPage =
                                        (symbolPage - 1 + SymbolPages.PAGE_COUNT) % SymbolPages.PAGE_COUNT
                                },
                                onLongRelease = { },
                            ) {
                                SpecialText(ui, "${symbolPage + 1}/${SymbolPages.PAGE_COUNT}")
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
                        (if (symbols) SymbolPages.pages[symbolPage][2] else Letters3).forEach { c ->
                            CharKey(
                                ui, label(c),
                                onPress = { onChar(c) },
                                onAlternate = { emit(KeyAction.TEXT, it) },
                                hintStart = hintStartFn,
                                hintSlide = hintSlideFn,
                                hintCommit = hintCommitFn,
                            )
                        }
                        SpecialKey(ui, 1.45f, repeat = true, onPress = { emit(KeyAction.BACKSPACE) }) {
                            RekeyGlyph(Glyph.BACKSPACE, ui.textColor, Modifier.size(19.dp))
                        }
                    }
                    }
                  }
                  if (emoji || fade > 0f) {
                    EmojiPanel(
                        ui = ui,
                        p = p,
                        gap = gap,
                        rowGap = rowGap,
                        alpha = fade,
                        enabled = emoji,
                        onPick = { e ->
                            EmojiRepository.pushRecent(e)
                            emit(KeyAction.TEXT, e)
                        },
                        onBackspace = { emit(KeyAction.BACKSPACE) },
                        onCombineSend = onCombineSend,
                        modifier = Modifier.fillMaxSize(),
                    )
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
                                symbols -> { symbols = false; symbolPage = 0 }
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
                        modifier = Modifier
                            .weight(4.5f)
                            .height(ui.keyH)
                            .onGloballyPositioned { spaceBounds = it.boundsInWindow() },
                        bg = ui.keyBg,
                        pressedBg = ui.specialBg,
                        border = ui.borderCol,
                        borderWidth = ui.borderW,
                        shape = ui.keyShape,
                        interactive = interactive,
                        onHaptic = haptic,
                        cursorSwipe = interactive,
                        onCursor = { emit(KeyAction.CURSOR, it.toString()) },
                        onLongPress = if (interactive) { { toolsOpen = true } } else null,
                        onLongRelease = { },
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

            // Long-press tray over the space bar: every copy/paste tool lives
            // inside the keyboard, one lift of the thumb away.
            if (toolsOpen && spaceBounds != Rect.Zero && interactive) {
                KeyToolsPopup(
                    anchor = spaceBounds,
                    onAction = { emit(it) },
                    onDismiss = { toolsOpen = false },
                )
            }

            // The grown key: same corner, bigger box, four crisp hints.
            val hintOpen = hint
            if (hintOpen != null && interactive) {
                HintOverlay(
                    hint = hintOpen,
                    quad = hintQuad,
                    ui = ui,
                    p = p,
                    board = boardSize.value,
                )
            }

            // While floating, a curved grip in the top-right corner resizes
            // the whole board.
            if (floating && interactive) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .pointerInput(floating) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                onFloatResize((drag.x - drag.y) / 320f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(26.dp)) {
                        val sw = 2.2f * (size.minDimension / 24f)
                        val r = size.width * 0.9f
                        val arc = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.08f, size.height - r * 0.1f)
                            quadraticTo(
                                size.width * 0.08f, size.height * 0.08f,
                                size.width - r * 0.1f, size.height * 0.08f,
                            )
                        }
                        drawPath(
                            arc,
                            color = (ui.st.keys.specialColor ?: ui.p.muted).copy(alpha = 0.85f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw),
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
    onFloat: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val tint = spec.grabberColor ?: ui.p.muted
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            // The board follows the finger the whole way down; whether that
            // becomes a close is decided once, on release, by the parent.
            .pointerInput(floating) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        onDrag(dy)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
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
 * Grouped emoji: a recents rail slot, a category rail, a scrolling grid, a
 * backspace — and, at the end of the rail, the offline combiner.
 *
 * The parent owns the height animation; this panel only cross-fades, so
 * growing and shrinking are one smooth movement in each direction.
 */
@Composable
private fun EmojiPanel(
    ui: Ui,
    p: KeyboardPalette,
    gap: Dp,
    rowGap: Dp,
    alpha: Float,
    enabled: Boolean,
    onPick: (String) -> Unit,
    onBackspace: () -> Unit,
    onCombineSend: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val combiner = remember(context) { EmojiCombiner(context) }
    var group by remember { mutableIntStateOf(0) } // 0 = recents, 1..n = categories
    var combine by remember { mutableStateOf(false) }
    var slotA by remember { mutableStateOf<String?>(null) }
    var slotB by remember { mutableStateOf<String?>(null) }
    var mash by remember { mutableStateOf(MashupStyle.SPLIT) }
    val railH = 30.dp
    val cats = EmojiRepository.categories
    val ready = EmojiRepository.isReady && cats.isNotEmpty()
    val recents = EmojiRepository.recent

    val titles: List<String> =
        if (ready) listOf("Recent") + cats.map { it.title } else listOf("Recent")

    val entries: List<EmojiEntry> = when {
        group == 0 -> recents.map { r -> EmojiEntry(r, "", EmojiRepository.variantsFor(r)) }
        ready -> cats[(group - 1).coerceIn(0, cats.lastIndex)].items
        else -> emptyList()
    }

    val gridInteractive = ui.interactive && enabled

    Column(modifier.alpha(alpha.coerceIn(0f, 1f))) {
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
                titles.forEachIndexed { i, t ->
                    val on = i == group && !combine
                    KeyBox(
                        modifier = Modifier.width(40.dp).height(railH),
                        bg = if (on) ui.specialBg else Color.Transparent,
                        pressedBg = ui.specialBg,
                        shape = RoundedCornerShape(8.dp),
                        interactive = ui.interactive,
                        onHaptic = ui.haptic,
                        onPress = {
                            group = i
                            combine = false
                            slotA = null; slotB = null
                        },
                    ) {
                        val g = if (i == 0) Glyph.CLOCK else emojiGlyph(t)
                        RekeyGlyph(g, if (on) p.enter else ui.textColor, Modifier.size(16.dp))
                    }
                }
                // The combiner lives at the end of the same rail.
                KeyBox(
                    modifier = Modifier.width(44.dp).height(railH),
                    bg = if (combine) ui.specialBg else Color.Transparent,
                    pressedBg = ui.specialBg,
                    shape = RoundedCornerShape(8.dp),
                    interactive = ui.interactive,
                    onHaptic = ui.haptic,
                    onPress = { combine = !combine },
                ) {
                    RekeyGlyph(
                        Glyph.MERGE,
                        if (combine) p.enter else ui.textColor,
                        Modifier.size(16.dp),
                    )
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

        if (combine) {
            CombineTray(
                ui = ui,
                p = p,
                gap = gap,
                slotA = slotA,
                slotB = slotB,
                mash = mash,
                combiner = combiner,
                onStyle = { mash = it },
                onClear = { slotA = null; slotB = null },
                onSend = {
                    val a = slotA
                    val b = slotB
                    if (a != null && b != null) {
                        onCombineSend(a, b, mash.name)
                        slotA = null
                        slotB = null
                    }
                },
            )
            Spacer(Modifier.height(rowGap))
        } else {
            Spacer(Modifier.height(rowGap))
        }

        if (entries.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = if (group == 0) "Emoji you use will collect here."
                    else "Nothing here yet.",
                    fontSize = 12.sp,
                    color = ui.textColor.copy(alpha = 0.55f),
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(remember(group) { ScrollState(0) }),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                entries.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { e ->
                            var popup by remember(e.char) { mutableStateOf(false) }
                            var bounds by remember { mutableStateOf(Rect.Zero) }
                            KeyBox(
                                modifier = Modifier.weight(1f).height(ui.keyH)
                                    .onGloballyPositioned { bounds = it.boundsInWindow() },
                                bg = Color.Transparent,
                                pressedBg = ui.specialBg,
                                shape = ui.keyShape,
                                interactive = gridInteractive,
                                onHaptic = ui.haptic,
                                onLongPress = if (e.variants.isEmpty() || !gridInteractive) null else {
                                    { popup = true }
                                },
                                onLongRelease = { popup = false },
                                onPress = {
                                    if (combine) {
                                        if (slotA == null || slotB != null) {
                                            slotA = e.char
                                            slotB = null
                                        } else {
                                            slotB = e.char
                                        }
                                        EmojiRepository.pushRecent(e.char)
                                    } else {
                                        onPick(e.char)
                                    }
                                },
                            ) { Text(text = e.char, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 1) }
                            if (popup) {
                                AlternatesPopup(
                                    anchor = bounds,
                                    options = e.variants,
                                    selected = -1,
                                    onPick = { popup = false; onPick(it) },
                                    onDismiss = { popup = false },
                                )
                            }
                        }
                        repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Two slots, a live preview, four mashup styles and Send — all offline. */
@Composable
private fun CombineTray(
    ui: Ui,
    p: KeyboardPalette,
    gap: Dp,
    slotA: String?,
    slotB: String?,
    mash: MashupStyle,
    combiner: EmojiCombiner,
    onStyle: (MashupStyle) -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
) {
    val preview = remember(slotA, slotB, mash) {
        val a = slotA
        val b = slotB
        if (a != null && b != null) combiner.combine(a, b, mash) else null
    }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CombineSlot(slotA, ui, shape, Modifier.width(44.dp))
            RekeyGlyph(Glyph.PLUS, ui.textColor.copy(alpha = 0.6f), Modifier.size(14.dp))
            CombineSlot(slotB, ui, shape, Modifier.width(44.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(shape)
                    .background(ui.specialBg),
                contentAlignment = Alignment.Center,
            ) {
                when (preview) {
                    null -> Text("?", color = ui.textColor, fontSize = 18.sp)
                    is io.github.lxzrvi.rekey.ime.CombineResult.Zwj -> Text(
                        preview.text,
                        fontSize = 20.sp,
                        maxLines = 1,
                    )
                    is io.github.lxzrvi.rekey.ime.CombineResult.Mashup -> Image(
                        preview.bitmap.asImageBitmap(),
                        null,
                        Modifier.size(40.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                MashupStyle.entries.forEach { st ->
                    val on = st == mash
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (on) p.enter.copy(alpha = 0.25f) else ui.specialBg)
                            .clickable { onStyle(st) }
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                    ) {
                        Text(
                            st.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 9.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on) p.enter else ui.textColor,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = when {
                    slotA == null -> "Tap two emoji to combine — Unicode first, sticker second."
                    slotB == null -> "Pick the second emoji."
                    preview == null -> "Building…"
                    else -> combiner.describe(preview)
                },
                fontSize = 9.sp,
                color = ui.textColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            KeyBox(
                modifier = Modifier.height(26.dp).padding(horizontal = 0.dp),
                bg = Color.Transparent,
                shape = RoundedCornerShape(7.dp),
                pressedScale = .92f,
                interactive = ui.interactive,
                onHaptic = ui.haptic,
                onPress = onClear,
            ) {
                Text("Clear", fontSize = 10.sp, color = ui.textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
            KeyBox(
                modifier = Modifier.height(26.dp),
                bg = if (preview != null) p.enter else ui.specialBg,
                shape = RoundedCornerShape(7.dp),
                pressedScale = .92f,
                interactive = ui.interactive && preview != null,
                onHaptic = ui.haptic,
                onPress = onSend,
            ) {
                Text(
                    "Send",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (preview != null) ui.textColor else ui.textColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun CombineSlot(value: String?, ui: Ui, shape: androidx.compose.ui.graphics.Shape, modifier: Modifier) {
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(ui.specialBg),
        contentAlignment = Alignment.Center,
    ) {
        if (value == null) {
            Text("+", color = ui.textColor.copy(alpha = 0.5f), fontSize = 18.sp)
        } else {
            Text(value, fontSize = 20.sp, maxLines = 1)
        }
    }
}

/** Unicode group name -> one of our existing glyphs. */
private fun emojiGlyph(title: String): Glyph = when {
    title.startsWith("Smileys") -> Glyph.SMILEY
    title.startsWith("People") -> Glyph.CAPS_ON
    title.startsWith("Animals") -> Glyph.GLOBE
    title.startsWith("Food") -> Glyph.KEYBOARD_TEXT
    title.startsWith("Travel") -> Glyph.CHEVRON_RIGHT
    title.startsWith("Activities") -> Glyph.TRANSLATE
    title.startsWith("Objects") -> Glyph.PASTE
    title.startsWith("Flags") -> Glyph.CHECK
    else -> Glyph.PALETTE
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
    voiceActive: Boolean,
    voiceLevel: Float,
    suggestions: List<String>,
    onToggle: () -> Unit,
    onSuggestion: (String) -> Unit,
    onClipboard: () -> Unit,
    onTheme: () -> Unit,
    onLanguage: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onVoice: () -> Unit,
    onClipBounds: (Rect) -> Unit,
    onLangBounds: (Rect) -> Unit,
) {
    val restPill = (inner * 0.53f).coerceIn(175.dp, 211.dp)
    val pillTarget = when {
        // Voice takes the whole strip: the mic stretches, the suggestions
        // collapse to nothing, and stopping reverses the same movement.
        voiceActive -> (inner - gap).coerceAtLeast(120.dp)
        open -> restPill
        else -> 37.dp
    }
    val pillWidth by animateDpAsState(
        targetValue = pillTarget,
        animationSpec = tween(350, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)),
        label = "pill",
    )
    val optAlpha by animateFloatAsState(if (open || voiceActive) 1f else 0f, tween(200), label = "oa")
    val optShift by animateDpAsState(if (open || voiceActive) 0.dp else 12.dp, tween(250), label = "os")
    val arrowRot by animateFloatAsState(
        if (voiceActive) 90f else if (open) 180f else 0f,
        tween(300),
        label = "ar",
    )
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
                if (spec.pillPaste) {
                    Box(Modifier.onGloballyPositioned { onClipBounds(it.boundsInWindow()) }) {
                        ToolBtn(ui, Glyph.CLIPBOARD, iconTint, onClipboard)
                    }
                }
                if (spec.pillTranslate) ToolBtn(ui, Glyph.KEYBOARD_TEXT, iconTint, onSwitchKeyboard)
                if (spec.pillTheme) ToolBtn(ui, Glyph.CONTRAST, iconTint, onTheme)
                if (spec.pillLanguage) {
                    Box(Modifier.onGloballyPositioned { onLangBounds(it.boundsInWindow()) }) {
                        ToolBtn(ui, Glyph.GLOBE, iconTint, onLanguage)
                    }
                }
                if (spec.pillVoice && !voiceActive) ToolBtn(ui, Glyph.MIC, iconTint, onVoice)
            }
            if (voiceActive) {
                VoiceWave(ui, voiceLevel, width = 62.dp, onClick = onVoice)
            }
            KeyBox(
                modifier = Modifier.size(35.dp),
                bg = Color.Transparent,
                shape = CircleShape,
                pressedScale = .84f,
                interactive = ui.interactive,
                onHaptic = ui.haptic,
                onPress = { if (voiceActive) onVoice() else onToggle() },
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

/** The wide mic slot: five bars that breathe with the speaker's voice. */
@Composable
private fun VoiceWave(ui: Ui, level: Float, width: Dp, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Restart),
        label = "phase",
    )
    KeyBox(
        modifier = Modifier.width(width).height(34.dp),
        bg = Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        pressedScale = .9f,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onPress = onClick,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val amp = 0.3f + level.coerceIn(0f, 1f) * 0.7f
            repeat(5) { i ->
                val wobble = 0.55f + 0.45f * sin(phase + i * 0.9f)
                Box(
                    Modifier
                        .weight(1f)
                        .height((3f + 18f * amp * wobble).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ui.textColor.copy(alpha = 0.9f)),
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

/** The open corner-hint state: which key grew, and what its corners hold. */
private class HintOpen(val label: String, val options: Array<String?>, val rect: Rect)

@Composable
private fun RowScope.CharKey(
    ui: Ui,
    label: String,
    onPress: () -> Unit,
    onAlternate: (String) -> Unit = { _ -> },
    hintStart: ((String, Array<String?>, Rect) -> Unit)? = null,
    hintSlide: (Int) -> Unit = {},
    hintCommit: (Int) -> Unit = {},
) {
    val key = label.lowercase()
    val over = ui.st.keys.overrides[key]
    val upper = label != key && label.any { it.isUpperCase() }
    val corners = remember(key, upper) { KeyHints.cornersFor(key, upper) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val photo = rememberUriImage(over?.imageUri ?: ui.st.keys.imageUri, 256)
    val shape = over?.radius?.takeIf { it >= 0f }
        ?.let { RoundedCornerShape(it.dp) } ?: ui.keyShape
    val hintColor = (over?.textColor ?: ui.textColor)

    KeyBox(
        modifier = Modifier
            .weight(1f)
            .height(ui.keyH)
            .onGloballyPositioned { bounds = it.boundsInWindow() },
        bg = over?.color ?: ui.keyBg,
        pressedBg = ui.specialBg,
        border = ui.borderCol,
        borderWidth = ui.borderW,
        shape = shape,
        interactive = ui.interactive,
        onHaptic = ui.haptic,
        onLongPress = if (hintStart == null || !ui.interactive) null else {
            { hintStart(label, corners, bounds) }
        },
        onSlide = if (ui.interactive) hintSlide else null,
        onLongRelease = { q -> hintCommit(q) },
        onPress = onPress,
    ) {
        if (photo != null) {
            Image(
                photo, null, Modifier.matchParentSize().clip(shape),
                contentScale = ContentScale.Crop,
                alpha = over?.imageAlpha ?: ui.st.keys.imageAlpha,
            )
        }
        Text(
            text = label,
            color = over?.textColor ?: ui.textColor,
            fontSize = ui.fs.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        // Four quiet corner hints, always visible: long-press and slide to
        // pick one of them.
        HintCorner(corners[0], hintColor, Alignment.TopStart, ui.fs)
        HintCorner(corners[1], hintColor, Alignment.TopEnd, ui.fs)
        HintCorner(corners[2], hintColor, Alignment.BottomEnd, ui.fs)
        HintCorner(corners[3], hintColor, Alignment.BottomStart, ui.fs)
    }
}

@Composable
private fun BoxScope.HintCorner(
    text: String?,
    tint: Color,
    alignment: Alignment,
    fs: Float,
) {
    if (text.isNullOrEmpty()) return
    Text(
        text = text,
        color = tint.copy(alpha = 0.40f),
        fontSize = (fs * 0.34f).sp,
        maxLines = 1,
        modifier = Modifier
            .align(alignment)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun RowScope.SpecialKey(
    ui: Ui,
    weight: Float,
    repeat: Boolean = false,
    bg: Color? = null,
    onLongPress: (() -> Unit)? = null,
    onLongRelease: ((Int) -> Unit)? = null,
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
        onLongPress = onLongPress,
        onLongRelease = onLongRelease,
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
    /** Held past the long-press timeout. Null disables the long press. */
    onLongPress: (() -> Unit)? = null,
    /** Finger slid into a corner (0 TL, 1 TR, 2 BR, 3 BL) while held. */
    onSlide: ((Int) -> Unit)? = null,
    /** Fired when a long press is released, with the corner (-1 = no move). */
    onLongRelease: ((Int) -> Unit)? = null,
    /** Horizontal drag moves the caret instead of typing. Used by the space bar. */
    cursorSwipe: Boolean = false,
    onCursor: (Int) -> Unit = {},
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
            repeatOnHold, onHaptic, onLongPress, onSlide, onLongRelease, cursorSwipe, onCursor,
            onPress, content,
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
    onLongPress: (() -> Unit)?,
    onSlide: ((Int) -> Unit)?,
    onLongRelease: ((Int) -> Unit)?,
    cursorSwipe: Boolean,
    onCursor: (Int) -> Unit,
    onPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    // A drag owns the pointer: it must kill any pending long press so sliding
    // the space bar never opens the clipboard tray by accident.
    var dragging by remember { mutableStateOf(false) }
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
            .pointerInput(repeatOnHold, cursorSwipe) {
                if (cursorSwipe) {
                    // The space bar is a trackpad: every 18 px of travel is one
                    // caret step, and a tap still types a space.
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { acc = 0f; pressed = true; dragging = true },
                        onDragEnd = { pressed = false; dragging = false },
                        onDragCancel = { pressed = false; dragging = false },
                    ) { change, drag ->
                        change.consume()
                        acc += drag.x
                        val step = 18f
                        while (kotlin.math.abs(acc) >= step) {
                            val dir = if (acc > 0) 1 else -1
                            onCursor(dir)
                            currentHaptic()
                            acc -= dir * step
                        }
                    }
                }
            }
            .pointerInput(repeatOnHold, cursorSwipe, onLongPress != null) {
                // One press at a time, held end to end inside a single
                // pointer-event scope: a finger that brushes or slides off
                // stays silent, a long press grows the key, and the release
                // either commits the slid-into corner or the key itself.
                while (true) {
                    awaitPointerEventScope {
                        val start = awaitFirstDown(requireUnconsumed = false).position
                        pressed = true
                        var longFired = false
                        var repeated = false
                        var quad = -1
                        var cancelled = false
                        val slop = size.width.coerceAtMost(size.height) * 0.5f
                        val job = scope.launch {
                            if (repeatOnHold) {
                                delay(400)
                                repeated = true
                                currentPress()
                                while (isActive) {
                                    delay(50)
                                    currentPress()
                                }
                            } else if (onLongPress != null) {
                                delay(
                                    android.view.ViewConfiguration.getLongPressTimeout().toLong(),
                                )
                                longFired = true
                                currentHaptic()
                                onLongPress.invoke()
                            }
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull() ?: break
                                if (!ch.pressed) break
                                if (!longFired && dragging) {
                                    cancelled = true
                                    break
                                }
                                if (!longFired) {
                                    val moved = kotlin.math.hypot(
                                        ch.position.x - start.x,
                                        ch.position.y - start.y,
                                    )
                                    if (moved > slop) {
                                        cancelled = true
                                        break
                                    }
                                }
                                if (longFired && onSlide != null) {
                                    val dx = ch.position.x - start.x
                                    val dy = ch.position.y - start.y
                                    // Dead zone so a jitter does not flick the choice.
                                    if (kotlin.math.abs(dx) > size.width * 0.10f ||
                                        kotlin.math.abs(dy) > size.height * 0.10f
                                    ) {
                                        val q = when {
                                            dx < 0 && dy < 0 -> 0
                                            dx >= 0 && dy < 0 -> 1
                                            dx >= 0 && dy >= 0 -> 2
                                            else -> 3
                                        }
                                        if (q != quad) {
                                            quad = q
                                            onSlide(q)
                                        }
                                    }
                                }
                            }
                        } finally {
                            job.cancel()
                            pressed = false
                        }
                        when {
                            cancelled -> Unit
                            longFired -> onLongRelease?.invoke(quad) ?: currentPress()
                            // A held repeat already fired; releasing must not
                            // add one more strike of its own.
                            repeated -> Unit
                            else -> currentPress()
                        }
                    }
                }
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
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val base = LocalDensity.current
        val byWidth = maxWidth.value / KeyboardCanvas.WIDTH_VALUE
        val natural = KeyboardCanvas.contentHeight(style, showToolbar, maxWidth.value < 350f)
        // Fit, never squash. When the box is shorter than the board wants, the
        // whole keyboard scales down uniformly instead of the last row being
        // compressed to make up the difference.
        val byHeight =
            if (maxHeight.value.isFinite() && maxHeight.value > 0f) maxHeight.value / natural
            else byWidth
        val scale = minOf(byWidth, byHeight)
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, base.fontScale),
        ) {
            RekeyKeyboard(
                palette = palette,
                modifier = Modifier.width(KeyboardCanvas.WIDTH),
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

/** One shared popup positioner: centred on the anchor, clamped to the screen. */
private class CenteredAnchor(val anchor: Rect) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = 6
        val x = (anchor.center.x.roundToInt() - popupContentSize.width / 2)
            .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val y = (anchor.top.roundToInt() - popupContentSize.height - 10)
            .coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        return IntOffset(x, y)
    }
}

private fun panelShape() = RoundedCornerShape(14.dp)
private val PanelBg = Color(0xF2303134)

/** Copy / cut / paste / select-all, one long press on the space bar away. */
@Composable
private fun KeyToolsPopup(
    anchor: Rect,
    onAction: (KeyAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(anchor) { CenteredAnchor(anchor) },
        onDismissRequest = onDismiss,
    ) {
        Row(
            Modifier
                .clip(panelShape())
                .background(PanelBg)
                .border(1.dp, Color.White.copy(alpha = 0.12f), panelShape())
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                Glyph.SELECT_ALL to KeyAction.SELECT_ALL,
                Glyph.COPY to KeyAction.COPY,
                Glyph.CUT to KeyAction.CUT,
                Glyph.CLIPBOARD to KeyAction.PASTE,
            ).forEach { (g, a) ->
                Box(
                    Modifier
                        .size(46.dp, 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onAction(a) },
                    contentAlignment = Alignment.Center,
                ) {
                    RekeyGlyph(g, Color.White, Modifier.size(19.dp))
                }
            }
        }
    }
}

/** The clipboard history, inside the keyboard: tap to paste, pin, remove. */
@Composable
private fun ClipPanelPopup(
    anchor: Rect,
    items: List<ClipEntry>,
    onPick: (String) -> Unit,
    onPin: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(anchor) { CenteredAnchor(anchor) },
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .width(250.dp)
                .heightIn(max = 210.dp)
                .clip(panelShape())
                .background(PanelBg)
                .border(1.dp, Color.White.copy(alpha = 0.12f), panelShape())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Clipboard",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            )
            if (items.isEmpty()) {
                Text(
                    "Nothing copied yet.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onPick(item.text) }
                                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item.text.replace('\\n', ' '),
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.pinned) Color(0xFF4C8DF6) else Color.Transparent,
                                    )
                                    .clickable { onPin(item.text) },
                                contentAlignment = Alignment.Center,
                            ) {
                                RekeyGlyph(
                                    Glyph.PIN,
                                    if (item.pinned) Color.White else Color.White.copy(alpha = 0.7f),
                                    Modifier.size(14.dp),
                                )
                            }
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemove(item.text) },
                                contentAlignment = Alignment.Center,
                            ) {
                                RekeyGlyph(Glyph.CROSS, Color.White.copy(alpha = 0.7f), Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The language chooser, opened from the pill's globe. */
@Composable
private fun LanguagePopup(
    anchor: Rect,
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(anchor) { CenteredAnchor(anchor) },
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .width(210.dp)
                .clip(panelShape())
                .background(PanelBg)
                .border(1.dp, Color.White.copy(alpha = 0.12f), panelShape())
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "Keyboard language",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
            )
            TopLanguages.forEach { l ->
                val on = l.tag == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) Color(0xFF4C8DF6).copy(alpha = 0.35f) else Color.Transparent)
                        .clickable { onPick(l.tag) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        l.native,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(l.english, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    if (on) {
                        Spacer(Modifier.width(6.dp))
                        RekeyGlyph(Glyph.CHECK, Color(0xFF4C8DF6), Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

/**
 * The grown key: the pressed key redrawn crisp above the frosted board, in
 * place, with its four hints enlarged and the slid-into corner highlighted.
 */
@Composable
private fun HintOverlay(
    hint: HintOpen,
    quad: Int,
    ui: Ui,
    p: KeyboardPalette,
    board: Rect,
) {
    val density = LocalDensity.current
    val grow = 1.55f
    val w = hint.rect.width * grow
    val h = hint.rect.height * grow
    val cx = hint.rect.left + hint.rect.width / 2f - board.left
    val cy = hint.rect.top + hint.rect.height / 2f - board.top
    val margin = with(density) { 4.dp.toPx() }
    val left = if (w + margin * 2 <= board.width) {
        (cx - w / 2f).coerceIn(margin, board.width - w - margin)
    } else (board.width - w) / 2f
    val top = if (h <= board.height) {
        (cy - h / 2f).coerceIn(0f, board.height - h)
    } else 0f
    if (board.width <= 0f || board.height <= 0f) return

    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() })
            .graphicsLayer { shadowElevation = with(density) { 10.dp.toPx() } }
            .clip(ui.keyShape)
            .background(ui.keyBg)
            .then(
                if (ui.borderCol != null && ui.borderW > 0.dp) {
                    Modifier.border(ui.borderW, ui.borderCol, ui.keyShape)
                } else Modifier,
            )
            .zIndex(10f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = hint.label,
            color = ui.textColor,
            fontSize = (ui.fs * 1.35f).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        hint.options.forEachIndexed { i, opt ->
            if (opt.isNullOrEmpty()) return@forEachIndexed
            val on = i == quad
            Box(
                Modifier
                    .align(
                        when (i) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopEnd
                            2 -> Alignment.BottomEnd
                            else -> Alignment.BottomStart
                        },
                    )
                    .padding(3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (on) p.enter else Color.Transparent)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = opt,
                    color = if (on) ui.textColor else ui.textColor.copy(alpha = 0.55f),
                    fontSize = (ui.fs * 0.62f).sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** "Float over other apps?" — asked in the keyboard, allowed in Settings. */
@Composable
private fun FloatPermissionBar(onAllow: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xF2303134))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Float over other apps?",
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Not now",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Text(
            "Allow",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF4C8DF6))
                .clickable { onAllow() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
