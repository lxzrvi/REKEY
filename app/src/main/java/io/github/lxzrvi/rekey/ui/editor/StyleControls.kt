package io.github.lxzrvi.rekey.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Rounded group holding one set of related controls. */
@Composable
internal fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.9.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
internal fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    unit: String = "",
    decimals: Int = 0,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = format(value, decimals) + unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ThinSlider(
            value = value.coerceIn(range.start, range.endInclusive),
            range = range,
            onChange = onChange,
        )
    }
}

private fun format(v: Float, decimals: Int): String =
    if (decimals <= 0) v.roundToInt().toString()
    else {
        val f = Math.pow(10.0, decimals.toDouble()).toFloat()
        ((v * f).roundToInt() / f).toString()
    }

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A row of mutually exclusive choices, used for column counts and similar. */
@Composable
internal fun <T> SegmentRow(
    label: String,
    options: List<T>,
    selected: T,
    title: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Row(
            Modifier.fillMaxWidth().height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { o ->
                val on = o == selected
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(shape)
                        .background(
                            if (on) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable { onSelect(o) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title(o),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * A colour that can also be "inherit". Null shows a hatched swatch and means the
 * part follows the base palette, which is what keeps a preset coherent.
 */
@Composable
internal fun ColorRow(
    label: String,
    value: Color?,
    fallback: Color,
    onChange: (Color?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (value == null) "Auto" else hex(value),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (value == null) MaterialTheme.colorScheme.surfaceVariant else value)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (value == null) {
                Box(Modifier.size(14.dp, 1.5.dp).background(fallback.copy(alpha = 0.7f)))
            }
        }
    }
    if (open) {
        ColorWheelDialog(
            title = label,
            initial = value ?: fallback,
            onDismiss = { open = false },
            onInherit = { onChange(null); open = false },
            onPick = { onChange(it); open = false },
        )
    }
}

internal fun hex(c: Color): String {
    val a = (c.alpha * 255).roundToInt()
    val r = (c.red * 255).roundToInt()
    val g = (c.green * 255).roundToInt()
    val b = (c.blue * 255).roundToInt()
    return "#" + listOf(r, g, b, a).joinToString("") {
        it.coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
    }
}

/**
 * A thin track with no thumb.
 *
 * The filled portion is the only indicator, which keeps a page of a dozen
 * sliders calm instead of a column of Material lollipops. The touch target is
 * still 28 dp tall; only the painted track is thin.
 */
@Composable
internal fun ThinSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    val filled = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    fun emit(x: Float, width: Int) {
        val f = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
        onChange(range.start + f * span)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(range) {
                detectTapGestures { emit(it.x, size.width) }
            }
            .pointerInput(range) {
                detectDragGestures(
                    onDragStart = { emit(it.x, size.width) },
                    onDrag = { change, _ ->
                        change.consume()
                        emit(change.position.x, size.width)
                    },
                )
            },
    ) {
        val y = size.height / 2f
        val h = 3.dp.toPx()
        val r = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
        drawRoundRect(
            track,
            topLeft = Offset(0f, y - h / 2f),
            size = androidx.compose.ui.geometry.Size(size.width, h),
            cornerRadius = r,
        )
        drawRoundRect(
            filled,
            topLeft = Offset(0f, y - h / 2f),
            size = androidx.compose.ui.geometry.Size(size.width * frac, h),
            cornerRadius = r,
        )
    }
}

/** Transparency on its own, for parts whose colour is set elsewhere. */
@Composable
internal fun AlphaRow(
    label: String,
    value: Float,
    tint: Color,
    onChange: (Float) -> Unit,
) {
    GradientSlider(
        label = label,
        value = value,
        colors = listOf(Color.Transparent, tint.copy(alpha = 1f)),
        onChange = onChange,
        valueText = "${(value * 100).roundToInt()}%",
    )
}

/** Two-stop gradient, both stops edited in one dialog. */
@Composable
internal fun GradientRow(
    label: String,
    start: Color,
    end: Color?,
    angle: Float,
    onChange: (Color, Color?, Float) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (end == null) "Off" else "${angle.roundToInt()}°",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(44.dp, 26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (end == null) SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                    else Brush.linearGradient(listOf(start, end)),
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        )
    }
    if (open) {
        GradientDialog(
            start = start,
            end = end ?: start.copy(alpha = 0.35f),
            angle = angle,
            onDismiss = { open = false },
            onPick = { x, y, d -> onChange(x, y, d); open = false },
        )
    }
}
