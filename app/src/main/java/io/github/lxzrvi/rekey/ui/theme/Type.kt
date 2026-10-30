package io.github.lxzrvi.rekey.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.lxzrvi.rekey.R

/** Actual font files, not synthetic bold/thin. No network font provider is needed. */
val Poppins = FontFamily(
    Font(R.font.poppins_thin, FontWeight.Thin),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun poppinsStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Poppins,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

val RekeyTypography = Typography(
    displayLarge = poppinsStyle(57, 64, FontWeight.Thin, -0.25),
    displayMedium = poppinsStyle(45, 52, FontWeight.Thin),
    displaySmall = poppinsStyle(36, 44, FontWeight.Normal),
    headlineLarge = poppinsStyle(32, 40, FontWeight.Medium),
    headlineMedium = poppinsStyle(28, 36, FontWeight.Medium),
    headlineSmall = poppinsStyle(24, 32, FontWeight.Medium),
    titleLarge = poppinsStyle(22, 28, FontWeight.Medium),
    titleMedium = poppinsStyle(16, 24, FontWeight.Medium, 0.15),
    titleSmall = poppinsStyle(14, 20, FontWeight.Medium, 0.1),
    bodyLarge = poppinsStyle(16, 26),
    bodyMedium = poppinsStyle(14, 22),
    bodySmall = poppinsStyle(12, 18),
    labelLarge = poppinsStyle(14, 20, FontWeight.Medium, 0.1),
    labelMedium = poppinsStyle(12, 16, FontWeight.Medium, 0.5),
    labelSmall = poppinsStyle(11, 16, FontWeight.Medium, 0.5),
)
