package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AccentPalette
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.ui.theme.color
import kotlin.math.roundToInt

@Composable
fun AppearanceDrawer(
    settings: AppearanceSettings,
    darkTheme: Boolean,
    onChange: (AppearanceSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 20.dp, top = 28.dp, bottom = 28.dp)
            .testTag("appearance_drawer"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            stringResource(R.string.appearance),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.appearance_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingLabel(R.string.theme)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip(R.string.theme_system, ThemeMode.SYSTEM, settings, onChange, Modifier.weight(1f))
            ThemeChip(R.string.theme_light, ThemeMode.LIGHT, settings, onChange, Modifier.weight(1f))
            ThemeChip(R.string.theme_dark, ThemeMode.DARK, settings, onChange, Modifier.weight(1f))
        }

        SettingLabel(R.string.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentPalette.entries.forEach { palette ->
                val selected = settings.accent == palette
                Surface(
                    onClick = { onChange(settings.copy(accent = palette)) },
                    shape = CircleShape,
                    color = palette.color(darkTheme),
                    border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(40.dp).testTag("accent_${palette.name.lowercase()}"),
                ) {}
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(R.string.glass_blur, R.string.glass_blur_description, settings.blurEnabled, "blur_switch") { onChange(settings.copy(blurEnabled = it)) }
        SliderRow(R.string.blur_strength, settings.blurRadius, GlassContract.MIN_BLUR_DP..GlassContract.MAX_BLUR_DP, "blur_slider", enabled = settings.blurEnabled, suffix = " dp") { onChange(settings.copy(blurRadius = it)) }
        SliderRow(R.string.corner_radius, settings.cornerRadius, 12f..36f, "corner_slider", suffix = " dp") { onChange(settings.copy(cornerRadius = it)) }
        SliderRow(R.string.text_size, settings.textScale * 100f, 90f..120f, "text_slider", suffix = "%") { onChange(settings.copy(textScale = it / 100f)) }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(R.string.animations, R.string.animations_description, settings.animationsEnabled, "animations_switch") { onChange(settings.copy(animationsEnabled = it)) }
        SwitchRow(R.string.high_contrast, R.string.high_contrast_description, settings.highContrast, "contrast_switch") { onChange(settings.copy(highContrast = it)) }

        Button(
            onClick = { onChange(AppearanceSettings()) },
            modifier = Modifier.fillMaxWidth().testTag("reset_appearance"),
        ) { Text(stringResource(R.string.reset_appearance)) }
    }
}

@Composable
private fun SettingLabel(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
}

@Composable
private fun ThemeChip(label: Int, mode: ThemeMode, settings: AppearanceSettings, onChange: (AppearanceSettings) -> Unit, modifier: Modifier) {
    FilterChip(
        selected = settings.themeMode == mode,
        onClick = { onChange(settings.copy(themeMode = mode)) },
        label = { Text(stringResource(label), style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.testTag("theme_${mode.name.lowercase()}"),
    )
}

@Composable
private fun SwitchRow(title: Int, description: Int, checked: Boolean, tag: String, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun SliderRow(
    title: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    enabled: Boolean = true,
    suffix: String = "",
    onValue: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text("${value.roundToInt()}$suffix", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}
