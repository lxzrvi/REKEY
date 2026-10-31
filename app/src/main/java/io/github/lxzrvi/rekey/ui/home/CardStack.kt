package io.github.lxzrvi.rekey.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.KeyboardPreset
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.ui.blur.fullScreenBlur
import io.github.lxzrvi.rekey.ui.blur.liveBlur
import io.github.lxzrvi.rekey.ui.blur.liveBlurSource
import io.github.lxzrvi.rekey.ui.blur.rememberLiveBlurState
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import io.github.lxzrvi.rekey.ui.keyboard.KeyboardCanvas
import io.github.lxzrvi.rekey.ui.keyboard.KeyboardPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

private val Smooth = spring<Float>(dampingRatio = 1f, stiffness = 120f)
private val SlotSpring = spring<Float>(dampingRatio = 1f, stiffness = 200f)
private val ReturnSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 260f)
private val ExitEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

/** Card corner radius is fixed by the glass contract. */
private val CardShape = RoundedCornerShape(GlassContract.CORNER_DP.dp)

/**
 * A layout card that has left the stack. Drawn by [HomeScreen] in a root-level
 * overlay so the flight passes over the header instead of being clipped by the
 * scrolling column. The X and the hand swipe both come through here.
 */
@Stable
class FlyingCard(
    val key: Int,
    val preset: KeyboardPreset,
    val layoutNumber: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val direction: Float,
    val rotation: Float,
)

