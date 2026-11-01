package io.github.lxzrvi.rekey.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** HSV kept as four plain floats so the wheel never fights the colour it edits. */
internal class Hsv(h: Float, s: Float, v: Float, a: Float) {
    var h by mutableFloatStateOf(h)
    var s by mutableFloatStateOf(s)
    var v by mutableFloatStateOf(v)
    var a by mutableFloatStateOf(a)

    val color: Color
        get() {
            val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
            return Color(argb).copy(alpha = a)
        }
}

internal fun hsvFrom(c: Color): Hsv {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            (c.alpha * 255).roundToInt(),
            (c.red * 255).roundToInt(),
            (c.green * 255).roundToInt(),
            (c.blue * 255).roundToInt(),
        ),
        out,
    )
    return Hsv(out[0], out[1], out[2], c.alpha)
}

/**
 * Hue around the circle, saturation from the centre out.
 *
 * A sweep gradient supplies the hue and a white-to-transparent radial gradient
 * on top supplies the saturation, which is the same construction the platform
 * colour pickers use. Brightness is a separate slider because a wheel cannot
 * show three dimensions at once.
 */
@Composable
internal fun ColorWheel(
    hsv: Hsv,
    modifier: Modifier = Modifier,
) {
    val hues = remember {
        (0..12).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 30f, 1f, 1f))) }
    }
    val ring = MaterialTheme.colorScheme.onSurface

    fun pick(offset: Offset, size: androidx.compose.ui.unit.IntSize) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy)
        val dx = offset.x - cx
        val dy = offset.y - cy
        // atan2 gives 0 at three o'clock; sweepGradient also starts there, so
        // the angle maps straight onto hue with no offset.
        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (deg < 0) deg += 360f
        hsv.h = deg % 360f
        hsv.s = (hypot(dx, dy) / r).coerceIn(0f, 1f)
    }

    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { pick(it, size) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pick(it, size) },
                    onDrag = { change, _ ->
                        change.consume()
                        pick(change.position, size)
                    },
                )
            },
    ) {
        val r = min(size.width, size.height) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.sweepGradient(hues, c), r, c)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), c, r), r, c)
        // Brightness is previewed by darkening the whole disc.
        if (hsv.v < 1f) drawCircle(Color.Black.copy(alpha = 1f - hsv.v), r, c)

        val rad = Math.toRadians(hsv.h.toDouble())
        val px = c.x + cos(rad).toFloat() * r * hsv.s
        val py = c.y + sin(rad).toFloat() * r * hsv.s
        drawCircle(Color.White, r * 0.075f, Offset(px, py), style = Stroke(3f))
        drawCircle(ring.copy(alpha = 0.55f), r * 0.075f + 2f, Offset(px, py), style = Stroke(1.5f))
    }
}

/** Horizontal gradient strip with a thin indicator, used for value and alpha. */
@Composable
internal fun GradientSlider(
    label: String,
    value: Float,
    colors: List<Color>,
    onChange: (Float) -> Unit,
    valueText: String,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        val shape = RoundedCornerShape(8.dp)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(shape)
                .pointerInput(Unit) {
                    detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onChange((it.x / size.width).coerceIn(0f, 1f)) },
                        onDrag = { change, _ ->
                            change.consume()
                            onChange((change.position.x / size.width).coerceIn(0f, 1f))
                        },
                    )
                },
        ) {
            drawRect(Brush.horizontalGradient(colors))
            drawRoundRect(outline, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f), style = Stroke(1f))
            val x = size.width * value.coerceIn(0f, 1f)
            drawLine(Color.White, Offset(x, 2f), Offset(x, size.height - 2f), strokeWidth = 3f)
            drawLine(Color.Black.copy(alpha = 0.45f), Offset(x, 2f), Offset(x, size.height - 2f), strokeWidth = 1f)
        }
    }
}

/** Small checkerboard so transparency reads as transparency, not as black. */
@Composable
internal fun AlphaPreview(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val step = size.minDimension / 6f
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                drawRect(
                    if ((row + col) % 2 == 0) Color(0xFFDDDDDD) else Color(0xFFF7F7F7),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(min(step, size.width - x), min(step, size.height - y)),
                )
                x += step
                col++
            }
            y += step
            row++
        }
        drawRect(color)
    }
}

/**
 * The full picker: wheel, brightness, transparency, hex entry.
 *
 * [onInherit] is null when the colour cannot be inherited, which hides the
 * button instead of offering a choice that would do nothing.
 */
