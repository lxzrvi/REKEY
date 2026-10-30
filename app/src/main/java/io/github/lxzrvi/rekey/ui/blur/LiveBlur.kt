package io.github.lxzrvi.rekey.ui.blur

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Pure live blur: no tint, noise or background color. */
fun pureBlur(
    radius: Dp,
    gradient: HazeProgressive? = null,
    enabled: Boolean = true,
): HazeBlurStyle = HazeBlurStyle {
    blurEnabled(enabled)
    blurRadius(radius)
    noiseFactor(0f)
    backgroundColor(Color.Transparent)
    colorEffects(emptyList())
    progressive(gradient)
}

fun topEdgeBlur(radius: Dp, enabled: Boolean = true): HazeBlurStyle = pureBlur(
    radius = radius,
    gradient = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f),
    enabled = enabled,
)

fun bottomEdgeBlur(radius: Dp, enabled: Boolean = true): HazeBlurStyle = pureBlur(
    radius = radius,
    gradient = HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f),
    enabled = enabled,
)

fun fullScreenBlur(radius: Dp, enabled: Boolean = true): HazeBlurStyle = pureBlur(radius, enabled = enabled)

@Composable
fun rememberLiveBlurState(): HazeState = rememberHazeState()

fun Modifier.liveBlurSource(state: HazeState): Modifier = hazeSource(state)

fun Modifier.liveBlur(
    state: HazeState,
    style: HazeBlurStyle,
    performanceMode: HazePerformanceMode = HazePerformanceMode.Quality,
): Modifier = hazeBlur(
    input = HazeInput.Sources(state),
    style = style,
    performanceMode = performanceMode,
)
