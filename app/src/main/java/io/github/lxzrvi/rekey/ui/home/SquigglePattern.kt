package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.lxzrvi.rekey.design.PatternContract
import kotlin.math.max

/** Squiggle pattern with rounded stroke tips. Light: white bg + black strokes. Dark: black bg + white strokes. */
@Composable
fun SquigglePattern(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    strokeAlpha: Float = 1f,
) {
    val background = if (darkTheme) Color.Black else Color.White
    val strokeColor = (if (darkTheme) Color.White else Color.Black).copy(alpha = strokeAlpha)
    // One combined path per style: the whole pattern is 2 draw calls, not 106.
    val curvePath = remember {
        Path().apply { SquiggleData.CURVES.forEach { addPath(buildSquigglePath(it, smooth = true)) } }
    }
    val zigzagPath = remember {
        Path().apply { SquiggleData.ZIGZAGS.forEach { addPath(buildSquigglePath(it, smooth = false)) } }
    }
    val curveStyle = remember { Stroke(SQUIGGLE_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val zigzagStyle = remember { Stroke(SQUIGGLE_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Miter, miter = 4f) }
    Canvas(modifier.fillMaxSize()) {
        drawRect(background)
        val s = max(size.width / SQUIGGLE_W, size.height / SQUIGGLE_H)
        val dx = (size.width - SQUIGGLE_W * s) / 2f
        val dy = (size.height - SQUIGGLE_H * s) / 2f
        withTransform({
            translate(dx, dy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawPath(curvePath, strokeColor, style = curveStyle)
            drawPath(zigzagPath, strokeColor, style = zigzagStyle)
        }
    }
}

private const val SQUIGGLE_W = PatternContract.DESIGN_WIDTH
private const val SQUIGGLE_H = PatternContract.DESIGN_HEIGHT
private const val SQUIGGLE_STROKE = PatternContract.STROKE

/** "x y x y ..." -> Path. smooth = Catmull-Rom spline, otherwise straight segments. */
private fun buildSquigglePath(data: String, smooth: Boolean): Path {
    val v = data.split(' ').map { it.toFloat() }
    val n = v.size / 2
    val path = Path()
    path.moveTo(v[0], v[1])
    if (!smooth || n < 3) {
        for (i in 1 until n) path.lineTo(v[2 * i], v[2 * i + 1])
        return path
    }
    val closed = n > 3 && v[0] == v[2 * n - 2] && v[1] == v[2 * n - 1]
    val m = if (closed) n - 1 else n
    fun idx(i: Int) = if (closed) ((i % m) + m) % m else i.coerceIn(0, n - 1)
    fun px(i: Int) = v[2 * idx(i)]
    fun py(i: Int) = v[2 * idx(i) + 1]
    for (i in 0 until n - 1) {
        path.cubicTo(
            px(i) + (px(i + 1) - px(i - 1)) / 6f, py(i) + (py(i + 1) - py(i - 1)) / 6f,
            px(i + 1) - (px(i + 2) - px(i)) / 6f, py(i + 1) - (py(i + 2) - py(i)) / 6f,
            px(i + 1), py(i + 1),
        )
    }
    return path
}
