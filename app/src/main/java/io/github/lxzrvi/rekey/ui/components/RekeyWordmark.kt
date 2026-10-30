package io.github.lxzrvi.rekey.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.design.BrandContract
import io.github.lxzrvi.rekey.ui.brand.BrandTypography
import io.github.lxzrvi.rekey.ui.theme.BrandWhite

/** The only spelling used anywhere in the product. Never title-case or autocorrect it. */
@Composable
fun RekeyWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 64.sp,
    color: Color = BrandWhite,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = BrandContract.APP_NAME,
        color = color,
        style = BrandTypography.wordmark.copy(
            fontSize = fontSize,
            lineHeight = fontSize * 1.18f,
        ),
        textAlign = textAlign,
        maxLines = 1,
        modifier = modifier.clearAndSetSemantics { contentDescription = BrandContract.APP_NAME },
    )
}
