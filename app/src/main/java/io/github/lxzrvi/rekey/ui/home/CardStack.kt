package io.github.lxzrvi.rekey.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.design.GlassContract
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

private val Names = listOf(
    "Aurora", "Ember", "Tidal", "Onyx", "Velvet", "Solstice",
    "Zenith", "Mirage", "Cobalt", "Saffron", "Lumen", "Opal",
)

private val Descs = listOf(
    "Swipe sideways to bring the next box forward.",
    "Drag left or right and the stack follows.",
    "Tap a box behind to pull it to the center.",
    "Use the circle to remove a box.",
    "Every new box joins the end of the stack.",
)

/** Swiping: no bounce, just smooth. */
private val Smooth = spring<Float>(dampingRatio = 1f, stiffness = 120f)

/** Re-slotting after add/remove: snappier, still no bounce. */
private val SlotSpring = spring<Float>(dampingRatio = 1f, stiffness = 200f)

@Stable
private class CardItem(val id: Int, val animateIn: Boolean = false) {
    var removing by mutableStateOf(false)
    var gone by mutableStateOf(false)
    var frozenIndex = 0
}

private fun two(i: Int) = i.toString().padStart(2, '0')

/**
 * Draggable card stack. A new card rises from below with a light overshoot;
 * a removed card flies upward, then the remaining cards close the gap.
 */
@Composable
fun CardStack(
    settings: AppearanceSettings,
    preferences: RekeyPreferences,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val animated = settings.animationsEnabled
    val initialCount = remember { preferences.loadCardCount().coerceIn(0, 10_000) }
    val cards = remember {
        mutableStateListOf<CardItem>().apply { for (i in 1..initialCount) add(CardItem(i)) }
    }
    var nextId by remember { mutableIntStateOf(initialCount + 1) }
    val savedIndex = remember { preferences.loadSelectedCard().coerceIn(0, initialCount) }
    var pos by remember { mutableFloatStateOf(if (animated) savedIndex + 1.6f else savedIndex.toFloat()) }
    var target by remember { mutableIntStateOf(savedIndex) }
    val job = remember { arrayOfNulls<Job>(1) }

    fun liveCount() = cards.count { !it.gone }

    fun settleTo(t: Int, v0: Float = 0f) {
        target = t
        preferences.saveSelectedCard(t)
        job[0]?.cancel()
        if (!animated) {
            pos = t.toFloat()
            return
        }
        job[0] = scope.launch {
            animate(
                initialValue = pos,
                targetValue = t.toFloat(),
                initialVelocity = v0,
                animationSpec = Smooth,
            ) { value, _ -> pos = value }
        }
    }

    fun addBox() {
        val n = liveCount()
        cards.add(CardItem(nextId++, animateIn = animated))
        preferences.saveCardCount(liveCount())
        settleTo(n)
    }

    fun removeBox(item: CardItem) {
        if (item.removing || item.gone) return
        item.removing = true
        scope.launch {
            delay(if (animated) 130L else 0L)
            val live = cards.filter { !it.gone }
            val r = live.indexOf(item)
            if (r < 0) return@launch
            item.frozenIndex = r
            item.gone = true
            var t = target
            if (r < t) t--
            settleTo(t.coerceIn(0, live.size - 1))
            preferences.saveCardCount(live.size - 1)
            delay(if (animated) 450L else 0L)
            cards.remove(item)
        }
    }

    LaunchedEffect(Unit) { settleTo(savedIndex) }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).testTag("card_stack")) {
            val cardW = minOf(maxWidth * 0.86f, 420.dp)
            val cardH = minOf(cardW * 0.62f, maxHeight * 0.7f)
            val density = LocalDensity.current
            val wPx = with(density) { cardW.toPx() }
            val hPx = with(density) { cardH.toPx() }
            val stepPx = wPx * 0.5f

            val dragState = rememberDraggableState { delta ->
                val n = liveCount().toFloat()
                val d = -delta / stepPx
                val out = (pos <= 0f && d < 0f) || (pos >= n && d > 0f)
                pos += if (out) d * 0.35f else d
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = { job[0]?.cancel() },
                        onDragStopped = { velocity ->
                            val n = liveCount()
                            val v = (-velocity / stepPx).coerceIn(-10f, 10f)
                            settleTo((pos + v * 0.16f).roundToInt().coerceIn(0, n), v)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val live = cards.filter { !it.gone }

                cards.forEach { item ->
                    key(item.id) {
                        val idx = if (item.gone) item.frozenIndex else live.indexOf(item)
                        StackCard(
                            slotIndex = idx,
                            pos = { pos },
                            wPx = wPx,
                            hPx = hPx,
                            w = cardW,
                            h = cardH,
                            animateIn = item.animateIn,
                            removing = item.removing,
                            animated = animated,
                            onClick = { if (!item.removing && idx != target) settleTo(idx) },
                        ) {
                            BoxFace(item, animated) { removeBox(item) }
                        }
                    }
                }

                key("add-card") {
                    val n = live.size
                    val addDescription = stringResource(R.string.add_card_description)
                    StackCard(
                        slotIndex = n,
                        pos = { pos },
                        wPx = wPx,
                        hPx = hPx,
                        w = cardW,
                        h = cardH,
                        animateIn = false,
                        removing = false,
                        animated = animated,
                        onClick = { if (target != n) settleTo(n) else addBox() },
                    ) {
                        PlusButton(
                            modifier = Modifier.align(Alignment.Center).testTag("add_card"),
                            description = addDescription,
                            animated = animated,
                        )
                    }
                }
            }
        }

        val n = cards.count { !it.gone }
        val cur by remember {
            derivedStateOf { pos.roundToInt().coerceIn(0, cards.count { !it.gone }) }
        }
        val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
        val fillColor = MaterialTheme.colorScheme.onBackground
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp).testTag("card_position"),
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
                    .background(trackColor),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val nn = cards.count { !it.gone }
                            scaleX = if (nn > 0) (pos / nn).coerceIn(0f, 1f) else 1f
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .background(fillColor),
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
    removing: Boolean,
    animated: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val slot = animateFloatAsState(
        targetValue = slotIndex.toFloat(),
        animationSpec = if (animated) SlotSpring else tween<Float>(0),
        label = "slot",
    )

    // ADD: the new box rises from below with a slight spring overshoot.
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f)) else appear.snapTo(1f)
    }

    // REMOVE: the card accelerates upward and fades out.
    val exit = animateFloatAsState(
        targetValue = if (removing) 1f else 0f,
        animationSpec = tween(if (animated) 430 else 0, easing = CubicBezierEasing(0.5f, 0f, 1f, 0.6f)),
        label = "exit",
    )

    val entering by remember { derivedStateOf { appear.value < 0.98f } }
    val z by remember { derivedStateOf { 1000 - (abs(slot.value - pos()) * 20f).roundToInt() } }
    val zi = if (removing) 5000f else z.toFloat() + if (entering) 2f else 0f

    Box(
        Modifier
            .size(w, h)
            .zIndex(zi)
            .graphicsLayer {
                val d = slot.value - pos()
                val a = abs(d)
                val s = sign(d)
                val sin1 = sin(PI.toFloat() * a)
                val bump = if (a < 1f) sin1 * sin1 else 0f
                translationX = wPx * (0.32f * tanh(0.75f * d) + 0.38f * s * bump)

                val ap = appear.value
                val ex = exit.value
                translationY = (1f - ap) * hPx * 0.5f - ex * hPx * 1.3f

                val t = ((a - 2.4f) / 1.4f).coerceIn(0f, 1f)
                val fade = 1f - t * t * (3f - 2f * t)
                val sc = (0.7f + 0.3f * exp(-a * a * 0.35f)) *
                    (0.86f + 0.14f * ap) *
                    (1f - 0.06f * ex)
                scaleX = sc
                scaleY = sc
                alpha = fade * (ap * 1.8f).coerceIn(0f, 1f) * (1f - ex * ex).coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(GlassContract.CORNER_DP.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val a = abs(slot.value - pos())
                    alpha = 0.62f * (1f - exp(-a * a * 0.9f))
                }
                .background(Color.Black),
        )
    }
}

