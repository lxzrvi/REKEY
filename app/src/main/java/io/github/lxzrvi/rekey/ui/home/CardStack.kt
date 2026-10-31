package io.github.lxzrvi.rekey.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.KeyboardPalettes
import io.github.lxzrvi.rekey.data.KeyboardPreset
import io.github.lxzrvi.rekey.design.GlassContract
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.ui.components.Glyph
import io.github.lxzrvi.rekey.ui.components.rememberUriImage
import io.github.lxzrvi.rekey.ui.components.RekeyGlyph
import io.github.lxzrvi.rekey.ui.keyboard.KeyboardCanvas
import io.github.lxzrvi.rekey.ui.keyboard.KeyboardPreviewSuggestions
import io.github.lxzrvi.rekey.ui.keyboard.ScaledKeyboard
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
// Critically damped: an underdamped return overshoots past zero, which
// showed up as the card shaking and rotating after a downward pull.
private val ReturnSpring = spring<Float>(dampingRatio = 1f, stiffness = 220f)

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
    val shape: Shape,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val direction: Float,
    val rotation: Float,
    /** Finger speed at release, px/s. The flight continues at exactly this speed. */
    val velocityY: Float = 0f,
)

/**
 * The card while a finger is still on it. The stack lives inside a scrolling
 * column that clips at the header, so an upward drag is handed to a root-level
 * overlay that tracks the finger past the header instead of being cut off.
 */
