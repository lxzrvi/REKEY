package io.github.lxzrvi.rekey.ui.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.data.ArrowStyle
import io.github.lxzrvi.rekey.data.BackgroundStyle
import io.github.lxzrvi.rekey.data.FeelStyle
import io.github.lxzrvi.rekey.data.KeySounds
import io.github.lxzrvi.rekey.data.KeyStyle
import io.github.lxzrvi.rekey.data.KeyboardPalette
import io.github.lxzrvi.rekey.data.KeyboardStyle
import io.github.lxzrvi.rekey.data.LayoutStyle
import io.github.lxzrvi.rekey.data.SuggestionStyle
import io.github.lxzrvi.rekey.ime.KeyFeedback

/** A fresh scroll position per page, so page 4 never opens half way down. */
@Composable
private fun scrollFor(page: Int): ScrollState = remember(page) { ScrollState(0) }

private val PageTitles = listOf(
    "Background", "Keyboard layout", "Keys", "Touch & feel", "Suggestion bar", "Arrow bar",
)

/**
 * The six-page style editor.
 *
 * Every page owns one section of [KeyboardStyle], which is what makes "Reset"
 * local: it replaces that section with its default and leaves the other five
 * alone. Skip moves on without touching anything; Previous walks back.
 */
@Composable
fun StyleWizard(
    style: KeyboardStyle,
    palette: KeyboardPalette,
    onChange: (KeyboardStyle) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = PageTitles.lastIndex

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = PageTitles[page],
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${page + 1} / ${PageTitles.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Step pips, so it is obvious how much is left.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PageTitles.indices.forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= page) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        ),
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollFor(page))
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (page) {
                0 -> BackgroundPage(style.background, palette) { onChange(style.copy(background = it)) }
                1 -> LayoutPage(style.layout) { onChange(style.copy(layout = it)) }
                2 -> KeysPage(style.keys, palette) { onChange(style.copy(keys = it)) }
                3 -> FeelPage(style.feel) { onChange(style.copy(feel = it)) }
                4 -> SuggestionPage(style.suggestions, palette) { onChange(style.copy(suggestions = it)) }
                else -> ArrowPage(style.arrows, palette) { onChange(style.copy(arrows = it)) }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(onClick = { if (page > 0) page -= 1 }, enabled = page > 0) {
                Text("Previous", fontSize = 12.sp)
            }
            TextButton(onClick = {
                // Reset only this page's section.
                onChange(
                    when (page) {
                        0 -> style.copy(background = BackgroundStyle())
                        1 -> style.copy(layout = LayoutStyle())
                        2 -> style.copy(keys = KeyStyle())
                        3 -> style.copy(feel = FeelStyle())
                        4 -> style.copy(suggestions = SuggestionStyle())
                        else -> style.copy(arrows = ArrowStyle())
                    },
                )
            }) { Text("Reset", fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            if (page < last) {
                TextButton(onClick = { page += 1 }) { Text("Skip", fontSize = 12.sp) }
                Button(
                    onClick = { page += 1 },
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.height(40.dp),
                ) { Text("Next", fontSize = 13.sp) }
            } else {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.height(40.dp),
                ) { Text("Done", fontSize = 13.sp) }
            }
        }
    }
}

// ---- page 1 ------------------------------------------------------------------

