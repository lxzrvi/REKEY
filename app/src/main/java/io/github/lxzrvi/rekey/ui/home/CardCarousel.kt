package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Infinite circular carousel: wider cards rotate/scale as if passing behind the center card. */
@Composable
fun CardCarousel(
    settings: AppearanceSettings,
    preferences: RekeyPreferences,
    modifier: Modifier = Modifier,
) {
    var cardCount by rememberSaveable { mutableIntStateOf(preferences.loadCardCount()) }
    val itemCount = cardCount + 1
    val base = 100_000 - (100_000 % itemCount)
    val pagerState = rememberPagerState(
        initialPage = base + preferences.loadSelectedCard().coerceAtMost(itemCount - 1),
        pageCount = { Int.MAX_VALUE },
    )
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }

    fun itemFor(page: Int): Int = Math.floorMod(page, itemCount)

    LaunchedEffect(pagerState.settledPage, itemCount) {
        preferences.saveSelectedCard(itemFor(pagerState.settledPage))
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(224.dp).testTag("card_carousel")) {
            val cardWidth = maxWidth - 40.dp
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = PageSize.Fixed(cardWidth),
                contentPadding = PaddingValues(horizontal = 20.dp),
                pageSpacing = 8.dp,
                beyondViewportPageCount = 2,
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val item = itemFor(page)
                val offset = pagerState.getOffsetDistanceInPages(page).coerceIn(-1.5f, 1.5f)
                val distance = abs(offset).coerceAtMost(1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f - distance)
                        .graphicsLayer {
                            val scale = 1f - 0.14f * distance
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.28f * distance
                            rotationY = -offset * 22f
                            cameraDistance = 16f
                            translationX = -offset * 30.dp.toPx()
                            translationZ = -distance * 24.dp.toPx()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (item < cardCount) {
                        DemoCard(settings, item)
                    } else {
                        AddCard(
                            settings = settings,
                            enabled = !adding,
                            onAdd = {
                                if (!adding && cardCount < 10_000) {
                                    adding = true
                                    val newIndex = cardCount
                                    cardCount += 1
                                    preferences.saveCardCount(cardCount)
                                    val newItemCount = cardCount + 1
                                    val start = pagerState.currentPage
                                    val target = (start + 1..start + newItemCount).first { Math.floorMod(it, newItemCount) == newIndex }
                                    scope.launch {
                                        try {
                                            if (settings.animationsEnabled) pagerState.animateScrollToPage(target)
                                            else pagerState.scrollToPage(target)
                                        } finally {
                                            adding = false
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        val current = itemFor(pagerState.currentPage)
        Text(
            text = if (current < cardCount) stringResource(R.string.card_position, current + 1, cardCount) else stringResource(R.string.add_card_description),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("card_position"),
        )
        Text(
            text = stringResource(R.string.swipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun DemoCard(settings: AppearanceSettings, index: Int) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (settings.highContrast) 0.9f else 0.4f)),
        modifier = Modifier.fillMaxWidth().height(184.dp).testTag("demo_card_$index"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.demo_text),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddCard(settings: AppearanceSettings, enabled: Boolean, onAdd: () -> Unit) {
    val description = stringResource(R.string.add_card_description)
    Surface(
        onClick = onAdd,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (settings.highContrast) 0.9f else 0.4f)),
        modifier = Modifier.fillMaxWidth().height(184.dp).testTag("add_card").semantics { contentDescription = description },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