/** Geometry captured when a card is tapped, so the editor can grow out of it. */
@Stable
class CardAnchor(
    val presetId: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

@Stable
private class CardState(val id: Int, val animateIn: Boolean = true) {
    var removing by mutableStateOf(false)
    var gone by mutableStateOf(false)
    var frozenIndex = 0
    var offsetY by mutableFloatStateOf(0f)
    var returnJob: Job? = null
    var bounds: Rect = Rect.Zero
    var layoutW = 0f
    var layoutH = 0f
}

private fun two(i: Int) = i.toString().padStart(2, '0')

/**
 * Draggable preset stack. Behind-cards keep full brightness — depth comes only
 * from scale and offset.
 *
 * Dragging down never removes a card: it only stretches, then springs back.
 * Removal happens through the X or an upward swipe, and both hand the card to
 * the root overlay so it travels over the header.
 */
@Composable
fun CardStack(
    presets: List<KeyboardPreset>,
    editorProgress: () -> Float,
    editingId: () -> Int?,
    onRemove: (FlyingCard) -> Unit,
    onRemoveCommitted: (Int) -> Unit,
    onAdd: () -> Unit,
    onOpen: (CardAnchor) -> Unit,
    initialIndex: Int = 0,
    onIndexChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val states = remember { mutableMapOf<Int, CardState>() }
    var nextKey by remember { mutableIntStateOf(0) }
    val startIndex = initialIndex.coerceIn(0, presets.size.coerceAtLeast(0))
    var pos by remember { mutableFloatStateOf(startIndex.toFloat()) }
    var target by remember { mutableIntStateOf(startIndex) }
    val job = remember { arrayOfNulls<Job>(1) }
    val cardHPxHolder = remember { floatArrayOf(0f) }
    val seeded = remember { mutableStateOf(false) }

    // Keep one animation record per preset id; new cards animate in.
    presets.forEach { p ->
        if (!states.containsKey(p.id)) states[p.id] = CardState(p.id, animateIn = seeded.value)
    }
    val liveIds = presets.map { it.id }.toSet()
    states.keys.retainAll(liveIds)
    LaunchedEffect(Unit) { seeded.value = true }

    fun stateFor(p: KeyboardPreset) = states.getOrPut(p.id) { CardState(p.id, animateIn = false) }
    fun live() = presets.filter { !stateFor(it).gone }

    fun settleTo(t: Int, v0: Float = 0f) {
        target = t
        onIndexChanged(t)
        job[0]?.cancel()
        job[0] = scope.launch {
            animate(
                initialValue = pos,
                targetValue = t.toFloat(),
                initialVelocity = v0,
                animationSpec = Smooth,
            ) { value, _ -> pos = value }
        }
    }

    LaunchedEffect(presets.size) {
        val n = live().size
        if (target > n) settleTo(n.coerceAtLeast(0))
    }

    fun removePreset(p: KeyboardPreset) {
        val item = stateFor(p)
        if (item.removing || item.gone) return
        item.returnJob?.cancel()
        item.removing = true
        val b = item.bounds
        if (b.width > 0f && b.height > 0f) {
            val hPx = cardHPxHolder[0].takeIf { it > 0f } ?: b.height
            onRemove(
                FlyingCard(
                    key = nextKey++,
                    preset = p,
                    layoutNumber = (presets.indexOfFirst { it.id == p.id } + 1).coerceAtLeast(1),
                    left = b.left,
                    top = b.top,
                    width = item.layoutW.takeIf { it > 0f } ?: b.width,
                    height = item.layoutH.takeIf { it > 0f } ?: b.height,
                    direction = -1f,
                    rotation = (item.offsetY / hPx).coerceIn(-1f, 1f) * 3f,
                ),
            )
        }
        scope.launch {
            delay(MotionContract.CARD_GAP_CLOSE_MILLIS)
            val remaining = live().filter { it.id != p.id }
            val r = live().indexOfFirst { it.id == p.id }
            item.frozenIndex = r.coerceAtLeast(0)
            item.gone = true
            val t = if (r in 0 until target) target - 1 else target
            settleTo(t.coerceIn(0, remaining.size.coerceAtLeast(0)))
            onRemoveCommitted(p.id)
        }
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().zIndex(2f).testTag("card_stack")) {
            val cardW = minOf(maxWidth * 0.86f, 420.dp)
            val cardH = cardW * 0.74f
            val density = LocalDensity.current
            val wPx = with(density) { cardW.toPx() }
            val hPx = with(density) { cardH.toPx() }
            cardHPxHolder[0] = hPx
            val stepPx = wPx * 0.5f

            val dragState = rememberDraggableState { delta ->
                val n = live().size.toFloat()
                val d = -delta / stepPx
                val out = (pos <= 0f && d < 0f) || (pos >= n && d > 0f)
                pos += if (out) d * 0.35f else d
            }

            val verticalDrag = rememberDraggableState { delta ->
                val p = live().getOrNull(target) ?: return@rememberDraggableState
                val item = stateFor(p)
                if (item.removing) return@rememberDraggableState
                item.returnJob?.cancel()
                // Downward travel is rubber-banded: it can never reach a
                // dismissal threshold, it only stretches.
                item.offsetY = if (item.offsetY + delta > 0f) {
                    (item.offsetY + delta * 0.4f).coerceAtMost(hPx * 0.5f)
                } else {
                    item.offsetY + delta
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cardH + 24.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = { job[0]?.cancel() },
                        onDragStopped = { velocity ->
                            val n = live().size
                            val v = (-velocity / stepPx).coerceIn(-10f, 10f)
                            settleTo((pos + v * 0.16f).roundToInt().coerceIn(0, n), v)
                        },
                    )
                    .draggable(
                        state = verticalDrag,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val p = live().getOrNull(target)
                            if (p != null) {
                                val item = stateFor(p)
                                if (!item.removing) {
                                    val up = item.offsetY < 0f
                                    val far = -item.offsetY > hPx * 0.26f
                                    val fast = velocity < -900f && -item.offsetY > hPx * 0.05f
                                    if (up && (far || fast)) {
                                        removePreset(p)
                                    } else {
                                        item.returnJob = scope.launch {
                                            animate(
                                                initialValue = item.offsetY,
                                                targetValue = 0f,
                                                initialVelocity = velocity,
                                                animationSpec = ReturnSpring,
                                            ) { value, _ -> item.offsetY = value }
                                        }
                                    }
                                }
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val visible = live()
                val editId = editingId()
                val ep = editorProgress()

                presets.forEach { p ->
                    key(p.id) {
                        val item = stateFor(p)
                        // While removing or editing, an overlay owns the visuals.
                        if (!item.removing && p.id != editId) {
                            val idx = if (item.gone) item.frozenIndex else visible.indexOfFirst { it.id == p.id }
                            val side = if (editId == null) 0f else {
                                val editIdx = visible.indexOfFirst { it.id == editId }
                                when {
                                    idx < editIdx -> -1f
                                    idx > editIdx -> 1f
                                    else -> 0f
                                }
                            }
                            StackCard(
                                slotIndex = idx,
                                pos = { pos },
                                wPx = wPx,
                                hPx = hPx,
                                w = cardW,
                                h = cardH,
                                animateIn = item.animateIn,
                                offsetY = { item.offsetY },
                                slideOut = side * ep,
                                onBounds = { rect, w, h ->
                                    item.bounds = rect
                                    item.layoutW = w
                                    item.layoutH = h
                                },
                                onClick = {
                                    if (idx != target) {
                                        settleTo(idx)
                                    } else {
                                        val b = item.bounds
                                        onOpen(
                                            CardAnchor(
                                                presetId = p.id,
                                                left = b.left,
                                                top = b.top,
                                                width = item.layoutW.takeIf { it > 0f } ?: b.width,
                                                height = item.layoutH.takeIf { it > 0f } ?: b.height,
                                            ),
                                        )
                                    }
                                },
                            ) {
                                PresetCardFace(
                                    preset = p,
                                    layoutNumber = (presets.indexOfFirst { it.id == p.id } + 1).coerceAtLeast(1),
                                    onRemove = { removePreset(p) },
                                )
                            }
                        }
                    }
                }

                key("add-card") {
                    val n = visible.size
                    val addDescription = stringResource(R.string.add_card_description)
                    StackCard(
                        slotIndex = n,
                        pos = { pos },
                        wPx = wPx,
                        hPx = hPx,
                        w = cardW,
                        h = cardH,
                        animateIn = false,
                        offsetY = { 0f },
                        slideOut = if (editId == null) 0f else ep,
                        onBounds = { _, _, _ -> },
                        onClick = {
                            if (n != target) {
                                settleTo(n)
                            } else {
                                onAdd()
                                settleTo(n)
                            }
                        },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RekeyGlyph(
                                glyph = Glyph.PLUS,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(40.dp).semantics { contentDescription = addDescription },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val n = presets.count { !stateFor(it).gone }
        val cur = pos.roundToInt().coerceIn(0, n)
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            Modifier.fillMaxWidth().zIndex(1f).testTag("card_position"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(if (cur < n) two(cur + 1) else "+", color = muted, fontSize = 13.sp)
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(muted.copy(alpha = 0.25f)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (n > 0) (pos / n).coerceIn(0f, 1f) else 1f
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(two(n), color = muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StackCard(
    slotIndex: Int,
    pos: () -> Float,
    wPx: Float,
    hPx: Float,
    w: Dp,
    h: Dp,
    animateIn: Boolean,
    offsetY: () -> Float,
    slideOut: Float,
    onBounds: (Rect, Float, Float) -> Unit,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val slot = animateFloatAsState(
        targetValue = slotIndex.toFloat(),
        animationSpec = SlotSpring,
        label = "slot",
    )
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f)) else appear.snapTo(1f)
    }
    val entering by remember { derivedStateOf { appear.value < 0.98f } }
    val z by remember { derivedStateOf { 1000 - (abs(slot.value - pos()) * 20f).roundToInt() } }

    Box(
        Modifier
            .size(w, h)
            .zIndex(z.toFloat() + if (entering) 2f else 0f)
            .graphicsLayer {
                val d = slot.value - pos()
                val a = abs(d)
                val s = sign(d)
                val sin1 = sin(PI.toFloat() * a)
                val bump = if (a < 1f) sin1 * sin1 else 0f
                translationX = wPx * (0.32f * tanh(0.75f * d) + 0.38f * s * bump) + slideOut * size.width * 1.35f

                val ap = appear.value
                val drag = offsetY()
                val down = (drag / hPx).coerceIn(0f, 0.5f)
                translationY = (1f - ap) * hPx * 0.5f + if (drag > 0f) drag * 0.45f else drag
                rotationZ = if (drag > 0f) 0f else (drag / hPx).coerceIn(-1f, 1f) * 3f

                val beyond = ((a - 1f) / 0.3f).coerceIn(0f, 1f)
                val visibility = 1f - beyond * beyond * (3f - 2f * beyond)
                val sc = (0.7f + 0.3f * exp(-a * a * 0.35f)) * (0.86f + 0.14f * ap)
                scaleX = sc * (1f - down * 0.10f)
                // Pulling down stretches the card instead of moving it away.
                scaleY = sc * (1f + down * 0.34f)
                alpha = visibility * (ap * 1.8f).coerceIn(0f, 1f) * (1f - abs(slideOut)).coerceIn(0f, 1f)
            }
            .onGloballyPositioned { coords ->
                val origin = coords.positionInRoot()
                onBounds(
                    Rect(origin.x, origin.y, origin.x + coords.size.width, origin.y + coords.size.height),
                    coords.size.width.toFloat(),
                    coords.size.height.toFloat(),
                )
            }
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), CardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) { content() }
}

/**
 * The card face: "Layout N", a frosted X, a real keyboard preview, the keyboard
 * name and its details. A brand-new card shows "No name" / "Tap to edit".
 */
@Composable
fun PresetCardFace(
    preset: KeyboardPreset,
    layoutNumber: Int,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Haze cannot blur an ancestor source, so the face is the source and the X
    // is drawn as a sibling on top of it — never inside it.
    val faceBlur = rememberLiveBlurState()
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().liveBlurSource(faceBlur)) {
            PresetCardContents(preset, layoutNumber, hasClose = onRemove != null)
        }
        if (onRemove != null) {
            FrostedCross(
                blurState = faceBlur,
                description = stringResource(R.string.remove_card_description, layoutNumber),
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 14.dp),
            )
        }
    }
}

@Composable
private fun PresetCardContents(
    preset: KeyboardPreset,
    layoutNumber: Int,
    hasClose: Boolean,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.fillMaxSize().padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.card_layout_label, layoutNumber),
                color = onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            if (hasClose) Spacer(Modifier.size(30.dp))
        }
        KeyboardPreview(
            palette = preset.palette,
            modifier = Modifier.fillMaxWidth().aspectRatio(KeyboardCanvas.RATIO),
            cornerRadius = 10.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (preset.placeholder) stringResource(R.string.editor_unnamed) else preset.name,
                color = onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (preset.placeholder) stringResource(R.string.editor_tap_to_edit) else preset.details,
                color = onSurface.copy(alpha = 0.68f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The X: a live-blurred frosted disc with a translucent fallback wash. */
@Composable
private fun FrostedCross(
    blurState: HazeState,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "xScale",
    )
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .size(30.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .liveBlur(blurState, fullScreenBlur(GlassContract.BLUR_DP.dp))
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RekeyGlyph(Glyph.CROSS, tint, Modifier.size(15.dp))
    }
}

/**
 * The dismissed card, drawn above everything else at root level. It keeps its
 * exact size and start position, then travels past the top edge — over the
 * header, never clipped by it.
 */
@Composable
fun FlyingCardLayer(card: FlyingCard, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(card.key) {
        progress.animateTo(1f, tween(MotionContract.CARD_EXIT_MILLIS, easing = ExitEasing))
        onFinished()
    }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val travel = card.top + card.height + 60f
        Box(
            Modifier
                .offset {
                    IntOffset(
                        card.left.roundToInt(),
                        (card.top + card.direction * travel * progress.value).roundToInt(),
                    )
                }
                .size(
                    with(density) { card.width.toDp() },
                    with(density) { card.height.toDp() },
                )
                .graphicsLayer {
                    val p = progress.value
                    alpha = (1f - p * p).coerceIn(0f, 1f)
                    val sc = 1f - 0.06f * p
                    scaleX = sc
                    scaleY = sc
                    rotationZ = card.rotation * (1f - p)
                }
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), CardShape),
        ) {
            PresetCardFace(card.preset, card.layoutNumber, onRemove = null)
        }
    }
}
