package io.github.lxzrvi.rekey.ui.home

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.BuildConfig
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.ui.blur.fullScreenBlur
import io.github.lxzrvi.rekey.ui.blur.liveBlur
import io.github.lxzrvi.rekey.ui.blur.liveBlurSource
import io.github.lxzrvi.rekey.ui.blur.rememberLiveBlurState
import io.github.lxzrvi.rekey.ui.blur.topEdgeBlur
import io.github.lxzrvi.rekey.ui.blur.bottomEdgeBlur
import io.github.lxzrvi.rekey.ui.components.RekeyWordmark
import io.github.lxzrvi.rekey.ui.theme.BrandWhite
import io.github.lxzrvi.rekey.ui.theme.color

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
            durationMillis = if (!settings.animationsEnabled) 0 else if (drawerOpen) MotionContract.DRAWER_ENTER_MILLIS else MotionContract.DRAWER_EXIT_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "drawerProgress",
    )
    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    BoxWithConstraints(modifier.fillMaxSize().testTag("home")) {
        val drawerWidth = minOf(maxWidth - 56.dp, 360.dp)
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val safeTop = safeInsets.calculateTopPadding()
        val safeBottom = safeInsets.calculateBottomPadding()
        val safeRight = safeInsets.calculateRightPadding(LayoutDirection.Ltr)
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val closedX = maxWidth - safeRight - 20.dp - 48.dp
        val openX = maxWidth - drawerWidth + 12.dp
        val menuX = lerp(closedX, openX, drawerProgress)
        val menuY = safeTop + 20.dp
        val accent = settings.accent.color(darkTheme, settings.customColor)

        Box(Modifier.fillMaxSize().liveBlurSource(blurState)) {
            AmbientBackground(accent = accent, darkTheme = darkTheme)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                HomeHeader(darkTheme)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CardCarousel(settings, preferences)
                }
            }
        }

        // Edge blur only: the header itself stays crisp and unboxed.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topInset + 10.dp)
                .liveBlur(blurState, topEdgeBlur(settings.blurRadius.dp, settings.blurEnabled)),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomInset + 96.dp)
                .liveBlur(blurState, bottomEdgeBlur(settings.blurRadius.dp, settings.blurEnabled)),
        )

        if (drawerProgress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = drawerProgress }
                    .liveBlur(blurState, fullScreenBlur(30.dp, settings.blurEnabled))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { drawerOpen = false },
                    ),
            )
            val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (settings.highContrast) 0.9f else 0.55f)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = drawerWidth.toPx() * (1f - drawerProgress) }
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    AppearanceDrawer(settings, darkTheme, onSettingsChange)
                }
            }
        }

        MenuMorphButton(
            openProgress = drawerProgress,
            onClick = { drawerOpen = !drawerOpen },
            modifier = Modifier.graphicsLayer { translationX = menuX.toPx(); translationY = menuY.toPx() },
        )
    }
}

@Composable
private fun HomeHeader(darkTheme: Boolean) {
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
            .padding(start = 20.dp, end = 80.dp, top = 16.dp, bottom = 8.dp)
            .height(72.dp)
            .testTag("home_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = BrandWhite,
            modifier = Modifier.size(56.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_brand_mark),
                contentDescription = stringResource(R.string.brand_icon_description),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            RekeyWordmark(fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Start)
            Text(
                text = stringResource(R.string.version_prefix, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
