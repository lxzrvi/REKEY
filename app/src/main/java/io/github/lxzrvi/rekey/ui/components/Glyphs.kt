package io.github.lxzrvi.rekey.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Every icon in REKEY is drawn here with Canvas on a 24x24 grid. No Material
 * icons dependency is pulled in, so the APK stays small and the stroke weight
 * is identical across the keyboard, the cards and the setup flow.
 */
enum class Glyph {
    CROSS, PLUS, BACK, CHECK, CHEVRON_DOWN,
    PASTE, TRANSLATE, PALETTE, MORE,
    SHIFT, BACKSPACE, GLOBE, ENTER, KEYBOARD_TEXT,
    CHEVRON_LEFT, SMILEY, CAPS_ON, CAPS_LOCK,
    CHEVRON_RIGHT, CHEVRON_UP, COPY, CUT, SELECT_ALL,
    HOME, END, BOLD, ITALIC, UNDERLINE,
    MIC, CLIPBOARD, PIN, CLOCK, MERGE, CONTRAST,
}

@Composable
fun RekeyGlyph(
    glyph: Glyph,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.9f,
) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val sw = strokeWidth * s
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        when (glyph) {
            Glyph.CHEVRON_LEFT -> {
                val a = Path().apply {
                    moveTo(14.5f * s, 5.5f * s)
                    lineTo(8.5f * s, 12f * s)
                    lineTo(14.5f * s, 18.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.SMILEY -> {
                drawCircle(color, 8.6f * s, p(12f, 12f), style = stroke)
                drawCircle(color, 1.15f * s, p(9f, 10f))
                drawCircle(color, 1.15f * s, p(15f, 10f))
                val a = Path().apply {
                    moveTo(8.2f * s, 14.2f * s)
                    quadraticTo(12f * s, 17.6f * s, 15.8f * s, 14.2f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.CAPS_ON -> {
                val a = Path().apply {
                    moveTo(12f * s, 3.5f * s)
                    lineTo(20f * s, 11.5f * s)
                    lineTo(15.5f * s, 11.5f * s)
                    lineTo(15.5f * s, 18f * s)
                    lineTo(8.5f * s, 18f * s)
                    lineTo(8.5f * s, 11.5f * s)
                    lineTo(4f * s, 11.5f * s)
                    close()
                }
                drawPath(a, color)
            }
            Glyph.CAPS_LOCK -> {
                val a = Path().apply {
                    moveTo(12f * s, 2.5f * s)
                    lineTo(20f * s, 10.5f * s)
                    lineTo(15.5f * s, 10.5f * s)
                    lineTo(15.5f * s, 15.5f * s)
                    lineTo(8.5f * s, 15.5f * s)
                    lineTo(8.5f * s, 10.5f * s)
                    lineTo(4f * s, 10.5f * s)
                    close()
                }
                drawPath(a, color)
                drawLine(color, p(8.5f, 19f), p(15.5f, 19f), sw * 1.2f, StrokeCap.Round)
            }
            Glyph.KEYBOARD_TEXT -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(2.5f, 5.5f),
                    size = Size(19f * s, 13f * s),
                    cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
                    style = stroke,
                )
                listOf(7f, 11f, 15f).forEach { x ->
                    drawLine(color, p(x, 9.5f), p(x + 1.6f, 9.5f), sw, StrokeCap.Round)
                }
                drawLine(color, p(8f, 14.5f), p(16f, 14.5f), sw, StrokeCap.Round)
            }
            Glyph.CHEVRON_RIGHT -> {
                val a = Path().apply {
                    moveTo(9.5f * s, 5.5f * s)
                    lineTo(15.5f * s, 12f * s)
                    lineTo(9.5f * s, 18.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.CHEVRON_UP -> {
                val a = Path().apply {
                    moveTo(5.5f * s, 14.5f * s)
                    lineTo(12f * s, 8.5f * s)
                    lineTo(18.5f * s, 14.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.COPY -> {
                drawRoundRect(color, p(8.5f, 3.5f), Size(12f * s, 14f * s), CornerRadius(2.2f * s, 2.2f * s), style = stroke)
                val a = Path().apply {
                    moveTo(15.5f * s, 20.5f * s)
                    lineTo(5.5f * s, 20.5f * s)
                    lineTo(5.5f * s, 7f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.CUT -> {
                drawCircle(color, 3f * s, p(6.5f, 18f), style = stroke)
                drawCircle(color, 3f * s, p(17.5f, 18f), style = stroke)
                drawLine(color, p(8.6f, 15.8f), p(18f, 3.5f), sw, StrokeCap.Round)
                drawLine(color, p(15.4f, 15.8f), p(6f, 3.5f), sw, StrokeCap.Round)
            }
            Glyph.SELECT_ALL -> {
                val a = Path().apply {
                    moveTo(4f * s, 8f * s); lineTo(4f * s, 4f * s); lineTo(8f * s, 4f * s)
                    moveTo(16f * s, 4f * s); lineTo(20f * s, 4f * s); lineTo(20f * s, 8f * s)
                    moveTo(20f * s, 16f * s); lineTo(20f * s, 20f * s); lineTo(16f * s, 20f * s)
                    moveTo(8f * s, 20f * s); lineTo(4f * s, 20f * s); lineTo(4f * s, 16f * s)
                }
                drawPath(a, color, style = stroke)
                drawRect(color, p(8.5f, 8.5f), Size(7f * s, 7f * s))
            }
            Glyph.HOME -> {
                val a = Path().apply {
                    moveTo(4f * s, 11.5f * s); lineTo(12f * s, 4.5f * s); lineTo(20f * s, 11.5f * s)
                    moveTo(6.5f * s, 10.5f * s); lineTo(6.5f * s, 19.5f * s)
                    lineTo(17.5f * s, 19.5f * s); lineTo(17.5f * s, 10.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.END -> {
                drawLine(color, p(19f, 4.5f), p(19f, 19.5f), sw, StrokeCap.Round)
                val a = Path().apply {
                    moveTo(4f * s, 12f * s); lineTo(15f * s, 12f * s)
                    moveTo(10.5f * s, 7.5f * s); lineTo(15f * s, 12f * s); lineTo(10.5f * s, 16.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.BOLD -> {
                val a = Path().apply {
                    moveTo(7f * s, 4.5f * s); lineTo(7f * s, 19.5f * s)
                    lineTo(13.5f * s, 19.5f * s)
                    quadraticTo(18.5f * s, 19.5f * s, 18.5f * s, 15.5f * s)
                    quadraticTo(18.5f * s, 12f * s, 13.5f * s, 12f * s)
                    lineTo(7f * s, 12f * s)
                    moveTo(13.5f * s, 12f * s)
                    quadraticTo(17.5f * s, 12f * s, 17.5f * s, 8.2f * s)
                    quadraticTo(17.5f * s, 4.5f * s, 13.5f * s, 4.5f * s)
                    lineTo(7f * s, 4.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.ITALIC -> {
                drawLine(color, p(10f, 4.5f), p(19f, 4.5f), sw, StrokeCap.Round)
                drawLine(color, p(5f, 19.5f), p(14f, 19.5f), sw, StrokeCap.Round)
                drawLine(color, p(14.5f, 4.5f), p(9.5f, 19.5f), sw, StrokeCap.Round)
            }
            Glyph.UNDERLINE -> {
                val a = Path().apply {
                    moveTo(6.5f * s, 4f * s); lineTo(6.5f * s, 11.5f * s)
                    quadraticTo(6.5f * s, 16.5f * s, 12f * s, 16.5f * s)
                    quadraticTo(17.5f * s, 16.5f * s, 17.5f * s, 11.5f * s)
                    lineTo(17.5f * s, 4f * s)
                }
                drawPath(a, color, style = stroke)
                drawLine(color, p(5f, 20.5f), p(19f, 20.5f), sw, StrokeCap.Round)
            }
            Glyph.CROSS -> {
                drawLine(color, p(6.5f, 6.5f), p(17.5f, 17.5f), sw, StrokeCap.Round)
                drawLine(color, p(17.5f, 6.5f), p(6.5f, 17.5f), sw, StrokeCap.Round)
            }
            Glyph.PLUS -> {
                drawLine(color, p(12f, 4.5f), p(12f, 19.5f), sw, StrokeCap.Round)
                drawLine(color, p(4.5f, 12f), p(19.5f, 12f), sw, StrokeCap.Round)
            }
            Glyph.BACK -> {
                drawLine(color, p(19f, 12f), p(5.5f, 12f), sw, StrokeCap.Round)
                val a = Path().apply {
                    moveTo(11.5f * s, 5.5f * s)
                    lineTo(5f * s, 12f * s)
                    lineTo(11.5f * s, 18.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.CHECK -> {
                val a = Path().apply {
                    moveTo(5f * s, 12.5f * s)
                    lineTo(10f * s, 17.5f * s)
                    lineTo(19f * s, 6.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.CHEVRON_DOWN -> {
                val a = Path().apply {
                    moveTo(6f * s, 9.5f * s)
                    lineTo(12f * s, 15.5f * s)
                    lineTo(18f * s, 9.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.PASTE -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(6f, 4.5f),
                    size = Size(12f * s, 16f * s),
                    cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = p(9f, 2.5f),
                    size = Size(6f * s, 4f * s),
                    cornerRadius = CornerRadius(1.4f * s, 1.4f * s),
                )
                drawLine(color, p(9f, 11f), p(15f, 11f), sw * 0.85f, StrokeCap.Round)
                drawLine(color, p(9f, 15f), p(13.5f, 15f), sw * 0.85f, StrokeCap.Round)
            }
            Glyph.TRANSLATE -> {
                drawLine(color, p(3.5f, 6f), p(12.5f, 6f), sw, StrokeCap.Round)
                drawLine(color, p(8f, 3.5f), p(8f, 6f), sw, StrokeCap.Round)
                val a = Path().apply {
                    moveTo(11f * s, 6f * s)
                    quadraticTo(9.5f * s, 13f * s, 4f * s, 15.5f * s)
                    moveTo(5.5f * s, 8.5f * s)
                    quadraticTo(7.5f * s, 13.5f * s, 12f * s, 15.5f * s)
                }
                drawPath(a, color, style = stroke)
                val b = Path().apply {
                    moveTo(12.5f * s, 20.5f * s)
                    lineTo(16.5f * s, 10.5f * s)
                    lineTo(20.5f * s, 20.5f * s)
                    moveTo(14f * s, 17.5f * s)
                    lineTo(19f * s, 17.5f * s)
                }
                drawPath(b, color, style = stroke)
            }
            Glyph.PALETTE -> {
                val outline = Path().apply {
                    moveTo(12f * s, 3.2f * s)
                    cubicTo(17.4f * s, 3.2f * s, 21f * s, 7f, 21f * s, 11.6f)
                    cubicTo(21f * s, 14.4f * s, 18.9f * s, 15.6f * s, 16.8f * s, 15.6f)
                    lineTo(15.2f * s, 15.6f * s)
                    cubicTo(13.9f * s, 15.6f * s, 13.1f * s, 16.5f * s, 13.1f * s, 17.6f * s)
                    cubicTo(13.1f * s, 18.4f * s, 13.5f * s, 18.9f * s, 13.5f * s, 19.6f * s)
                    cubicTo(13.5f * s, 20.4f * s, 12.8f * s, 20.8f * s, 12f * s, 20.8f * s)
                    cubicTo(7f * s, 20.8f * s, 3f * s, 16.9f * s, 3f * s, 12f * s)
                    cubicTo(3f * s, 7.1f * s, 7f * s, 3.2f * s, 12f * s, 3.2f * s)
                    close()
                }
                drawPath(outline, color, style = stroke)
                drawCircle(color, 1.35f * s, p(8f, 8.5f))
                drawCircle(color, 1.35f * s, p(13f, 7f))
                drawCircle(color, 1.35f * s, p(17f, 10f))
                drawCircle(color, 1.35f * s, p(7.2f, 14f))
            }
            Glyph.MORE -> {
                drawCircle(color, 1.6f * s, p(5.5f, 12f))
                drawCircle(color, 1.6f * s, p(12f, 12f))
                drawCircle(color, 1.6f * s, p(18.5f, 12f))
            }
            Glyph.SHIFT -> {
                val a = Path().apply {
                    moveTo(12f * s, 4f * s)
                    lineTo(20f * s, 12f * s)
                    lineTo(15.5f * s, 12f * s)
                    lineTo(15.5f * s, 19.5f * s)
                    lineTo(8.5f * s, 19.5f * s)
                    lineTo(8.5f * s, 12f * s)
                    lineTo(4f * s, 12f * s)
                    close()
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.BACKSPACE -> {
                val a = Path().apply {
                    moveTo(9f * s, 5f * s)
                    lineTo(21f * s, 5f * s)
                    lineTo(21f * s, 19f * s)
                    lineTo(9f * s, 19f * s)
                    lineTo(2.5f * s, 12f * s)
                    close()
                }
                drawPath(a, color, style = stroke)
                drawLine(color, p(11.5f, 9f), p(17.5f, 15f), sw * 0.9f, StrokeCap.Round)
                drawLine(color, p(17.5f, 9f), p(11.5f, 15f), sw * 0.9f, StrokeCap.Round)
            }
            Glyph.GLOBE -> {
                drawCircle(color, 8.6f * s, p(12f, 12f), style = stroke)
                drawLine(color, p(3.4f, 12f), p(20.6f, 12f), sw * 0.85f, StrokeCap.Round)
                val a = Path().apply {
                    moveTo(12f * s, 3.4f * s)
                    cubicTo(7.6f * s, 7f * s, 7.6f * s, 17f * s, 12f * s, 20.6f * s)
                    cubicTo(16.4f * s, 17f * s, 16.4f * s, 7f * s, 12f * s, 3.4f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.ENTER -> {
                drawLine(color, p(20f, 6f), p(20f, 13f), sw, StrokeCap.Round)
                drawLine(color, p(20f, 13f), p(6f, 13f), sw, StrokeCap.Round)
                val a = Path().apply {
                    moveTo(11f * s, 7.5f * s)
                    lineTo(5.2f * s, 13f * s)
                    lineTo(11f * s, 18.5f * s)
                }
                drawPath(a, color, style = stroke)
            }
            Glyph.MIC -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(9f, 2.5f),
                    size = Size(6f * s, 11f * s),
                    cornerRadius = CornerRadius(3f * s, 3f * s),
                    style = stroke,
                )
                val a = Path().apply {
                    moveTo(5.5f * s, 11.5f * s)
                    quadraticTo(5.5f * s, 19f, 12f * s, 19f * s)
                    quadraticTo(18.5f * s, 19f, 18.5f * s, 11.5f * s)
                }
                drawPath(a, color, style = stroke)
                drawLine(color, p(12f, 19f), p(12f, 22f), sw, StrokeCap.Round)
            }
            Glyph.CLIPBOARD -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(4.5f, 4.5f),
                    size = Size(15f * s, 16.5f * s),
                    cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = p(8.5f, 2.5f),
                    size = Size(7f * s, 4.5f * s),
                    cornerRadius = CornerRadius(1.6f * s, 1.6f * s),
                    style = stroke,
                )
                drawLine(color, p(8.5f, 12f), p(15.5f, 12f), sw * 0.85f, StrokeCap.Round)
                drawLine(color, p(8.5f, 16f), p(13.5f, 16f), sw * 0.85f, StrokeCap.Round)
            }
            Glyph.PIN -> {
                val a = Path().apply {
                    moveTo(9f * s, 3.5f * s)
                    lineTo(15f * s, 3.5f * s)
                    lineTo(14.2f * s, 10.5f * s)
                    quadraticTo(18f * s, 12.5f, 18f * s, 15f * s)
                    lineTo(6f * s, 15f * s)
                    quadraticTo(6f * s, 12.5f, 9.8f * s, 10.5f * s)
                    close()
                }
                drawPath(a, color, style = stroke)
                drawLine(color, p(12f, 15f), p(12f, 21.5f), sw, StrokeCap.Round)
            }
            Glyph.CLOCK -> {
                drawCircle(color, 8.6f * s, p(12f, 12f), style = stroke)
                drawLine(color, p(12f, 7f), p(12f, 12f), sw, StrokeCap.Round)
                drawLine(color, p(12f, 12f), p(16f, 14.5f), sw, StrokeCap.Round)
            }
            Glyph.MERGE -> {
                val a = Path().apply {
                    moveTo(4f * s, 5f * s)
                    lineTo(10f * s, 12f * s)
                    lineTo(4f * s, 19f * s)
                }
                drawPath(a, color, style = stroke)
                val b = Path().apply {
                    moveTo(20f * s, 5f * s)
                    lineTo(14f * s, 12f * s)
                    lineTo(20f * s, 19f * s)
                }
                drawPath(b, color, style = stroke)
                drawLine(color, p(12f, 12f), p(12f, 12f), sw * 3.2f, StrokeCap.Round)
            }
            Glyph.CONTRAST -> {
                drawCircle(color, 8.6f * s, p(12f, 12f), style = stroke)
                val a = Path().apply {
                    moveTo(12f * s, 3.4f * s)
                    arcTo(RectF(3.4f * s, 3.4f * s, 20.6f * s, 20.6f * s), 270f, 180f, false)
                    close()
                }
                drawPath(a, color)
            }
        }
    }
}
