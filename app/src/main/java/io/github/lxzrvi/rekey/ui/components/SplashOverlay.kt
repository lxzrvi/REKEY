package io.github.lxzrvi.rekey.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.ui.theme.BrandBlack
import io.github.lxzrvi.rekey.ui.theme.BrandWhite

@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(BrandBlack).testTag("splash"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RekeyWordmark(fontSize = 76.sp, color = BrandWhite)
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = BrandWhite.copy(alpha = 0.8f),
            trackColor = BrandBlack,
            strokeWidth = 1.5.dp,
        )
    }
}
