package io.github.lxzrvi.rekey.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** How long a notice stays up before it fades away on its own. */
private const val NoticeMillis = 2400L

/**
 * The single place every in-app message appears.
 *
 * One pill at the bottom of the page carries all of them — refused removals,
 * added layouts, applied layouts, resets. [show] replaces whatever is on screen
 * and restarts the timer, so a burst of actions never stacks up a queue of
 * toasts the owner has to wait through.
 */
@Stable
class NoticeState {
    var message by mutableStateOf<String?>(null)
        private set

    /** Bumped on every [show] so the dismiss timer can tell it has been replaced. */
    var token by mutableIntStateOf(0)
        private set

    fun show(text: String) {
        if (text.isBlank()) return
        message = text
        token += 1
    }

    fun clear() {
        message = null
    }
}

@Composable
fun rememberNoticeState(): NoticeState = remember { NoticeState() }

/**
 * Bottom-anchored frosted pill. Deliberately not a Haze effect: it is drawn above
 * the page's own blur source, and an effect node may not blur an ancestor source.
 * A dense surface reads the same and cannot flicker on its first frame.
 */
@Composable
fun NoticePill(
    state: NoticeState,
    modifier: Modifier = Modifier,
) {
    val visible = state.message != null
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 220 else 180,
            easing = FastOutSlowInEasing,
        ),
        label = "notice",
    )

    // Keep the last text while fading out, otherwise the pill empties before it
    // has finished leaving.
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(state.token) {
        val text = state.message ?: return@LaunchedEffect
        shown = text
        val mine = state.token
        delay(NoticeMillis)
        if (state.token == mine) state.clear()
    }

    if (progress <= 0.001f) return

    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 18.dp.toPx()
            }
            .widthIn(max = 340.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
            .padding(horizontal = 18.dp, vertical = 11.dp)
            .testTag("notice_pill"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = shown,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
