package io.github.lxzrvi.rekey.ui.home

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import io.github.lxzrvi.rekey.BuildConfig
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.ui.components.RekeyWordmark
import io.github.lxzrvi.rekey.ui.glass.glass
import io.github.lxzrvi.rekey.ui.glass.glassSource
import io.github.lxzrvi.rekey.ui.glass.rememberGlassState
import io.github.lxzrvi.rekey.ui.theme.BrandBlack
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
    val backgroundGlass = rememberGlassState()
    val drawerGlass = rememberGlassState()
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
        val drawerWidth = minOf(maxWidth - 72.dp, 360.dp)
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val safeTop = safeInsets.calculateTopPadding()
        val safeRight = safeInsets.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
        val closedX = maxWidth - safeRight - 20.dp - 48.dp
        val openX = maxWidth - drawerWidth - 24.dp
        val menuX = lerp(closedX, openX, drawerProgress)
        val menuY = safeTop + 32.dp

        // Source 1: ambient color behind the glass header/cards.
        AmbientBackground(
            accent = settings.accent.color(darkTheme),
            darkTheme = darkTheme,
            modifier = Modifier.glassSource(backgroundGlass),
        )

        // Source 2: the whole home page behind the sliding drawer. Both sources are siblings of their effects.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(drawerGlass)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            HomeHeader(settings, backgroundGlass, darkTheme)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CardCarousel(settings, backgroundGlass, preferences)
            }
        }

        if (drawerProgress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f * drawerProgress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { drawerOpen = false },
                    ),
            )
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = drawerWidth.toPx() * (1f - drawerProgress) }
                    .glass(drawerGlass, RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp), settings.blurRadius.dp, settings.blurEnabled)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                AppearanceDrawer(settings, darkTheme, onSettingsChange)
            }
        }

        // The same button travels from the header's right edge to the drawer's left edge.
        MenuMorphButton(
            openProgress = drawerProgress,
            onClick = { drawerOpen = !drawerOpen },
            modifier = Modifier.graphicsLayer { translationX = menuX.toPx(); translationY = menuY.toPx() },
        )
    }
}

@Composable
private fun HomeHeader(settings: AppearanceSettings, glassState: HazeState, darkTheme: Boolean) {
    val deviceName = remember {
        val model = Build.MODEL.orEmpty().ifBlank { "Android device" }
        val maker = Build.MANUFACTURER.orEmpty()
        if (maker.isNotBlank() && !model.startsWith(maker, ignoreCase = true)) {
            "${maker.replaceFirstChar { it.uppercase() }} $model"
        } else model.replaceFirstChar { it.uppercase() }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (settings.highContrast) 0.9f else 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .height(88.dp)
            .testTag("home_header")
            .glass(glassState, MaterialTheme.shapes.large, settings.blurRadius.dp, settings.blurEnabled),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 68.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BrandBlack,
                border = BorderStroke(1.dp, BrandWhite.copy(alpha = 0.18f)),
                modifier = Modifier.size(56.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_mark),
                    contentDescription = stringResource(R.string.brand_icon_description),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                RekeyWordmark(fontSize = 34.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Start)
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
}