@Composable
private fun BoxFace(item: CardItem, animated: Boolean, onRemove: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val removeDescription = stringResource(R.string.remove_card_description, item.id)
    Column(
        Modifier.fillMaxSize().padding(start = 18.dp, top = 14.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(R.string.card_box_label, item.id),
                color = onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 7.dp),
            )
            val src = remember { MutableInteractionSource() }
            val pressed by src.collectIsPressedAsState()
            val xScale by animateFloatAsState(
                targetValue = if (pressed) 0.84f else 1f,
                animationSpec = if (animated) spring(dampingRatio = 0.6f, stiffness = 500f) else tween<Float>(0),
                label = "xScale",
            )
            Box(
                Modifier
                    .size(32.dp)
                    .graphicsLayer { scaleX = xScale; scaleY = xScale }
                    .clip(CircleShape)
                    .background(onSurface)
                    .clickable(interactionSource = src, indication = null, onClick = onRemove)
                    .semantics { contentDescription = removeDescription },
                contentAlignment = Alignment.Center,
            ) {
                CrossGlyph(color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp))
            }
        }
        Column {
            Text(
                text = Names[(item.id - 1) % Names.size],
                color = onSurface,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Descs[(item.id - 1) % Descs.size],
                color = onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun PlusButton(modifier: Modifier, description: String, animated: Boolean) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = if (animated) spring(dampingRatio = 0.6f, stiffness = 500f) else tween<Float>(0),
        label = "plusScale",
    )
    Box(
        modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        PlusGlyph(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun PlusGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val half = size.minDimension * 0.36f
        val stroke = 2.6.dp.toPx()
        drawLine(color, c - Offset(half, 0f), c + Offset(half, 0f), stroke, StrokeCap.Round)
        drawLine(color, c - Offset(0f, half), c + Offset(0f, half), stroke, StrokeCap.Round)
    }
}

@Composable
private fun CrossGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val half = size.minDimension * 0.34f
        val stroke = 2.2.dp.toPx()
        drawLine(color, c - Offset(half, half), c + Offset(half, half), stroke, StrokeCap.Round)
        drawLine(color, c - Offset(half, -half), c + Offset(half, -half), stroke, StrokeCap.Round)
    }
}
