package io.github.lxzrvi.rekey.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.lxzrvi.rekey.ui.theme.BrandBlack
import io.github.lxzrvi.rekey.ui.theme.Paper

/** Static, inexpensive color field gives the pure blur real content to sample. */
@Composable
fun AmbientBackground(accent: Color, darkTheme: Boolean, modifier: Modifier = Modifier) {
    val base = if (darkTheme) BrandBlack else Paper
    Canvas(modifier.fillMaxSize()) {
        drawRect(base)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = if (darkTheme) 0.42f else 0.30f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.24f),
                radius = size.minDimension * 0.72f,
            ),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.12f, size.height * 0.24f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8E6BFF).copy(alpha = if (darkTheme) 0.30f else 0.18f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.62f),
                radius = size.minDimension * 0.80f,
            ),
            radius = size.minDimension * 0.80f,
            center = Offset(size.width * 0.92f, size.height * 0.62f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF54D6B4).copy(alpha = if (darkTheme) 0.20f else 0.16f), Color.Transparent),
                center = Offset(size.width * 0.35f, size.height * 0.92f),
                radius = size.minDimension * 0.65f,
            ),
            radius = size.minDimension * 0.65f,
            center = Offset(size.width * 0.35f, size.height * 0.92f),
        )
    }
}