@Composable
private fun BackgroundPage(
    spec: BackgroundStyle,
    palette: KeyboardPalette,
    onChange: (BackgroundStyle) -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onChange(spec.copy(useImage = true, imageUri = uri.toString()))
        }
    }

    Panel("Source") {
        SegmentRow(
            label = "Fill the tray with",
            options = listOf(false, true),
            selected = spec.useImage,
            title = { if (it) "Image" else "Colour" },
            onSelect = { onChange(spec.copy(useImage = it)) },
        )
    }

    if (!spec.useImage) {
        Panel("Colour") {
            ColorRow("Base colour", spec.color, palette.canvas) { onChange(spec.copy(color = it)) }
            ColorRow("Gradient end", spec.gradient, palette.canvas) { onChange(spec.copy(gradient = it)) }
            if (spec.gradient != null) {
                SliderRow("Gradient angle", spec.gradientAngle, 0f..360f, { onChange(spec.copy(gradientAngle = it)) }, "°")
            }
        }
    } else {
        Panel("Image") {
            Button(
                onClick = { picker.launch(arrayOf("image/*")) },
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
            ) { Text(if (spec.imageUri == null) "Choose image" else "Change image", fontSize = 13.sp) }
            Text(
                text = if (spec.imageUri == null) "No image selected" else "Image selected",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SliderRow("Zoom", spec.imageScale, 1f..2.5f, { onChange(spec.copy(imageScale = it)) }, "x", 2)
            SliderRow("Blur", spec.blurDp, 0f..25f, { onChange(spec.copy(blurDp = it)) }, " dp")
            if (spec.imageUri != null) {
                TextButton(onClick = { onChange(spec.copy(imageUri = null, useImage = false)) }) {
                    Text("Remove image", fontSize = 12.sp)
                }
            }
        }
    }
    Panel("Readability") {
        SliderRow("Darken", spec.dim * 100f, 0f..80f, { onChange(spec.copy(dim = it / 100f)) }, "%")
        Text(
            text = "Keys stay solid on top of the background.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- page 2 ------------------------------------------------------------------

@Composable
private fun LayoutPage(spec: LayoutStyle, onChange: (LayoutStyle) -> Unit) {
    Panel("Size") {
        SliderRow("Key height", spec.heightScale * 100f, 75f..140f, { onChange(spec.copy(heightScale = it / 100f)) }, "%")
        SliderRow("Board width", spec.widthScale * 100f, 80f..100f, { onChange(spec.copy(widthScale = it / 100f)) }, "%")
    }
    Panel("Padding") {
        SliderRow("Left & right", spec.padHorizontal, 0f..24f, { onChange(spec.copy(padHorizontal = it)) }, " dp")
        SliderRow("Top", spec.padTop, 0f..28f, { onChange(spec.copy(padTop = it)) }, " dp")
        SliderRow("Bottom", spec.padBottom, 0f..28f, { onChange(spec.copy(padBottom = it)) }, " dp")
    }
    Panel("Spacing") {
        SliderRow("Between rows", spec.rowGap, 0f..18f, { onChange(spec.copy(rowGap = it)) }, " dp")
        SliderRow("Between keys", spec.keyGap, 0f..12f, { onChange(spec.copy(keyGap = it)) }, " dp")
    }
    Panel("Corners") {
        SliderRow("Top left", spec.cornerTopLeft, 0f..36f, { onChange(spec.copy(cornerTopLeft = it)) }, " dp")
        SliderRow("Top right", spec.cornerTopRight, 0f..36f, { onChange(spec.copy(cornerTopRight = it)) }, " dp")
        SliderRow("Bottom left", spec.cornerBottomLeft, 0f..36f, { onChange(spec.copy(cornerBottomLeft = it)) }, " dp")
        SliderRow("Bottom right", spec.cornerBottomRight, 0f..36f, { onChange(spec.copy(cornerBottomRight = it)) }, " dp")
    }
}

// ---- page 3 ------------------------------------------------------------------

@Composable
private fun KeysPage(spec: KeyStyle, palette: KeyboardPalette, onChange: (KeyStyle) -> Unit) {
    Panel("Shape & type") {
        SliderRow("Corner radius", spec.radius, 0f..24f, { onChange(spec.copy(radius = it)) }, " dp")
        SliderRow("Text size", spec.textSize, 13f..26f, { onChange(spec.copy(textSize = it)) }, " sp")
        SliderRow("Border width", spec.borderWidth, 0f..3f, { onChange(spec.copy(borderWidth = it)) }, " dp", 1)
    }
    Panel("Letter keys") {
        ColorRow("Key colour", spec.keyColor, palette.key) { onChange(spec.copy(keyColor = it)) }
        ColorRow("Text colour", spec.textColor, palette.text) { onChange(spec.copy(textColor = it)) }
        ColorRow("Border colour", spec.borderColor, palette.border) { onChange(spec.copy(borderColor = it)) }
    }
    Panel("Special keys") {
        ColorRow("Special background", spec.specialColor, palette.special) { onChange(spec.copy(specialColor = it)) }
        ColorRow("Shift", spec.shiftColor, palette.special) { onChange(spec.copy(shiftColor = it)) }
        ColorRow("Numbers (?123)", spec.numberColor, palette.special) { onChange(spec.copy(numberColor = it)) }
        ColorRow("Emoji", spec.emojiColor, palette.special) { onChange(spec.copy(emojiColor = it)) }
        ColorRow("Enter", spec.enterColor, palette.enter) { onChange(spec.copy(enterColor = it)) }
        ColorRow("Enter icon", spec.enterTextColor, palette.enterText) { onChange(spec.copy(enterTextColor = it)) }
    }
}

// ---- page 4 ------------------------------------------------------------------

@Composable
private fun FeelPage(spec: FeelStyle, onChange: (FeelStyle) -> Unit) {
    val context = LocalContext.current
    val feedback = remember(context) { KeyFeedback(context) }
    DisposableEffect(Unit) { onDispose { feedback.release() } }

    Panel("Tap sound") {
        SwitchRow("Play a sound on every key", spec.soundOn) { onChange(spec.copy(soundOn = it)) }
        SliderRow("Volume", spec.soundVolume * 100f, 0f..100f, { onChange(spec.copy(soundVolume = it / 100f)) }, "%")
    }
    Panel("Sound") {
        KeySounds.labels.forEachIndexed { i, name ->
            val on = i == spec.soundIndex
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    )
                    .border(
                        1.dp,
                        if (on) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape,
                    )
                    .clickable {
                        onChange(spec.copy(soundIndex = i, soundOn = true))
                        feedback.preview(i, spec.soundVolume)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Tap to hear",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Panel("Vibration") {
        SwitchRow("Vibrate on every key", spec.hapticOn) { onChange(spec.copy(hapticOn = it)) }
        SliderRow("Intensity", spec.hapticStrength.toFloat(), 1f..255f, { onChange(spec.copy(hapticStrength = it.toInt())) })
        SliderRow("Length", spec.hapticDuration.toFloat(), 5f..60f, { onChange(spec.copy(hapticDuration = it.toInt())) }, " ms")
        TextButton(onClick = { feedback.preview(spec.soundIndex, spec.soundVolume); feedback.configure(spec); feedback.tap() }) {
            Text("Test", fontSize = 12.sp)
        }
    }
}

// ---- page 5 ------------------------------------------------------------------

@Composable
private fun SuggestionPage(
    spec: SuggestionStyle,
    palette: KeyboardPalette,
    onChange: (SuggestionStyle) -> Unit,
) {
    Panel("Strip") {
        SwitchRow("Show the suggestion bar", spec.enabled) { onChange(spec.copy(enabled = it)) }
        SegmentRow(
            label = "Columns",
            options = listOf(3, 4, 5),
            selected = spec.columns,
            title = { it.toString() },
            onSelect = { onChange(spec.copy(columns = it)) },
        )
    }
    Panel("Size & shape") {
        SliderRow("Height", spec.height, 26f..44f, { onChange(spec.copy(height = it)) }, " dp")
        SliderRow("Corner radius", spec.radius, 0f..20f, { onChange(spec.copy(radius = it)) }, " dp")
        SliderRow("Text size", spec.textSize, 9f..16f, { onChange(spec.copy(textSize = it)) }, " sp")
    }
    Panel("Colour") {
        ColorRow("Chip colour", spec.color, palette.key) { onChange(spec.copy(color = it)) }
        ColorRow("Text colour", spec.textColor, palette.text) { onChange(spec.copy(textColor = it)) }
    }
    Panel("Grab handle") {
        SwitchRow("Show the handle", spec.grabberEnabled) { onChange(spec.copy(grabberEnabled = it)) }
        Text(
            text = "Swipe it down to close the keyboard. Double tap to float it. While floating, drag it to move.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SliderRow("Width", spec.grabberWidth, 24f..90f, { onChange(spec.copy(grabberWidth = it)) }, " dp")
        ColorRow("Handle colour", spec.grabberColor, palette.muted) { onChange(spec.copy(grabberColor = it)) }
    }
    Panel("Action pill") {
        SliderRow("Corner radius", spec.pillRadius, 6f..30f, { onChange(spec.copy(pillRadius = it)) }, " dp")
        ColorRow("Pill colour", spec.pillColor, palette.key) { onChange(spec.copy(pillColor = it)) }
        ColorRow("Icon colour", spec.pillIconColor, palette.text) { onChange(spec.copy(pillIconColor = it)) }
        SwitchRow("Paste", spec.pillPaste) { onChange(spec.copy(pillPaste = it)) }
        SwitchRow("Translate", spec.pillTranslate) { onChange(spec.copy(pillTranslate = it)) }
        SwitchRow("Change theme", spec.pillTheme) { onChange(spec.copy(pillTheme = it)) }
        SwitchRow("Change language", spec.pillLanguage) { onChange(spec.copy(pillLanguage = it)) }
    }
}

// ---- page 6 ------------------------------------------------------------------

@Composable
private fun ArrowPage(spec: ArrowStyle, palette: KeyboardPalette, onChange: (ArrowStyle) -> Unit) {
    Panel("Editing bar") {
        SwitchRow("Show the arrow bar", spec.enabled) { onChange(spec.copy(enabled = it)) }
        Text(
            text = "Adds a row of cursor keys above the suggestion bar.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SliderRow("Height", spec.height, 30f..48f, { onChange(spec.copy(height = it)) }, " dp")
        SliderRow("Corner radius", spec.radius, 0f..20f, { onChange(spec.copy(radius = it)) }, " dp")
    }
    Panel("Colour") {
        ColorRow("Button colour", spec.color, palette.special) { onChange(spec.copy(color = it)) }
        ColorRow("Icon colour", spec.iconColor, palette.text) { onChange(spec.copy(iconColor = it)) }
    }
    Panel("Tools") {
        SwitchRow("Select all", spec.selectAll) { onChange(spec.copy(selectAll = it)) }
        SwitchRow("Copy", spec.copy) { onChange(spec.copy(copy = it)) }
        SwitchRow("Cut", spec.cut) { onChange(spec.copy(cut = it)) }
        SwitchRow("Paste", spec.paste) { onChange(spec.copy(paste = it)) }
        SwitchRow("Home / End", spec.homeEnd) { onChange(spec.copy(homeEnd = it)) }
        SwitchRow("Bold", spec.bold) { onChange(spec.copy(bold = it)) }
        SwitchRow("Italic", spec.italic) { onChange(spec.copy(italic = it)) }
        SwitchRow("Underline", spec.underline) { onChange(spec.copy(underline = it)) }
    }
}
