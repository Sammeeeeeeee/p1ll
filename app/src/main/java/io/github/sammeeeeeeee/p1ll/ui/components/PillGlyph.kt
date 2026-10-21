package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import io.github.sammeeeeeeee.p1ll.data.PillShape
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll

/** The pill itself, drawn flat with one score line and one glint so it reads at any size. */
fun DrawScope.drawPillGlyph(shape: PillShape, color: Color) {
    val s = size.minDimension
    val c = center
    val shade = lerp(color, Color.Black, 0.22f).copy(alpha = 0.55f)
    val glint = Color.White.copy(alpha = 0.38f)
    when (shape) {
        PillShape.ROUND -> {
            val r = s * 0.42f
            drawCircle(color, r, c)
            drawLine(shade, Offset(c.x - r * 0.6f, c.y), Offset(c.x + r * 0.6f, c.y), s * 0.05f, StrokeCap.Round)
            val g = r * 0.68f
            drawArc(
                glint, 200f, 55f, false,
                topLeft = Offset(c.x - g, c.y - g), size = Size(g * 2, g * 2),
                style = Stroke(s * 0.055f, cap = StrokeCap.Round),
            )
        }
        PillShape.CAPSULE -> rotate(-40f, c) {
            val w = s * 0.96f
            val h = s * 0.42f
            val tl = Offset(c.x - w / 2, c.y - h / 2)
            drawRoundRect(lerp(color, Color.White, 0.58f), tl, Size(w, h), CornerRadius(h / 2))
            clipRect(tl.x, tl.y, c.x, tl.y + h) {
                drawRoundRect(color, tl, Size(w, h), CornerRadius(h / 2))
            }
            drawLine(glint, Offset(tl.x + h * 0.5f, tl.y + h * 0.27f), Offset(c.x - h * 0.25f, tl.y + h * 0.27f), s * 0.05f, StrokeCap.Round)
        }
        PillShape.OVAL -> rotate(-28f, c) {
            val w = s * 0.9f
            val h = s * 0.56f
            drawOval(color, Offset(c.x - w / 2, c.y - h / 2), Size(w, h))
            drawLine(shade, Offset(c.x - w * 0.28f, c.y), Offset(c.x + w * 0.28f, c.y), s * 0.045f, StrokeCap.Round)
            drawArc(
                glint, 195f, 50f, false,
                topLeft = Offset(c.x - w * 0.36f, c.y - h * 0.34f), size = Size(w * 0.72f, h * 0.68f),
                style = Stroke(s * 0.05f, cap = StrokeCap.Round),
            )
        }
        PillShape.TABLET -> {
            val w = s * 0.76f
            drawRoundRect(color, Offset(c.x - w / 2, c.y - w / 2), Size(w, w), CornerRadius(w * 0.32f))
            val i = w * 0.66f
            drawRoundRect(
                glint, Offset(c.x - i / 2, c.y - i / 2), Size(i, i), CornerRadius(i * 0.28f),
                style = Stroke(s * 0.045f),
            )
        }
        PillShape.DROP -> {
            val r = s * 0.3f
            val cy = c.y + s * 0.12f
            val top = c.y - s * 0.44f
            val path = Path().apply {
                moveTo(c.x, top)
                cubicTo(c.x + r * 0.25f, top + r * 0.55f, c.x + r, cy - r * 0.65f, c.x + r, cy)
                arcTo(Rect(c.x - r, cy - r, c.x + r, cy + r), 0f, 180f, false)
                cubicTo(c.x - r, cy - r * 0.65f, c.x - r * 0.25f, top + r * 0.55f, c.x, top)
                close()
            }
            drawPath(path, color)
            val g = r * 0.62f
            drawArc(
                glint, 120f, 60f, false,
                topLeft = Offset(c.x - g, cy - g), size = Size(g * 2, g * 2),
                style = Stroke(s * 0.05f, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Glyph on a pale tile shaped like one of the chart's squares. The tint is mixed from white
 * (or the dark surface), never from the background, so cool colours stay clean on cream.
 */
fun DrawScope.drawGlyphBadge(shape: PillShape, color: Color, dark: Boolean = false) {
    val tile = if (dark) lerp(Color(0xFF221C2E), color, 0.24f) else lerp(Color.White, color, 0.17f)
    drawRoundRect(tile, cornerRadius = CornerRadius(size.minDimension * 0.32f))
    inset(size.minDimension * 0.2f) { drawPillGlyph(shape, color) }
}

@Composable
fun PillGlyph(shape: PillShape, color: Color, modifier: Modifier = Modifier, badge: Boolean = true) {
    val dark = P1ll.colors.dark
    Canvas(modifier) { if (badge) drawGlyphBadge(shape, color, dark) else drawPillGlyph(shape, color) }
}
