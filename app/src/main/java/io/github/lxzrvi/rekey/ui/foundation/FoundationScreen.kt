package io.github.lxzrvi.rekey.ui.foundation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.ui.components.RekeyWordmark
import io.github.lxzrvi.rekey.ui.theme.BrandBlack
import io.github.lxzrvi.rekey.ui.theme.BrandWhite
import io.github.lxzrvi.rekey.ui.theme.RekeyTheme

/** A development foundation, deliberately not an invented product or onboarding flow. */
@Composable
fun FoundationScreen(modifier: Modifier = Modifier) {
    var motionRevealed by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        color = BrandWhite,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_brand_mark),
                            contentDescription = stringResource(R.string.brand_icon_description),
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(R.string.foundation_eyebrow),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = "Android · 0.1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    color = BrandBlack,
                    contentColor = BrandWhite,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        RekeyWordmark()
                        Surface(
                            color = BrandBlack,
                            contentColor = BrandWhite,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, BrandWhite.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = stringResource(R.string.brand_pending),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.foundation_title),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.foundation_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandWhite.copy(alpha = 0.8f),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.design_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                TypographyPreview()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MotionPreview(
                    revealed = motionRevealed,
                    onToggle = { motionRevealed = !motionRevealed },
                )
                Text(
                    text = stringResource(R.string.footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun TypographyPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.typography_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.typography_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeightTile("Aa", stringResource(R.string.font_thin), "100", FontWeight.Thin, Modifier.weight(1f))
            WeightTile("Aa", stringResource(R.string.font_regular), "400", FontWeight.Normal, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeightTile("Aa", stringResource(R.string.font_medium), "500", FontWeight.Medium, Modifier.weight(1f))
            WeightTile("Aa", stringResource(R.string.font_bold), "700", FontWeight.Bold, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeightTile(
    sample: String,
    label: String,
    weight: String,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = sample,
                fontFamily = io.github.lxzrvi.rekey.ui.theme.Poppins,
                fontWeight = fontWeight,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            )
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Poppins · $weight",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MotionPreview(revealed: Boolean, onToggle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.motion_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.motion_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.motion_idle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Compose animation honors the system Animator duration scale, including zero.
                AnimatedVisibility(
                    visible = revealed,
                    enter = fadeIn(tween(MotionContract.FADE_MILLIS)) + expandVertically(
                        animationSpec = tween(MotionContract.ENTER_MILLIS, easing = FastOutSlowInEasing),
                    ),
                    exit = fadeOut(tween(MotionContract.FADE_MILLIS)) + shrinkVertically(
                        animationSpec = tween(MotionContract.EXIT_MILLIS, easing = FastOutSlowInEasing),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.motion_revealed),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("motion_result"),
                    )
                }
            }
        }
        Button(
            onClick = onToggle,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("motion_toggle"),
        ) {
            Text(
                text = stringResource(
                    if (revealed) R.string.motion_reset else R.string.motion_reveal,
                ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FoundationPreview() {
    RekeyTheme { FoundationScreen() }
}