@Stable
class LiveDrag(
    val preset: KeyboardPreset,
    val layoutNumber: Int,
    val shape: Shape,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val hPx: Float,
    val offsetY: () -> Float,
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
private class CardState(val id: Int, var animateIn: Boolean = true) {
    var removing by mutableStateOf(false)
    var dragging by mutableStateOf(false)
    /** True while the root overlay, not this stack copy, owns the visuals. */
    var overlay by mutableStateOf(false)
    var gone by mutableStateOf(false)
    var frozenIndex = 0
    var offsetY by mutableFloatStateOf(0f)
    /** Unresisted finger travel. Resistance is a function of THIS, never of offsetY. */
    var rawY = 0f
    var returnJob: Job? = null
    var bounds: Rect = Rect.Zero
    var layoutW = 0f
    var layoutH = 0f
}

private fun two(i: Int) = i.toString().padStart(2, '0')

/**
 * The card is a preview of the real keyboard, so it carries the same corner
 * radii, scaled into the card's width. Changing a corner in the wizard changes
 * the card on the main page too.
 */
private fun cardShapeFor(l: io.github.lxzrvi.rekey.data.LayoutStyle, scale: Float): Shape =
    RoundedCornerShape(
        topStart = (l.cornerTopLeft * scale).dp,
        topEnd = (l.cornerTopRight * scale).dp,
        bottomStart = (l.cornerBottomLeft * scale).dp,
        bottomEnd = (l.cornerBottomRight * scale).dp,
    )

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
    onLiveDrag: (LiveDrag?) -> Unit = {},
    onNotice: (String) -> Unit = {},
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
    val cardScaleHolder = remember { floatArrayOf(1f) }
    val seeded = remember { mutableStateOf(false) }
    val defaultNotice = stringResource(R.string.notice_default_preset)

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

    fun removePreset(p: KeyboardPreset, velocityY: Float = 0f) {
        // The shipped layouts are the fallback set. Both the X and the upward
        // swipe land here, so one guard covers both and says why it refused.
        if (KeyboardPalettes.isDefault(p)) {
            onNotice(defaultNotice)
            return
        }
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
                    shape = cardShapeFor(p.style.layout, cardScaleHolder[0]),
                    left = b.left,
                    top = b.top,
                    width = item.layoutW.takeIf { it > 0f } ?: b.width,
                    height = item.layoutH.takeIf { it > 0f } ?: b.height,
                    direction = -1f,
                    rotation = (item.offsetY / hPx).coerceIn(-1f, 1f) * 3f,
                    velocityY = velocityY,
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
            val cardH = cardW / KeyboardCanvas.CARD_RATIO
            val density = LocalDensity.current
            val wPx = with(density) { cardW.toPx() }
            val hPx = with(density) { cardH.toPx() }
            cardHPxHolder[0] = hPx
            cardScaleHolder[0] = cardW.value / KeyboardCanvas.WIDTH_VALUE
            val stepPx = wPx * 0.5f

            val dragState = rememberDraggableState { delta ->
                val n = live().size.toFloat()
                val d = -delta / stepPx
                val out = (pos <= 0f && d < 0f) || (pos >= n && d > 0f)
                pos += if (out) d * 0.35f else d
            }

            // The stack copy and the overlay must never both draw, and never both
            // hide. CardStack decides, once, and publishes the result; the page
            // just renders whatever it is handed.
            fun setLifted(p: KeyboardPreset, item: CardState, want: Boolean) {
                if (item.overlay == want) return
                item.overlay = want
                if (!want) {
                    onLiveDrag(null)
                    return
                }
                onLiveDrag(
                    LiveDrag(
                        preset = p,
                        layoutNumber = (presets.indexOfFirst { it.id == p.id } + 1).coerceAtLeast(1),
                        shape = cardShapeFor(p.style.layout, cardW.value / KeyboardCanvas.WIDTH_VALUE),
                        left = item.bounds.left,
                        top = item.bounds.top - item.offsetY,
                        width = item.layoutW.takeIf { it > 0f } ?: item.bounds.width,
                        height = item.layoutH.takeIf { it > 0f } ?: item.bounds.height,
                        hPx = hPx,
                        offsetY = { item.offsetY },
                    ),
                )
            }

            val verticalDrag = rememberDraggableState { delta ->
                val p = live().getOrNull(target) ?: return@rememberDraggableState
                val item = stateFor(p)
                if (item.removing) return@rememberDraggableState
                item.returnJob?.cancel()
                // Pulling down moves the whole card, it never deforms it. The
                // resistance grows with distance so it feels progressively
                // heavier, and it can never reach a dismissal threshold.
                item.rawY += delta
                item.offsetY = if (item.rawY > 0f) {
                    val limit = hPx * 0.42f
                    val t = (item.rawY / limit).coerceAtLeast(0f)
                    (limit * (1f - 1f / (1f + t * 1.6f))).coerceAtMost(limit)
                } else {
                    item.rawY
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cardH + 24.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            job[0]?.cancel()
                            // A horizontal swipe must not inherit a half-finished
                            // vertical drag: the overlay is pinned to one x, so
                            // leaving it up while the stack scrolls is the glitch.
                            live().forEach { q ->
                                val st = stateFor(q)
                                if (st.dragging || st.offsetY != 0f || st.overlay) {
                                    st.returnJob?.cancel()
                                    st.rawY = 0f
                                    st.offsetY = 0f
                                    st.dragging = false
                                    if (st.overlay) {
                                        st.overlay = false
                                        onLiveDrag(null)
                                    }
                                }
                            }
                        },
                        onDragStopped = { velocity ->
                            val n = live().size
                            val v = (-velocity / stepPx).coerceIn(-10f, 10f)
                            settleTo((pos + v * 0.16f).roundToInt().coerceIn(0, n), v)
                        },
                    )
                    .draggable(
                        state = verticalDrag,
                        orientation = Orientation.Vertical,
                        onDragStarted = {
                            val p = live().getOrNull(target)
                            if (p != null) {
                                val item = stateFor(p)
                                if (!item.removing) {
                                    item.returnJob?.cancel()
                                    item.rawY = item.offsetY
                                    item.dragging = true
                                    // Hand over at the current offset, where the
                                    // two copies are pixel-identical. Swapping at
                                    // a threshold mid-gesture is what looked like
                                    // a jump; swapping at rest is invisible.
                                    setLifted(p, item, true)
                                }
                            }
                        },
                        onDragStopped = { velocity ->
                            val p = live().getOrNull(target)
                            if (p != null) {
                                val item = stateFor(p)
                                if (!item.removing) {
                                    val up = item.offsetY < 0f
                                    val far = -item.offsetY > hPx * 0.26f
                                    val fast = velocity < -900f && -item.offsetY > hPx * 0.05f
                                    val dismiss = up && (far || fast)
                                    // A refused dismissal must still spring home,
                                    // otherwise a default card stays stuck in the air.
                                    if (dismiss && !KeyboardPalettes.isDefault(p)) {
                                        item.dragging = false
                                        removePreset(p, velocity)
                                        setLifted(p, item, false)
                                    } else {
                                        if (dismiss) onNotice(defaultNotice)
                                        item.returnJob = scope.launch {
                                            animate(
                                                initialValue = item.offsetY,
                                                targetValue = 0f,
                                                // Always start from rest. Even a
                                                // critically damped spring crosses
                                                // zero once if it is handed a velocity,
                                                // and that crossing was the up-down
                                                // shake after a downward pull.
                                                initialVelocity = 0f,
                                                animationSpec = ReturnSpring,
                                            ) { value, _ -> item.offsetY = value }
                                            // Hand the card back to the stack only
                                            // once it is genuinely home again.
                                            item.rawY = 0f
                                            item.dragging = false
                                            setLifted(p, item, false)
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
                                onEntered = { item.animateIn = false },
                                hidden = { item.overlay },
                                shape = cardShapeFor(p.style.layout, cardW.value / KeyboardCanvas.WIDTH_VALUE),
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
                                PresetCardFace(preset = p)
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
                        onEntered = {},
                        hidden = { false },
                        shape = CardShape,
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

        // Layout number, name and details, centred under the progress row. The
        // card itself stays a clean keyboard preview.
        Spacer(Modifier.height(14.dp))
        val current = live().getOrNull(cur)
        val currentNumber = if (current == null) 0 else presets.indexOfFirst { it.id == current.id } + 1
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("card_caption"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = if (current != null) {
                    stringResource(R.string.card_layout_label, currentNumber).uppercase()
                } else {
                    stringResource(R.string.card_new_label).uppercase()
                },
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = when {
                    current == null -> stringResource(R.string.card_new_title)
                    current.placeholder -> stringResource(R.string.editor_unnamed)
                    else -> current.name
                },
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            val sub = when {
                current == null -> stringResource(R.string.card_new_hint)
                current.placeholder -> stringResource(R.string.editor_tap_to_edit)
                else -> current.details
            }
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (current != null) {
                Spacer(Modifier.height(6.dp))
                FrostedCross(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    paletteCanvas = MaterialTheme.colorScheme.surface,
                    description = stringResource(R.string.remove_card_description, currentNumber),
                    onClick = { removePreset(current) },
                )
            }
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
    onEntered: () -> Unit,
    hidden: () -> Boolean,
    shape: Shape,
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
        onEntered()
    }
    val entering by remember { derivedStateOf { appear.value < 0.98f } }
    // Continuous distance, never quantised. Banding made two neighbours share a
    // z value, and then declaration order decided which edge drew on top — the
    // edges swapped front-to-back mid-swipe. A float ranking is always strict.
    val z by remember { derivedStateOf { 1000f - abs(slot.value - pos()) } }
    // A card carries a full keyboard. Composing all of them costs far more than
    // the two neighbours the user can actually see, so gate the content on
    // distance and only recompose when that boolean flips.
    val showContent by remember { derivedStateOf { abs(slot.value - pos()) < 2.0f } }

    Box(
        Modifier
            .size(w, h)
            .zIndex(z + if (entering) 2f else 0f)
            .graphicsLayer {
                val d = slot.value - pos()
                val a = abs(d)
                val s = sign(d)
                val sin1 = sin(PI.toFloat() * a)
                val bump = if (a < 1f) sin1 * sin1 else 0f
                translationX = wPx * (0.32f * tanh(0.75f * d) + 0.38f * s * bump) + slideOut * size.width * 1.35f

                val ap = appear.value
                val drag = offsetY()
                translationY = (1f - ap) * hPx * 0.5f + drag
                rotationZ = if (drag > 0f) 0f else (drag / hPx).coerceIn(-1f, 1f) * 3f

                val beyond = ((a - 1f) / 0.3f).coerceIn(0f, 1f)
                val visibility = 1f - beyond * beyond * (3f - 2f * beyond)
                val sc = (0.7f + 0.3f * exp(-a * a * 0.35f)) * (0.86f + 0.14f * ap)
                // The card keeps its shape at all times — no stretch on pull-down.
                scaleX = sc
                scaleY = sc
                alpha = visibility * (ap * 1.8f).coerceIn(0f, 1f) *
                    (1f - abs(slideOut)).coerceIn(0f, 1f) *
                    // While the root overlay owns the drag, this copy stays hidden.
                    (if (hidden()) 0f else 1f)
            }
            .onGloballyPositioned { coords ->
                val origin = coords.positionInRoot()
                onBounds(
                    Rect(origin.x, origin.y, origin.x + coords.size.width, origin.y + coords.size.height),
                    coords.size.width.toFloat(),
                    coords.size.height.toFloat(),
                )
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) { if (showContent) content() }
}

/**
 * The card *is* the keyboard.
 *
 * The layout is drawn edge-to-edge with no inner frame, and "Layout N" plus the
 * X sit on translucent panels over its header strip.
 *
 * These panels deliberately do NOT use a live blur. What sits behind them is the
 * card's own static keyboard, so blurring it live adds nothing visually — but a
 * Haze source needs a frame to capture whenever the card is re-hosted (drag
 * overlay, dismissal flight, editor), and that empty frame is exactly the
 * flicker seen while dragging and swiping. A dense translucent panel looks the
 * same and is correct on the very first frame.
 */
@Composable
fun PresetCardFace(
    preset: KeyboardPreset,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        // Nothing but the keyboard and its suggestion bar. The layout number,
        // name and details live under the progress row, and the remove control
        // sits with them, so the preview is never covered by chrome.
        val bg = rememberUriImage(
            preset.style.background.imageUri.takeIf { preset.style.background.useImage },
            maxPx = 720,
        )
        ScaledKeyboard(
            palette = preset.palette,
            modifier = Modifier.fillMaxWidth(),
            style = preset.style,
            backgroundImage = bg,
            showToolbar = true,
            interactive = false,
            suggestions = KeyboardPreviewSuggestions,
        )
    }
}

/** The X: a live-blurred frosted disc with a translucent fallback wash. */
@Composable
private fun FrostedCross(
    tint: Color,
    paletteCanvas: Color,
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
    Box(
        modifier
            .size(30.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(paletteCanvas.copy(alpha = 0.88f))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RekeyGlyph(Glyph.CROSS, tint, Modifier.size(15.dp))
    }
}

/**
 * The card under the finger, drawn at root level so an upward drag is visible
 * over the header while it is still happening. It reads the same offset the
 * stack uses, so there is no jump when the drag ends: either the flight takes
 * over from this exact position, or the card springs home and the stack copy
 * fades back in.
 */
@Composable
fun LiveDragLayer(drag: LiveDrag) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset {
                    IntOffset(drag.left.roundToInt(), (drag.top + drag.offsetY()).roundToInt())
                }
                .size(
                    with(density) { drag.width.toDp() },
                    with(density) { drag.height.toDp() },
                )
                .graphicsLayer {
                    val d = drag.offsetY()
                    rotationZ = if (d > 0f) 0f else (d / drag.hPx).coerceIn(-1f, 1f) * 3f
                }
                .clip(drag.shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), drag.shape),
        ) {
            PresetCardFace(drag.preset)
        }
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
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val travel = card.top + card.height + 60f
        // Constant speed equal to the release speed, so the card keeps going
        // exactly as it was thrown. An ease-in tween restarted it from zero,
        // which is the separate little jolt after letting go.
        val speed = maxOf(abs(card.velocityY), travel / 0.7f)
        val exitMillis = (travel / speed * 1000f).toInt().coerceIn(120, 700)
        LaunchedEffect(card.key) {
            progress.animateTo(1f, tween(exitMillis, easing = LinearEasing))
            onFinished()
        }
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
                    rotationZ = card.rotation * (1f + 0.6f * p)
                }
                .clip(card.shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), card.shape),
        ) {
            PresetCardFace(card.preset)
        }
    }
}
