package io.github.lxzrvi.rekey.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.ui.theme.BrandBlack
import io.github.lxzrvi.rekey.ui.theme.BrandWhite

/** Startup shows only the REKEY wordmark. No spinner, no loader, no icon. */
@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(BrandBlack).testTag("splash"),
        contentAlignment = Alignment.Center,
    ) {
        RekeyWordmark(fontSize = 72.sp, color = BrandWhite)
    }
}
