package io.github.lxzrvi.rekey.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.lxzrvi.rekey.design.BrandContract
import io.github.lxzrvi.rekey.ui.brand.BrandTypography
import io.github.lxzrvi.rekey.ui.theme.BrandWhite

/** A white wordmark on the dark brand surface. Treat its dimensions like a logo, not body text. */
@Composable
fun RekeyWordmark(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maximumSize = minOf(maxWidth / 4.1f, 56.dp)
        val size = with(LocalDensity.current) { maximumSize.toSp() }
        Text(
            text = BrandContract.APP_NAME,
            color = BrandWhite,
            style = BrandTypography.wordmark.copy(fontSize = size, lineHeight = size * 1.15f),
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = BrandContract.APP_NAME
            },
        )
    }
}
