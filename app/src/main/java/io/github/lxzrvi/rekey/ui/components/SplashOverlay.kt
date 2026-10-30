package io.github.lxzrvi.rekey.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.ui.theme.BrandBlack
import io.github.lxzrvi.rekey.ui.theme.BrandWhite

/**
 * Startup shows only the REKEY wordmark. No spinner, no loader, no icon, and no
 * zoom on the way out — the app simply cross-fades in at its natural size.
 */
@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(BrandBlack).testTag("splash").padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Cinzel Decorative is wide: keep REKEY on one line on every screen.
        val size = (maxWidth.value / 3.95f).coerceIn(28f, 64f).sp
        RekeyWordmark(fontSize = size, color = BrandWhite)
    }
}