@Composable
internal fun ColorWheelDialog(
    title: String,
    initial: Color,
    onDismiss: () -> Unit,
    onInherit: (() -> Unit)?,
    onPick: (Color) -> Unit,
) {
    val hsv = remember { hsvFrom(initial) }
    var hexText by remember { mutableStateOf(hexRgb(hsv.color)) }
    var hexDirty by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ColorWheel(hsv, Modifier.fillMaxWidth().padding(horizontal = 12.dp))

            GradientSlider(
                label = "Brightness",
                value = hsv.v,
                colors = listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.h, hsv.s, 1f)))),
                onChange = { hsv.v = it; hexDirty = false },
                valueText = "${(hsv.v * 100).roundToInt()}%",
            )
            GradientSlider(
                label = "Transparency",
                value = hsv.a,
                colors = listOf(Color.Transparent, hsv.color.copy(alpha = 1f)),
                onChange = { hsv.a = it; hexDirty = false },
                valueText = "${(hsv.a * 100).roundToInt()}%",
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AlphaPreview(
                    hsv.color,
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(10.dp))
                val fieldShape = RoundedCornerShape(10.dp)
                BasicTextField(
                    value = if (hexDirty) hexText else hexRgb(hsv.color),
                    onValueChange = { raw ->
                        hexText = raw
                        hexDirty = true
                        parseHexLoose(raw)?.let { parsed ->
                            val next = hsvFrom(parsed)
                            hsv.h = next.h; hsv.s = next.s; hsv.v = next.v
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .clip(fieldShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onInherit != null) {
                    TextButton(onClick = onInherit) { Text("Auto", fontSize = 12.sp) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 12.sp) }
                TextButton(onClick = { onPick(hsv.color) }) {
                    Text("Okay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Both gradient stops in one dialog.
 *
 * The owner flips between Start and End without closing anything, so a gradient
 * is a single decision instead of two separate trips through the picker.
 */
@Composable
internal fun GradientDialog(
    start: Color,
    end: Color,
    angle: Float,
    onDismiss: () -> Unit,
    onPick: (Color, Color, Float) -> Unit,
) {
    val a = remember { hsvFrom(start) }
    val b = remember { hsvFrom(end) }
    var which by remember { mutableStateOf(0) }
    var deg by remember { mutableFloatStateOf(angle) }
    val active = if (which == 0) a else b

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Gradient",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(a.color, b.color))),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Start" to a, "End" to b).forEachIndexed { i, (name, model) ->
                    val on = which == i
                    val s = RoundedCornerShape(11.dp)
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(s)
                            .background(
                                if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .border(
                                if (on) 1.5.dp else 1.dp,
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                s,
                            )
                            .clickable { which = i }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AlphaPreview(model.color, Modifier.size(20.dp).clip(CircleShape))
                        Text(
                            name,
                            fontSize = 12.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            ColorWheel(active, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            GradientSlider(
                "Brightness", active.v,
                listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(active.h, active.s, 1f)))),
                { active.v = it }, "${(active.v * 100).roundToInt()}%",
            )
            GradientSlider(
                "Transparency", active.a,
                listOf(Color.Transparent, active.color.copy(alpha = 1f)),
                { active.a = it }, "${(active.a * 100).roundToInt()}%",
            )
            SliderRow("Angle", deg, 0f..360f, { deg = it }, "°")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 12.sp) }
                TextButton(onClick = { onPick(a.color, b.color, deg) }) {
                    Text("Okay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

internal fun hexRgb(c: Color): String {
    val r = (c.red * 255).roundToInt().coerceIn(0, 255)
    val g = (c.green * 255).roundToInt().coerceIn(0, 255)
    val b = (c.blue * 255).roundToInt().coerceIn(0, 255)
    return "#" + listOf(r, g, b).joinToString("") { it.toString(16).padStart(2, '0').uppercase() }
}

/** Accepts #RGB, #RRGGBB and #RRGGBBAA, with or without the hash. */
internal fun parseHexLoose(raw: String): Color? {
    val t = raw.trim().removePrefix("#")
    if (t.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return null
    return runCatching {
        when (t.length) {
            3 -> Color(android.graphics.Color.parseColor("#" + t.map { "$it$it" }.joinToString("")))
            6 -> Color(android.graphics.Color.parseColor("#$t"))
            8 -> {
                val rgb = android.graphics.Color.parseColor("#${t.substring(0, 6)}")
                Color(rgb).copy(alpha = t.substring(6, 8).toInt(16) / 255f)
            }
            else -> null
        }
    }.getOrNull()
}
