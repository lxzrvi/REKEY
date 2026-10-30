package io.github.lxzrvi.rekey.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.data.KeyboardPalette
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph

/**
 * The keyboard is authored once on a fixed 440 x 266 dp canvas so a preset card
 * and the real input method render exactly the same geometry. Cards scale the
 * whole canvas down; the input method scales it to the device width.
 */
object KeyboardCanvas {
    val WIDTH: Dp = 440.dp
    val HEIGHT: Dp = 266.dp
    const val RATIO = 440f / 266f
}

private val Outer = 7.dp
private val BarHeight = 40.dp
private val Gap = 8.dp
private val KeyRadius = 7.dp

private val Letters1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val Letters2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val Letters3 = listOf("z", "x", "c", "v", "b", "n", "m")
private val Symbols1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val Symbols2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@")
private val Symbols3 = listOf(".", ",", "?", "!", "'", "\"", "+")

/** What the editor and the input method need to know about a tap. */
enum class KeyAction { TEXT, SPACE, ENTER, BACKSPACE, LANGUAGE }

/**
 * The full REKEY keyboard: toolbar, three letter rows and the bottom row.
 *
 * [onKey] defaults to a no-op so the same composable doubles as a static
 * preview inside a preset card.
 */
@Composable
fun RekeyKeyboard(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    onKey: (KeyAction, String) -> Unit = { _, _ -> },
) {
    var shifted by remember { mutableStateOf(false) }
    var symbols by remember { mutableStateOf(false) }

    fun face(ch: String) = if (shifted && !symbols) ch.uppercase() else ch

    Column(
        modifier = modifier
            .requiredSize(KeyboardCanvas.WIDTH, KeyboardCanvas.HEIGHT)
            .background(palette.canvas)
            .padding(Outer),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        KeyboardToolbar(palette)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.board)
                .padding(horizontal = 6.dp, vertical = 7.dp),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyRow {
                    (if (symbols) Symbols1 else Letters1).forEach { ch ->
                        LetterKey(face(ch), palette, Modifier.weight(1f)) {
                            onKey(KeyAction.TEXT, face(ch))
                        }
                    }
                }
                KeyRow {
                    Box(Modifier.weight(0.5f))
                    (if (symbols) Symbols2 else Letters2).forEach { ch ->
                        LetterKey(face(ch), palette, Modifier.weight(1f)) {
                            onKey(KeyAction.TEXT, face(ch))
                        }
                    }
                    Box(Modifier.weight(0.5f))
                }
                KeyRow {
                    SpecialKey(
                        palette = palette,
                        modifier = Modifier.weight(1.45f),
                        active = shifted,
                        onClick = { shifted = !shifted },
                    ) {
                        RekeyGlyph(
                            glyph = Glyph.SHIFT,
                            color = if (shifted) palette.enter else palette.text,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    (if (symbols) Symbols3 else Letters3).forEach { ch ->
                        LetterKey(face(ch), palette, Modifier.weight(1f)) {
                            onKey(KeyAction.TEXT, face(ch))
                        }
                    }
                    SpecialKey(
                        palette = palette,
                        modifier = Modifier.weight(1.45f),
                        onClick = { onKey(KeyAction.BACKSPACE, "") },
                    ) {
                        RekeyGlyph(Glyph.BACKSPACE, palette.text, Modifier.size(19.dp))
                    }
                }
                KeyRow {
                    SpecialKey(
                        palette = palette,
                        modifier = Modifier.weight(1.45f),
                        onClick = { symbols = !symbols; shifted = false },
                    ) {
                        Text(
                            text = if (symbols) "ABC" else "?123",
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    SpecialKey(
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(KeyAction.LANGUAGE, "") },
                    ) {
                        RekeyGlyph(Glyph.GLOBE, palette.text, Modifier.size(18.dp))
                    }
                    Box(
                        Modifier
                            .weight(4.6f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(KeyRadius))
                            .background(palette.key)
                            .clickableKey { onKey(KeyAction.SPACE, " ") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("space", color = palette.text.copy(alpha = 0.55f), fontSize = 12.sp)
                    }
                    Box(
                        Modifier
                            .weight(2.1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(KeyRadius))
                            .background(palette.enter)
                            .clickableKey { onKey(KeyAction.ENTER, "\n") },
                        contentAlignment = Alignment.Center,
                    ) {
                        RekeyGlyph(Glyph.ENTER, palette.enterText, Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardToolbar(palette: KeyboardPalette) {
    Row(
        Modifier.fillMaxWidth().height(BarHeight).padding(horizontal = 8.dp),
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
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(KeyRadius))
            .background(palette.key)
            .clickableKey(onClick),
        contentAlignment = Alignment.Center,
    ) {
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
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(KeyRadius))
            .background(if (active) palette.key else palette.special)
            .clickableKey(onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Keys react to taps but never show a ripple: the preview must stay flat. */
private fun Modifier.clickableKey(onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, onClick = onClick)

/**
 * Scales the fixed keyboard canvas down to whatever width the caller gives it.
 * Used by preset cards, the editor and the setup screen.
 */
@Composable
fun KeyboardPreview(
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(cornerRadius))) {
        val scale = maxWidth.value / KeyboardCanvas.WIDTH.value
        Box(
            Modifier.fillMaxSize().background(palette.canvas),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                Modifier
                    .requiredSize(KeyboardCanvas.WIDTH, KeyboardCanvas.HEIGHT)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                RekeyKeyboard(palette = palette)
            }
        }
    }
}
