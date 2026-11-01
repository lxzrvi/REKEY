package io.github.lxzrvi.rekey.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

/**
 * Long-press alternates, drawn above the key that opened them.
 *
 * This is the Compose port of the supplied LongPressPopup. The original used a
 * PopupWindow and setOnTouchListener, which cannot attach to a Compose key; the
 * behaviour is kept — six per row, nearest cell wins, clamped to the screen —
 * but the popup itself is non-touchable so the key keeps the gesture and the
 * finger never has to be lifted to change selection.
 */
@Composable
internal fun AlternatesPopup(
    anchor: Rect,
    options: List<String>,
    selected: Int,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (options.isEmpty()) return
    val density = LocalDensity.current
    val provider = remember(anchor, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val margin = with(density) { 4.dp.roundToPx() }
                val gap = with(density) { 8.dp.roundToPx() }
                val x = (anchor.center.x.roundToInt() - popupContentSize.width / 2)
                    .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                val y = (anchor.top.roundToInt() - popupContentSize.height - gap)
                    .coerceAtLeast(margin)
                return IntOffset(x, y)
            }
        }
    }
    Popup(popupPositionProvider = provider, onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier
                .clip(shape)
                .background(Color(0xF2303134))
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.chunked(6).forEachIndexed { rowIdx, chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    chunk.forEachIndexed { col, s ->
                        val i = rowIdx * 6 + col
                        val on = i == selected
                        Box(
                            Modifier
                                .size(42.dp, 46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) Color(0xFF4C8DF6) else Color.Transparent)
                                .clickable { onPick(s) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                s,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

