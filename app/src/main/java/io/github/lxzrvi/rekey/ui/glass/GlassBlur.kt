package io.github.lxzrvi.rekey.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Pure background blur: no tint, noise, or painted background in the glass layer. */
private fun pureBlur(radius: Dp) = HazeStyle(
    backgroundColor = Color.Transparent,
    tints = emptyList(),
    blurRadius = radius,
    noiseFactor = 0f,
    fallbackTint = HazeTint(Color.Transparent),
)

@Composable
fun rememberGlassState(): HazeState = remember { HazeState() }

/** Apply to content behind the glass. Source and effect must be siblings, not parent/child. */
fun Modifier.glassSource(state: HazeState): Modifier = hazeSource(state)

@Composable
fun Modifier.glass(
    state: HazeState,
    shape: Shape = RectangleShape,
    blurRadius: Dp = 28.dp,
    enabled: Boolean = true,
): Modifier {
    val clipped = clip(shape)
    return if (enabled) clipped.hazeEffect(state = state, style = pureBlur(blurRadius)) else clipped
}
