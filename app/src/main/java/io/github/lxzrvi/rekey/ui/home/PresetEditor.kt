package io.github.lxzrvi.rekey.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.KeyboardPreset
import io.github.lxzrvi.rekey.data.KeyboardStyle
import io.github.lxzrvi.rekey.ui.editor.StyleWizard
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.ui.keyboard.KeyboardCanvas
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import kotlin.math.roundToInt

private val EditorShape = RoundedCornerShape(GlassContract.CORNER_DP.dp)
private val EditorBarHeight = 48.dp

/**
 * The preset editor.
 *
 * The tapped card grows from its exact stack rectangle up to the top of the
 * screen, stopping just below the status bar. The page behind it is the real
 * main page, blurred — not a separate screen. Everything is driven by one
 * [progress] value so pressing back simply plays the same animation backwards.
 */
@Composable
fun PresetEditor(
    preset: KeyboardPreset,
    anchor: CardAnchor,
    progress: Float,
    topInsetPx: Float,
    inUse: Boolean,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDetails: (String) -> Unit,
    onStyleChange: (KeyboardStyle) -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    var styleOpen by rememberSaveable { mutableStateOf(false) }
    val styleProgress by animateFloatAsState(
        targetValue = if (styleOpen) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "stylePane",
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = with(density) { maxWidth.toPx() }
        val gutter = with(density) { 16.dp.toPx() }
        val barH = with(density) { EditorBarHeight.toPx() }
        val gap = with(density) { 10.dp.toPx() }

        // Final rectangle: full width minus gutters, directly under the bar.
        val endLeft = gutter
        val endTop = topInsetPx + barH + gap
        val endW = screenW - gutter * 2f
        val endH = endW / KeyboardCanvas.CARD_RATIO

        val p = progress
        val left = anchor.left + (endLeft - anchor.left) * p
        val top = anchor.top + (endTop - anchor.top) * p
        val w = anchor.width + (endW - anchor.width) * p
        val h = anchor.height + (endH - anchor.height) * p

        // Top bar: circular back button, then "Preset".
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, topInsetPx.roundToInt()) }
                .height(EditorBarHeight)
                .graphicsLayer { alpha = p }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClick = onBack)
            Text(
                text = stringResource(R.string.editor_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // The travelling card. It is laid out once at its final size and only
        // transformed during the flight, so the keyboard inside it never
        // remeasures mid-animation — that is what makes open and back smooth.
        Box(
            Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .graphicsLayer {
                    scaleX = if (endW > 0f) w / endW else 1f
                    scaleY = if (endH > 0f) h / endH else 1f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .size(with(density) { endW.toDp() }, with(density) { endH.toDp() })
                .clip(EditorShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), EditorShape),
        ) {
            PresetCardFace(preset = preset)
        }

        // Editable fields fade in under the card once it has landed.
        val fieldsAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val fieldsTop = endTop + endH + gap
        val fieldsMaxH = with(density) { (maxHeight.toPx() - fieldsTop).coerceAtLeast(0f).toDp() }
        val paneW = with(density) { maxWidth.toPx() }
        if (fieldsAlpha > 0f) {
          Box(
              Modifier
                  .fillMaxWidth()
                  .offset { IntOffset(0, fieldsTop.roundToInt()) }
                  .height(fieldsMaxH)
                  .graphicsLayer { alpha = fieldsAlpha },
          ) {
            // The wizard slides in from the right while the name, details and
            // buttons slide out to the left. One progress value drives both, so
            // going back is the same movement reversed.
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -paneW * styleProgress
                        alpha = 1f - styleProgress
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EditableField(
                    label = stringResource(R.string.editor_name_label),
                    value = if (preset.placeholder) "" else preset.name,
                    placeholder = stringResource(R.string.editor_unnamed),
                    fontSize = 22,
                    bold = true,
                    onValueChange = onRename,
                    modifier = Modifier.testTag("editor_name"),
                )
                EditableField(
                    label = stringResource(R.string.editor_details_label),
                    value = preset.details,
                    placeholder = stringResource(R.string.editor_details_placeholder),
                    fontSize = 14,
                    bold = false,
                    onValueChange = onDetails,
                    modifier = Modifier.testTag("editor_details"),
                )
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = { keyboard?.hide(); onUse() },
                    enabled = !inUse,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("editor_use"),
                ) {
                    Text(
                        text = stringResource(if (inUse) R.string.editor_in_use else R.string.editor_use_layout),
                        fontSize = 14.sp,
                    )
                }
                Button(
                    onClick = { keyboard?.hide(); styleOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("editor_edit"),
                ) {
                    Text(text = stringResource(R.string.editor_edit_style), fontSize = 14.sp)
                }
            }
            if (styleProgress > 0f) {
                StyleWizard(
                    style = preset.style,
                    palette = preset.palette,
                    onChange = onStyleChange,
                    onDone = { styleOpen = false; onBack() },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = paneW * (1f - styleProgress)
                            alpha = styleProgress
                        },
                )
            }
          }
        }
    }
}

@Composable
private fun BackCircle(onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onBackground
    val description = stringResource(R.string.editor_back)
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RekeyGlyph(Glyph.BACK, tint, Modifier.size(19.dp))
    }
}

/** Plain editable text — no box, so the name reads like a title you can type into. */
@Composable
private fun EditableField(
    label: String,
    value: String,
    placeholder: String,
    fontSize: Int,
    bold: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value) }
    val style = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = fontSize.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        letterSpacing = if (bold) (-0.4).sp else 0.sp,
        lineHeight = (fontSize * 1.35f).sp,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it; onValueChange(it) },
                textStyle = style,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = modifier.fillMaxWidth(),
            )
        }
    }
}
