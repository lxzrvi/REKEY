package io.github.lxzrvi.rekey.ui.home

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lxzrvi.rekey.BuildConfig
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.KeyboardPalettes
import io.github.lxzrvi.rekey.data.KeyboardPreset
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
    val keyboard = LocalSoftwareKeyboardController.current
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val flying = remember { mutableStateListOf<FlyingCard>() }

    var presets by remember { mutableStateOf(preferences.loadPresets()) }
    var nextId by remember { mutableStateOf(preferences.nextPresetId()) }
    var activeId by remember { mutableStateOf(preferences.loadActivePresetId()) }
    var selectedIndex by remember { mutableStateOf(preferences.loadSelectedCard()) }
    var anchor by remember { mutableStateOf<CardAnchor?>(null) }
    var editorOpen by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (drawerOpen) MotionContract.DRAWER_ENTER_MILLIS else MotionContract.DRAWER_EXIT_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "drawerProgress",
    )
    val editorProgress by animateFloatAsState(
        targetValue = if (editorOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (editorOpen) MotionContract.EDITOR_ENTER_MILLIS else MotionContract.EDITOR_EXIT_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "editorProgress",
    )

    // The anchor is kept after closing so the card can animate back into place.
    val editingPreset = presets.firstOrNull { it.id == anchor?.presetId }

    fun persist(list: List<KeyboardPreset>) {
        presets = list
        preferences.savePresets(list)
    }

    fun closeEditor() {
        keyboard?.hide()
        editorOpen = false
    }

    BackHandler(enabled = editorOpen || drawerOpen) {
        if (editorOpen) closeEditor() else drawerOpen = false
    }

    BoxWithConstraints(modifier.fillMaxSize().testTag("home")) {
        val drawerWidth = minOf(maxWidth - 48.dp, 360.dp)
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val safeTop = safeInsets.calculateTopPadding()
        val safeRight = safeInsets.calculateRightPadding(LayoutDirection.Ltr)
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val blurRadius = GlassContract.BLUR_DP.dp
        val topInsetPx = with(density) { safeTop.toPx() }

        Box(Modifier.fillMaxSize().liveBlurSource(blurState)) {
            SquigglePattern(
                darkTheme = darkTheme,
                strokeAlpha = if (darkTheme) PatternContract.DARK_INK_ALPHA else PatternContract.LIGHT_INK_ALPHA,
            )
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                // The header slides up and out of the way as the editor opens.
                Box(
                    Modifier.graphicsLayer {
                        translationY = -size.height * editorProgress
                        alpha = 1f - editorProgress
                    },
                ) { HomeHeader() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 28.dp),
                ) {
                    Text(
                        text = stringResource(R.string.presets),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp)
                            .graphicsLayer { alpha = 1f - editorProgress }
                            .testTag("presets_label"),
                    )
                    CardStack(
                        presets = presets,
                        editorProgress = { editorProgress },
                        editingId = { if (editorProgress > 0f) anchor?.presetId else null },
                        initialIndex = selectedIndex,
                        onIndexChanged = {
                            selectedIndex = it
                            preferences.saveSelectedCard(it)
                        },
                        onRemove = { flying.add(it) },
                        onRemoveCommitted = { id ->
                            persist(presets.filterNot { it.id == id })
                            if (activeId == id) {
                                val fallback = presets.firstOrNull { it.id != id }?.id ?: 1
                                activeId = fallback
                                preferences.saveActivePresetId(fallback)
                            }
                        },
                        onAdd = {
                            val fresh = KeyboardPreset(
                                id = nextId,
                                name = "",
                                details = "",
                                palette = KeyboardPalettes.forTheme(darkTheme),
                                placeholder = true,
                            )
                            nextId += 1
                            preferences.saveNextPresetId(nextId)
                            persist(presets + fresh)
                        },
                        onOpen = {
                            anchor = it
                            editorOpen = true
                        },
                    )
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

        // The editor dims nothing: it blurs the real page underneath it.
        if (editorProgress > 0f && anchor != null && editingPreset != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(30f)
                    .graphicsLayer { alpha = editorProgress }
                    .liveBlur(blurState, fullScreenBlur(blurRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { closeEditor() },
                    ),
            )
            PresetEditor(
                preset = editingPreset,
                layoutNumber = (presets.indexOfFirst { it.id == editingPreset.id } + 1).coerceAtLeast(1),
                anchor = requireNotNull(anchor),
                progress = editorProgress,
                topInsetPx = topInsetPx,
                inUse = activeId == editingPreset.id,
                onBack = { closeEditor() },
                onRename = { value ->
                    persist(
                        presets.map {
                            if (it.id == editingPreset.id) it.copy(name = value, placeholder = false) else it
                        },
                    )
                },
                onDetails = { value ->
                    persist(
                        presets.map {
                            if (it.id == editingPreset.id) it.copy(details = value, placeholder = false) else it
                        },
                    )
                },
                onUse = {
                    activeId = editingPreset.id
                    preferences.saveActivePresetId(editingPreset.id)
                },
                modifier = Modifier.zIndex(31f),
            )
        }

        if (drawerProgress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(40f)
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
                    .zIndex(41f)
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

        // Dismissed cards fly here: above the header, above the blur, never cropped.
        if (flying.isNotEmpty()) {
            Box(Modifier.fillMaxSize().zIndex(50f)) {
                flying.toList().forEach { card ->
                    key(card.key) {
                        FlyingCardLayer(card) { flying.remove(card) }
                    }
                }
            }
        }

        // Fixed control: three dots morph into an X in the same spot. Hidden in the editor.
        if (editorProgress < 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = safeTop + MenuTop, end = safeRight + 18.dp)
                    .zIndex(60f)
                    .graphicsLayer { alpha = 1f - editorProgress },
            ) {
                MenuMorphButton(
                    openProgress = drawerProgress,
                    onClick = { drawerOpen = !drawerOpen },
                )
            }
        }
    }
}

/**
 * Compact brand line: no circular icon, small REKEY set at the left edge with the
 * version and device under it.
 */
@Composable
private fun HomeHeader() {
    val deviceName = remember {
        val model = Build.MODEL.orEmpty().ifBlank { "Android device" }
        val maker = Build.MANUFACTURER.orEmpty()
        if (maker.isNotBlank() && !model.startsWith(maker, ignoreCase = true)) {
            "${maker.replaceFirstChar { it.uppercase() }} $model"
        } else model.replaceFirstChar { it.uppercase() }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 84.dp, top = MenuTop, bottom = 2.dp)
            .testTag("home_header"),
        horizontalAlignment = Alignment.Start,
    ) {
        RekeyWordmark(
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
