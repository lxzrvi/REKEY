package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AccentChoice
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.ThemeMode

/** Compact option boxes: theme segments, accent segments and Apply all share this. */
private val OptionHeight = 38.dp

@Composable
fun AppearanceDrawer(
    settings: AppearanceSettings,
    darkTheme: Boolean,
    onChange: (AppearanceSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Custom starts closed. Tapping Custom opens the hex box; Apply closes it again.
    var hexOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            .testTag("appearance_drawer"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentButton(
                text = stringResource(R.string.accent_default),
                selected = settings.accent == AccentChoice.DEFAULT,
                modifier = Modifier.weight(1f),
                onClick = {
                    hexOpen = false
                    onChange(settings.copy(accent = AccentChoice.DEFAULT))
                },
            )
            SegmentButton(
                text = stringResource(R.string.accent_custom),
                selected = settings.accent == AccentChoice.CUSTOM,
                modifier = Modifier.weight(1f).testTag("accent_custom"),
                onClick = {
                    // Tapping Custom toggles the editor; the accent itself stays Custom.
                    hexOpen = if (settings.accent == AccentChoice.CUSTOM) !hexOpen else true
                    if (settings.accent != AccentChoice.CUSTOM) {
                        onChange(settings.copy(accent = AccentChoice.CUSTOM))
                    }
                },
            )
        }
        if (settings.accent == AccentChoice.CUSTOM && hexOpen) {
            HexColorField(
                current = DefaultAccentHexArgb,
                onColor = { argb ->
                    onChange(settings.copy(accent = AccentChoice.CUSTOM, customColor = argb))
                    hexOpen = false
                },
            )
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
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.height(OptionHeight),
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

/**
 * The hex box always opens on the default blue's own hex — never on a previously
 * typed colour, so what you read always matches the swatch beside it.
 */
private val DefaultAccentHexArgb: Int = 0xFF007AFF.toInt()

/**
 * Custom accent is hex-only. The field opens pre-filled with #007AFF and Apply
 * hides the box again.
 */
@Composable
private fun HexColorField(current: Int, onColor: (Int) -> Unit) {
    var hex by remember(current) { mutableStateOf(String.format(Locale.US, "#%06X", 0xFFFFFF and current)) }
    var error by remember { mutableStateOf(false) }
    val preview = Color(current)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(preview),
            )
            OutlinedTextField(
                value = hex,
                onValueChange = { hex = it.take(7); error = false },
                label = { Text(stringResource(R.string.hex_color)) },
                singleLine = true,
                isError = error,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f).testTag("hex_color_input"),
            )
        }
        Button(
            onClick = {
                val raw = hex.trim().removePrefix("#")
                val parsed = if (raw.length == 6) raw.toLongOrNull(16) else null
                if (parsed == null) {
                    error = true
                } else {
                    error = false
                    onColor((0xFF000000L or parsed).toInt())
                }
            },
            modifier = Modifier.fillMaxWidth().height(OptionHeight).testTag("hex_apply"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) { Text(stringResource(R.string.apply), fontSize = 13.sp) }
        if (error) {
            Text(
                stringResource(R.string.invalid_hex),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}
