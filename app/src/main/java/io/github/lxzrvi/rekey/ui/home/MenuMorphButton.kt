package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lxzrvi.rekey.R
import kotlin.math.cos
import kotlin.math.sin

/** Shared three-dot/X control. When open it sits inside the drawer's left edge. */
@Composable
fun MenuMorphButton(openProgress: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(if (openProgress > 0.5f) R.string.close_appearance else R.string.open_appearance)
    val iconColor = MaterialTheme.colorScheme.onBackground
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier.size(48.dp).testTag("menu_toggle").semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(24.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val p = openProgress.coerceIn(0f, 1f)
                val spread = 7.dp.toPx() * (1f - p)
                val half = 8.dp.toPx() * p
                val stroke = 2.4.dp.toPx()
                if (p < 0.04f) {
                    listOf(-1f, 0f, 1f).forEach { drawCircle(iconColor, radius = 1.9.dp.toPx(), center = c + Offset(0f, it * 7.dp.toPx())) }
                } else {
                    drawCircle(iconColor.copy(alpha = 1f - p), radius = 1.9.dp.toPx() * (1f - p), center = c)
                    listOf(-1f to 45f, 1f to -45f).forEach { (side, degrees) ->
                        val center = c + Offset(0f, side * spread)
                        val angle = Math.toRadians((degrees * p).toDouble())
                        val dx = (cos(angle) * half).toFloat()
                        val dy = (sin(angle) * half).toFloat()
                        drawLine(iconColor, center - Offset(dx, dy), center + Offset(dx, dy), stroke, StrokeCap.Round)
                    }
                }
            }
        }
    }
}
