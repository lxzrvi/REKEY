package io.github.lxzrvi.rekey.ui.brand

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R

/** Cinzel Decorative is reserved for the exact REKEY wordmark. Body text stays Poppins. */
val CinzelDecorative = FontFamily(
    Font(R.font.cinzel_decorative_regular, FontWeight.Normal),
    Font(R.font.cinzel_decorative_bold, FontWeight.Bold),
)

object BrandTypography {
    /** Cinzel Decorative is an all-caps display face; a little tracking keeps it open. */
    const val TRACKING_EM = 0.05f

    val wordmark = TextStyle(
        fontFamily = CinzelDecorative,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 86.sp,
        letterSpacing = 3.2.sp,
    )
}
