package io.github.lxzrvi.rekey.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.data.KeyboardPalette
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph

/**
 * The keyboard is authored once on a fixed-width canvas so a preset card and the
 * real input method draw exactly the same geometry.
 *
 * The key board is always 204 dp tall. Only the strip above it changes: the IME
 * shows its toolbar there, a preset card leaves it empty and floats its own
 * frosted "Layout N" / X controls over it. That is what lets the card *be* the
 * keyboard instead of holding a small picture of one.
 */
object KeyboardCanvas {
    val WIDTH: Dp = 440.dp

    /** Toolbar strip (IME) + board + outer padding. */
    val HEIGHT: Dp = 266.dp

    /** Taller strip (preset card) + the same board + outer padding. */
    val CARD_HEIGHT: Dp = 298.dp

    const val RATIO = 440f / 266f
    const val CARD_RATIO = 440f / 298f

    /** Outer canvas padding, shared with the card header placement. */
    const val OUTER = 7f
}

private val Outer = KeyboardCanvas.OUTER.dp
private val BoardHeight = 204.dp
private val Gap = 8.dp
private val KeyRadius = 7.dp
val ImeToolbarHeight: Dp = 40.dp
val CardHeaderHeight: Dp = 72.dp

private val Letters1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val Letters2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val Letters3 = listOf("z", "x", "c", "v", "b", "n", "m")
private val Symbols1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val Symbols2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@")
private val Symbols3 = listOf(".", ",", "?", "!", "'", "\"", "+")

/** What the input method needs to know about a tap. */
enum class KeyAction { TEXT, SPACE, ENTER, BACKSPACE, LANGUAGE }

/**
 * The full REKEY keyboard.
 *
 * [showToolbar] false leaves the top strip empty so a caller can overlay its own
 * controls; [interactive] false disables taps and haptics for a static preview.
 */
