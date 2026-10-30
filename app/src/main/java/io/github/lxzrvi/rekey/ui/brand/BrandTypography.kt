package io.github.lxzrvi.rekey.ui.brand

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R

/** Allura is reserved exclusively for the exact REKEY wordmark. Body text stays Poppins. */
val Allura = FontFamily(Font(R.font.allura_regular, FontWeight.Normal))

object BrandTypography {
    val wordmark = TextStyle(
        fontFamily = Allura,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = 0.sp,
    )
}
