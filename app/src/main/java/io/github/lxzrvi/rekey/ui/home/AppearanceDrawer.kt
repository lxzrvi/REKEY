package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AccentPalette
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.ui.theme.color
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun AppearanceDrawer(
    settings: AppearanceSettings,
    darkTheme: Boolean,
    onChange: (AppearanceSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 28.dp)
            .testTag("appearance_drawer"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.appearance_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionLabel(stringResource(R.string.theme_title))
        // Full-width segmented options with gaps between them.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                SegmentButton(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        },
                    ),
                    selected = settings.themeMode == mode,
                    modifier = Modifier.weight(1f),
                    onClick = { onChange(settings.copy(themeMode = mode)) },
                )
            }
        }

        SectionLabel(stringResource(R.string.accent_title))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentPalette.entries.forEach { palette ->
                val label = stringResource(
                    when (palette) {
                        AccentPalette.ICE -> R.string.accent_ice
                        AccentPalette.VIOLET -> R.string.accent_violet
                        AccentPalette.MINT -> R.string.accent_mint
                        AccentPalette.AMBER -> R.string.accent_amber
                        AccentPalette.CUSTOM -> R.string.accent_custom
                    },
                )
                Surface(
                    onClick = { onChange(settings.copy(accent = palette)) },
                    shape = CircleShape,
                    color = palette.color(darkTheme, settings.customColor),
                    border = BorderStroke(if (settings.accent == palette) 2.dp else 1.dp, MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.size(if (settings.accent == palette) 36.dp else 30.dp).semantics { contentDescription = label },
                ) {}
            }
        }
        if (settings.accent == AccentPalette.CUSTOM) {
            CustomColorPicker(
                current = settings.customColor,
                onColor = { argb -> onChange(settings.copy(accent = AccentPalette.CUSTOM, customColor = argb)) },
            )
        }

        SectionLabel(stringResource(R.string.glass_blur))
        ToggleRow(stringResource(R.string.glass_blur), stringResource(R.string.glass_blur_description), settings.blurEnabled) { onChange(settings.copy(blurEnabled = it)) }
        ThinSliderRow(
            label = stringResource(R.string.blur_strength),
            value = settings.blurRadius,
            valueRange = GlassContract.MIN_BLUR_DP..GlassContract.MAX_BLUR_DP,
            defaultValue = GlassContract.DEFAULT_BLUR_DP,
            valueText = "${settings.blurRadius.toInt()} dp",
            enabled = settings.blurEnabled,
            onValueChange = { onChange(settings.copy(blurRadius = it)) },
        )
        ThinSliderRow(
            label = stringResource(R.string.corner_radius),
            value = settings.cornerRadius,
            valueRange = 12f..36f,
            defaultValue = 24f,
            valueText = "${settings.cornerRadius.toInt()} dp",
            onValueChange = { onChange(settings.copy(cornerRadius = it)) },
        )

        SectionLabel(stringResource(R.string.typography_title))
        // Text size uses buttons, not a slider.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.9f to "S", 1.0f to "M", 1.1f to "L", 1.2f to "XL").forEach { (scale, label) ->
                SegmentButton(
                    text = label,
                    selected = kotlin.math.abs(settings.textScale - scale) < 0.02f,
                    modifier = Modifier.weight(1f),
                    onClick = { onChange(settings.copy(textScale = scale)) },
                )
            }
        }
        Text(
            text = stringResource(R.string.text_size_value, (settings.textScale * 100).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.typography_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.long_press_reset), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionLabel(stringResource(R.string.motion_accessibility))
        ToggleRow(stringResource(R.string.animations), stringResource(R.string.animations_description), settings.animationsEnabled) { onChange(settings.copy(animationsEnabled = it)) }
        ToggleRow(stringResource(R.string.high_contrast), stringResource(R.string.high_contrast_description), settings.highContrast) { onChange(settings.copy(highContrast = it)) }
        Text(stringResource(R.string.settings_persist), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CustomColorPicker(current: Int?, onColor: (Int) -> Unit) {
    var hex by remember(current) { mutableStateOf(current?.let { String.format("#%06X", 0xFFFFFF and it) } ?: "#9DD8FF") }
    var error by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        val description = stringResource(R.string.custom_color_wheel)
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.CenterHorizontally)
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    fun pick(position: Offset) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = position.x - center.x
                        val dy = position.y - center.y
                        val radius = minOf(size.width, size.height) / 2f
                        val saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                        onColor(Color.hsv(hue, saturation, 1f).toArgb())
                    }
                    detectTapGestures { pick(it) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val radius = minOf(size.width, size.height) / 2f
                        val saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                        onColor(Color.hsv(hue, saturation, 1f).toArgb())
                        change.consume()
                    }
                },
        ) {
            val radius = minOf(size.width, size.height) / 2f
            drawCircle(brush = Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
            drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), radius = radius))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hex,
                onValueChange = { hex = it; error = false },
                label = { Text(stringResource(R.string.hex_color)) },
                singleLine = true,
                isError = error,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f).testTag("hex_color_input"),
            )
            Button(onClick = {
                val raw = hex.trim().removePrefix("#")
                val parsed = if (raw.length == 6) raw.toLongOrNull(16) else null
                if (parsed == null) error = true else {
                    error = false
                    onColor((0xFF000000L or parsed).toInt())
                }
            }) { Text(stringResource(R.string.apply)) }
        }
        if (error) Text(stringResource(R.string.invalid_hex), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ThinSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    valueText: String,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        ThinSlider(
            value = value,
            valueRange = valueRange,
            enabled = enabled,
            label = label,
            onValueChange = onValueChange,
            onReset = { onValueChange(defaultValue) },
        )
    }
}

/** Thin track, no thumb. Drag/tap to adjust; long press to reset. */
@Composable
private fun ThinSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    label: String,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.45f else 0.2f)
    val activeColor = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.35f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                fun update(x: Float) {
                    val fraction = (x / size.width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                }
                detectTapGestures(onTap = { update(it.x) }, onLongPress = { onReset() })
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                    change.consume()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            val y = size.height / 2f
            drawLine(trackColor, Offset(0f, y), Offset(size.width, y), strokeWidth = size.height)
            val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            drawLine(activeColor, Offset(0f, y), Offset(size.width * fraction, y), strokeWidth = size.height)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