@Composable
fun RekeyKeyboard(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    headerHeight: Dp = ImeToolbarHeight,
    interactive: Boolean = true,
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
) {
    var shifted by rememberSaveable { mutableStateOf(false) }
    var symbols by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    fun emit(action: KeyAction, text: String) {
        if (!interactive) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onKey(action, text)
        // A shifted letter types once, then drops back — like a real keyboard.
        if (action == KeyAction.TEXT && shifted && !symbols) shifted = false
    }

    fun face(ch: String) = if (shifted && !symbols) ch.uppercase() else ch

    Column(
        modifier = modifier
            .requiredSize(KeyboardCanvas.WIDTH, headerHeight + Gap + BoardHeight + Outer * 2)
            .background(palette.canvas)
            .padding(Outer),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        if (showToolbar) {
            KeyboardToolbar(palette, headerHeight)
        } else {
            Box(Modifier.fillMaxWidth().height(headerHeight))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(BoardHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.board)
                .padding(horizontal = 6.dp, vertical = 7.dp),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyRow {
                    (if (symbols) Symbols1 else Letters1).forEach { ch ->
                        LetterKey(face(ch), palette, Modifier.weight(1f), interactive) {
                            emit(KeyAction.TEXT, face(ch))
                        }
                    }
                }
                KeyRow {
                    Box(Modifier.weight(0.5f))
                    (if (symbols) Symbols2 else Letters2).forEach { ch ->
                        LetterKey(face(ch), palette, Modifier.weight(1f), interactive) {
                            emit(KeyAction.TEXT, face(ch))
                        }
                    }
                    Box(Modifier.weight(0.5f))
                }
                KeyRow {
                    SpecialKey(palette, Modifier.weight(1.45f), interactive, active = shifted, onClick = {
                        if (interactive) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            shifted = !shifted
                        }
                    }) {
                        RekeyGlyph(
                            glyph = Glyph.SHIFT,
                            color = if (shifted) palette.enter else palette.text,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    (if (symbols) Symbols3 else Letters3).forEach { ch ->
                        LetterKey(face(ch), palette, Modifier.weight(1f), interactive) {
                            emit(KeyAction.TEXT, face(ch))
                        }
                    }
                    SpecialKey(palette, Modifier.weight(1.45f), interactive, onClick = {
                        emit(KeyAction.BACKSPACE, "")
                    }) {
                        RekeyGlyph(Glyph.BACKSPACE, palette.text, Modifier.size(19.dp))
                    }
                }
                KeyRow {
                    SpecialKey(palette, Modifier.weight(1.45f), interactive, onClick = {
                        if (interactive) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            symbols = !symbols
                            shifted = false
                        }
                    }) {
                        Text(
                            text = if (symbols) "ABC" else "?123",
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    SpecialKey(palette, Modifier.weight(1f), interactive, onClick = {
                        emit(KeyAction.LANGUAGE, "")
                    }) {
                        RekeyGlyph(Glyph.GLOBE, palette.text, Modifier.size(18.dp))
                    }
                    WideKey(palette.key, Modifier.weight(4.6f), interactive, { emit(KeyAction.SPACE, " ") }) {
                        Text("space", color = palette.text.copy(alpha = 0.55f), fontSize = 12.sp)
                    }
                    WideKey(palette.enter, Modifier.weight(2.1f), interactive, { emit(KeyAction.ENTER, "\n") }) {
                        RekeyGlyph(Glyph.ENTER, palette.enterText, Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardToolbar(palette: KeyboardPalette, height: Dp) {
    Row(
        Modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        RekeyGlyph(Glyph.PASTE, palette.icon, Modifier.size(21.dp))
        RekeyGlyph(Glyph.TRANSLATE, palette.icon, Modifier.size(21.dp))
        RekeyGlyph(Glyph.PALETTE, palette.icon, Modifier.size(21.dp))
        RekeyGlyph(Glyph.MORE, palette.icon, Modifier.size(21.dp))
        Box(Modifier.weight(1f))
        RekeyGlyph(Glyph.CHEVRON_DOWN, palette.icon, Modifier.size(21.dp))
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun LetterKey(
    label: String,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onClick: () -> Unit,
) {
    WideKey(palette.key, modifier, interactive, onClick) {
        Text(
            text = label,
            color = palette.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpecialKey(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    WideKey(if (active) palette.key else palette.special, modifier, interactive, onClick, content)
}

/** One key surface: pressed state darkens it, previews stay completely inert. */
@Composable
private fun WideKey(
    background: Color,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(KeyRadius)
    if (interactive) {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        Box(
            modifier
                .fillMaxHeight()
                .clip(shape)
                .background(if (pressed) background.copy(alpha = 0.72f) else background)
                .clickable(interactionSource = src, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    } else {
        Box(
            modifier.fillMaxHeight().clip(shape).background(background),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

/**
 * Draws the keyboard edge-to-edge at whatever width it is given.
 *
 * The canvas is authored at 440 dp. Instead of drawing it oversized and scaling
 * the render layer — which leaves the layout box wider than the pixels and makes
 * the keyboard drift sideways — this scales the *density*. The keyboard then
 * measures exactly [KeyboardCanvas.WIDTH] logical dp, which is exactly the
 * available physical width, so its left and right edges land on the container's
 * edges by construction. Text stays vector-crisp and no extra layer is allocated.
 */
@Composable
fun ScaledKeyboard(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    headerHeight: Dp = ImeToolbarHeight,
    interactive: Boolean = true,
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
) {
    BoxWithConstraints(modifier) {
        val base = LocalDensity.current
        val scale = maxWidth.value / KeyboardCanvas.WIDTH.value
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, base.fontScale),
        ) {
            RekeyKeyboard(
                palette = palette,
                showToolbar = showToolbar,
                headerHeight = headerHeight,
                interactive = interactive,
                onKey = onKey,
            )
        }
    }
}
