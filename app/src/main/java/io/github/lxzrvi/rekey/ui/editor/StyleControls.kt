package io.github.lxzrvi.rekey.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
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
        ColorPickerDialog(
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

private val Swatches = listOf(
    0xFFFFFFFF, 0xFFF5F5F7, 0xFFE9E9ED, 0xFFD9D9DF,
    0xFF8E8E93, 0xFF55555E, 0xFF303036, 0xFF000000,
    0xFF007AFF, 0xFF0A84FF, 0xFF5267A8, 0xFF8299EF,
    0xFF34C759, 0xFFFF9500, 0xFFFF3B30, 0xFFAF52DE,
    0xFFEEE6D8, 0xFFFFFAF0, 0xFFDED3C0, 0xFF9B7049,
)

@Composable
private fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onInherit: () -> Unit,
    onPick: (Color) -> Unit,
) {
    val hsv = remember {
        FloatArray(3).also {
            android.graphics.Color.colorToHSV(
                android.graphics.Color.argb(
                    (initial.alpha * 255).roundToInt(),
                    (initial.red * 255).roundToInt(),
                    (initial.green * 255).roundToInt(),
                    (initial.blue * 255).roundToInt(),
                ),
                it,
            )
        }
    }
    var h by remember { mutableStateOf(hsv[0]) }
    var s by remember { mutableStateOf(hsv[1]) }
    var v by remember { mutableStateOf(hsv[2]) }
    var a by remember { mutableStateOf(initial.alpha) }
    var text by remember { mutableStateOf(hex(initial)) }

    fun current(): Color {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        return Color(
            red = android.graphics.Color.red(argb) / 255f,
            green = android.graphics.Color.green(argb) / 255f,
            blue = android.graphics.Color.blue(argb) / 255f,
            alpha = a,
        )
    }

    fun sync() { text = hex(current()) }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(18.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Colour", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(10.dp))
                    .background(current())
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Swatches.take(10).forEach { c ->
                    Box(
                        Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp))
                            .background(Color(c))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                val nc = Color(c)
                                h = hsvOf(nc)[0]; s = hsvOf(nc)[1]; v = hsvOf(nc)[2]; a = nc.alpha
                                sync()
                            },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Swatches.drop(10).forEach { c ->
                    Box(
                        Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp))
                            .background(Color(c))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                val nc = Color(c)
                                h = hsvOf(nc)[0]; s = hsvOf(nc)[1]; v = hsvOf(nc)[2]; a = nc.alpha
                                sync()
                            },
                    )
                }
            }
            SliderRow("Hue", h, 0f..360f, { h = it; sync() })
            SliderRow("Saturation", s * 100f, 0f..100f, { s = it / 100f; sync() }, "%")
            SliderRow("Brightness", v * 100f, 0f..100f, { v = it / 100f; sync() }, "%")
            SliderRow("Opacity", a * 100f, 0f..100f, { a = it / 100f; sync() }, "%")
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input
                        parseHex(input)?.let { c ->
                            val f = hsvOf(c)
                            h = f[0]; s = f[1]; v = f[2]; a = c.alpha
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onInherit) { Text("Auto") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onPick(current()) }) { Text("Apply") }
            }
        }
    }
}

private fun hsvOf(c: Color): FloatArray {
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
    return out
}

private fun parseHex(raw: String): Color? {
    val t = raw.trim().removePrefix("#")
    if (t.length != 6 && t.length != 8) return null
    val n = t.toLongOrNull(16) ?: return null
    return if (t.length == 6) {
        Color(0xFF000000 or n)
    } else {
        // #RRGGBBAA
        val r = (n shr 24) and 0xFF
        val g = (n shr 16) and 0xFF
        val b = (n shr 8) and 0xFF
        val al = n and 0xFF
        Color(r.toInt(), g.toInt(), b.toInt(), al.toInt())
    }
}
