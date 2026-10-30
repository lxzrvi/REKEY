package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AccentPalette
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.ui.theme.color
import kotlin.math.atan2
import kotlin.math.hypot

private val Rainbow = listOf(
    Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFF9F871),
    Color(0xFF4FD1A5), Color(0xFF4FA8FF), Color(0xFF9B6BFF),
    Color(0xFFFF6BD6), Color(0xFFFF5F6D),
)

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
            .padding(start = 20.dp, end = 20.dp, top = 78.dp, bottom = 28.dp)
            .testTag("appearance_drawer"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.appearance_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionLabel(stringResource(R.string.theme_title))
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
        // Seven premium accents, then an eighth colourful swatch that opens the wheel.
        val presets = AccentPalette.entries.filter { it != AccentPalette.CUSTOM }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.take(4).forEach { palette ->
                AccentDot(palette, settings, darkTheme, Modifier.weight(1f)) { onChange(settings.copy(accent = palette)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.drop(4).forEach { palette ->
                AccentDot(palette, settings, darkTheme, Modifier.weight(1f)) { onChange(settings.copy(accent = palette)) }
            }
            CustomDot(
                selected = settings.accent == AccentPalette.CUSTOM,
                modifier = Modifier.weight(1f),
                onClick = { onChange(settings.copy(accent = AccentPalette.CUSTOM)) },
            )
        }
        Text(
            text = stringResource(
                when (settings.accent) {
                    AccentPalette.PLATINUM -> R.string.accent_platinum
                    AccentPalette.AMETHYST -> R.string.accent_amethyst
                    AccentPalette.JADE -> R.string.accent_jade
                    AccentPalette.AURUM -> R.string.accent_aurum
                    AccentPalette.ROSE -> R.string.accent_rose
                    AccentPalette.SAPPHIRE -> R.string.accent_sapphire
                    AccentPalette.COPPER -> R.string.accent_copper
                    AccentPalette.CUSTOM -> R.string.accent_custom
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.accent == AccentPalette.CUSTOM) {
            CustomColorPicker(
                current = settings.customColor,
                onColor = { argb -> onChange(settings.copy(accent = AccentPalette.CUSTOM, customColor = argb)) },
            )
        }

        SectionLabel(stringResource(R.string.typography_title))
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

        SectionLabel(stringResource(R.string.motion_accessibility))
        ToggleRow(stringResource(R.string.animations), stringResource(R.string.animations_description), settings.animationsEnabled) { onChange(settings.copy(animationsEnabled = it)) }
        ToggleRow(stringResource(R.string.high_contrast), stringResource(R.string.high_contrast_description), settings.highContrast) { onChange(settings.copy(highContrast = it)) }
        Text(stringResource(R.string.settings_persist), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AccentDot(
    palette: AccentPalette,
    settings: AppearanceSettings,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val selected = settings.accent == palette
    val label = stringResource(
        when (palette) {
            AccentPalette.PLATINUM -> R.string.accent_platinum
            AccentPalette.AMETHYST -> R.string.accent_amethyst
            AccentPalette.JADE -> R.string.accent_jade
            AccentPalette.AURUM -> R.string.accent_aurum
            AccentPalette.ROSE -> R.string.accent_rose
            AccentPalette.SAPPHIRE -> R.string.accent_sapphire
            AccentPalette.COPPER -> R.string.accent_copper
            AccentPalette.CUSTOM -> R.string.accent_custom
        },
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = palette.color(darkTheme, settings.customColor),
            border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.size(if (selected) 40.dp else 34.dp).semantics { contentDescription = label },
        ) {}
    }
}

@Composable
private fun CustomDot(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val label = stringResource(R.string.accent_custom)
    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier
                .size(if (selected) 40.dp else 34.dp)
                .semantics { contentDescription = label },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(Rainbow)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                )
            }
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
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
            drawCircle(brush = Brush.sweepGradient(Rainbow))
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
