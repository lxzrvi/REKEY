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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import io.github.lxzrvi.rekey.R
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.ui.glass.glass
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Center card is full size; neighbors sit behind it. The final page always adds another demo. */
@Composable
fun CardCarousel(
    settings: AppearanceSettings,
    glassState: HazeState,
    preferences: RekeyPreferences,
    modifier: Modifier = Modifier,
) {
    var cardCount by rememberSaveable { mutableIntStateOf(preferences.loadCardCount()) }
    val pagerState = rememberPagerState(
        initialPage = preferences.loadSelectedCard().coerceAtMost(cardCount),
        pageCount = { cardCount + 1 },
    )
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.settledPage) {
        preferences.saveSelectedCard(pagerState.settledPage)
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(224.dp).testTag("card_carousel")) {
            val cardWidth = minOf(252.dp, maxWidth - 72.dp)
            val sidePadding = ((maxWidth - cardWidth) / 2).coerceAtLeast(16.dp)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = PageSize.Fixed(cardWidth),
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val offset = pagerState.getOffsetDistanceInPages(page).coerceIn(-1.5f, 1.5f)
                val distance = abs(offset).coerceAtMost(1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f - distance)
                        .graphicsLayer {
                            val scale = 1f - 0.16f * distance
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.24f * distance
                            translationX = -offset * 38.dp.toPx()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (page < cardCount) {
                        DemoCard(settings, glassState, page)
                    } else {
                        AddCard(
                            settings = settings,
                            glassState = glassState,
                            enabled = !adding,
                            onAdd = {
                                if (!adding && cardCount < 10_000) {
                                    adding = true
                                    val newIndex = cardCount
                                    cardCount += 1
                                    preferences.saveCardCount(cardCount)
                                    scope.launch {
                                        try {
                                            if (settings.animationsEnabled) pagerState.animateScrollToPage(newIndex)
                                            else pagerState.scrollToPage(newIndex)
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
        Text(
            text = if (pagerState.currentPage < cardCount) {
                stringResource(R.string.card_position, pagerState.currentPage + 1, cardCount)
            } else {
                stringResource(R.string.add_card_description)
            },
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
private fun DemoCard(settings: AppearanceSettings, glassState: HazeState, index: Int) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (settings.highContrast) 0.9f else 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .testTag("demo_card_$index")
            .glass(glassState, MaterialTheme.shapes.large, settings.blurRadius.dp, settings.blurEnabled),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.demo_text),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddCard(
    settings: AppearanceSettings,
    glassState: HazeState,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val description = stringResource(R.string.add_card_description)
    Surface(
        onClick = onAdd,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (settings.highContrast) 0.9f else 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .testTag("add_card")
            .semantics { contentDescription = description }
            .glass(glassState, MaterialTheme.shapes.large, settings.blurRadius.dp, settings.blurEnabled),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.add), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
