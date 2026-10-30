package io.github.lxzrvi.rekey.ui.home

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.BuildConfig
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.design.PatternContract
import io.github.lxzrvi.rekey.ui.blur.bottomEdgeBlur
import io.github.lxzrvi.rekey.ui.blur.fullScreenBlur
import io.github.lxzrvi.rekey.ui.blur.liveBlur
import io.github.lxzrvi.rekey.ui.blur.liveBlurSource
import io.github.lxzrvi.rekey.ui.blur.rememberLiveBlurState
import io.github.lxzrvi.rekey.ui.blur.topEdgeBlur
import io.github.lxzrvi.rekey.ui.components.RekeyWordmark
import io.github.lxzrvi.rekey.ui.theme.BrandWhite

private val DrawerCorner = 26.dp

/** The X and the drawer title share this top offset and this height, so they line up. */
private val MenuTop = 18.dp
private val ControlHeight = 48.dp

@Composable
fun HomeScreen(
    settings: AppearanceSettings,
    onSettingsChange: (AppearanceSettings) -> Unit,
    preferences: RekeyPreferences,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val blurState = rememberLiveBlurState()
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (drawerOpen) MotionContract.DRAWER_ENTER_MILLIS else MotionContract.DRAWER_EXIT_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "drawerProgress",
    )
    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    BoxWithConstraints(modifier.fillMaxSize().testTag("home")) {
        val drawerWidth = minOf(maxWidth - 48.dp, 360.dp)
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val safeTop = safeInsets.calculateTopPadding()
        val safeRight = safeInsets.calculateRightPadding(LayoutDirection.Ltr)
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val blurRadius = GlassContract.BLUR_DP.dp

        Box(Modifier.fillMaxSize().liveBlurSource(blurState)) {
            SquigglePattern(
                darkTheme = darkTheme,
                strokeAlpha = if (darkTheme) PatternContract.DARK_INK_ALPHA else PatternContract.LIGHT_INK_ALPHA,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                HomeHeader()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 28.dp),
                ) {
                    // Screen title sits under the brand header, then a small gap to the cards.
                    Text(
                        text = stringResource(R.string.presets),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp)
                            .testTag("presets_label"),
                    )
                    CardStack(settings, preferences)
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topInset + 10.dp)
                .liveBlur(blurState, topEdgeBlur(blurRadius)),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomInset + 96.dp)
                .liveBlur(blurState, bottomEdgeBlur(blurRadius)),
        )

        if (drawerProgress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = drawerProgress }
                    .liveBlur(blurState, fullScreenBlur(blurRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { drawerOpen = false },
                    ),
            )
            val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            val drawerShape = RoundedCornerShape(topStart = DrawerCorner, bottomStart = DrawerCorner)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = drawerWidth.toPx() * (1f - drawerProgress) }
                    .clip(drawerShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .drawWithContent {
                        drawContent()
                        val r = DrawerCorner.toPx()
                        val x = 0.5.dp.toPx()
                        val path = Path().apply {
                            moveTo(r, x)
                            arcTo(Rect(x, x, x + 2 * r, x + 2 * r), 270f, -90f, false)
                            lineTo(x, size.height - r)
                            arcTo(Rect(x, size.height - x - 2 * r, x + 2 * r, size.height - x), 180f, -90f, false)
                        }
                        drawPath(path, borderColor, style = Stroke(width = 1.dp.toPx()))
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = MenuTop + ControlHeight + 14.dp),
                ) {
                    AppearanceDrawer(settings, darkTheme, onSettingsChange)
                }
                // Same top offset and same height as the X, so both baselines match.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = safeTop + MenuTop)
                        .height(ControlHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.appearance_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer { alpha = drawerProgress },
                    )
                }
            }
        }

        // Fixed control: three dots morph into an X in the same spot.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = safeTop + MenuTop, end = safeRight + 18.dp),
        ) {
            MenuMorphButton(
                openProgress = drawerProgress,
                onClick = { drawerOpen = !drawerOpen },
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    val deviceName = remember {
        val model = Build.MODEL.orEmpty().ifBlank { "Android device" }
        val maker = Build.MANUFACTURER.orEmpty()
        if (maker.isNotBlank() && !model.startsWith(maker, ignoreCase = true)) {
            "${maker.replaceFirstChar { it.uppercase() }} $model"
        } else model.replaceFirstChar { it.uppercase() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 84.dp, top = MenuTop, bottom = 4.dp)
            .height(60.dp)
            .testTag("home_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = BrandWhite,
            modifier = Modifier.size(44.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_brand_mark),
                contentDescription = stringResource(R.string.brand_icon_description),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            RekeyWordmark(
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
            )
            Text(
                text = stringResource(R.string.version_prefix, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
