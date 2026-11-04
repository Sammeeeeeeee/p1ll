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
    val pale = lerp(color, Color.White, 0.58f)
    val deep = lerp(color, Color.Black, 0.25f)
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
        // Powder: a packet with a crimped top and a seal line under it.
        PillShape.SACHET -> {
            val w = s * 0.66f
            val h = s * 0.84f
            val l = c.x - w / 2
            val t = c.y - h / 2
            val tooth = s * 0.06f
            drawRoundRect(color, Offset(l, t + tooth), Size(w, h - tooth), CornerRadius(s * 0.07f))
            val teeth = 10
            val crimp = Path().apply {
                moveTo(l, t + tooth * 2)
                for (i in 0..teeth) lineTo(l + w * i / teeth, if (i % 2 == 1) t else t + tooth)
                lineTo(l + w, t + tooth * 2)
                close()
            }
            drawPath(crimp, color)
            drawLine(shade, Offset(l + w * 0.14f, t + h * 0.25f), Offset(l + w * 0.86f, t + h * 0.25f), s * 0.045f, StrokeCap.Round)
            drawLine(glint, Offset(l + w * 0.2f, t + h * 0.42f), Offset(l + w * 0.2f, t + h * 0.8f), s * 0.05f, StrokeCap.Round)
        }
        // Cream: a squeeze tube, crimped at one end, tapering to its cap.
        PillShape.TUBE -> rotate(-40f, c) {
            val l = c.x - s * 0.46f
            val crimp = s * 0.07f
            val end = c.x + s * 0.2f
            val wide = s * 0.21f
            val narrow = s * 0.15f
            drawRoundRect(deep, Offset(l, c.y - wide), Size(crimp, wide * 2), CornerRadius(s * 0.02f))
            val body = Path().apply {
                moveTo(l + crimp, c.y - wide)
                lineTo(end, c.y - narrow)
                lineTo(end + s * 0.08f, c.y - s * 0.09f)
                lineTo(end + s * 0.08f, c.y + s * 0.09f)
                lineTo(end, c.y + narrow)
                lineTo(l + crimp, c.y + wide)
                close()
            }
            drawPath(body, color)
            drawRoundRect(pale, Offset(end + s * 0.08f, c.y - s * 0.1f), Size(s * 0.18f, s * 0.2f), CornerRadius(s * 0.04f))
            drawLine(glint, Offset(l + crimp + s * 0.08f, c.y - wide * 0.5f), Offset(end - s * 0.06f, c.y - narrow * 0.5f), s * 0.05f, StrokeCap.Round)
        }
        // Liquid: a bottle with a screw cap and a pale label.
        PillShape.BOTTLE -> {
            val w = s * 0.56f
            val top = c.y - s * 0.22f
            drawRoundRect(pale, Offset(c.x - s * 0.17f, c.y - s * 0.44f), Size(s * 0.34f, s * 0.24f), CornerRadius(s * 0.05f))
            drawRoundRect(color, Offset(c.x - w / 2, top), Size(w, c.y + s * 0.44f - top), CornerRadius(s * 0.12f))
            drawRoundRect(glint, Offset(c.x - w * 0.32f, c.y + s * 0.02f), Size(w * 0.64f, s * 0.2f), CornerRadius(s * 0.03f))
        }
        // Nasal spray: a bottle, collar and nozzle, with a puff of mist.
        PillShape.SPRAY -> {
            val x = c.x - s * 0.1f
            val w = s * 0.4f
            drawRoundRect(pale, Offset(x - s * 0.06f, c.y - s * 0.44f), Size(s * 0.12f, s * 0.34f), CornerRadius(s * 0.05f))
            drawRoundRect(pale, Offset(x - w * 0.36f, c.y - s * 0.15f), Size(w * 0.72f, s * 0.1f), CornerRadius(s * 0.02f))
            drawRoundRect(color, Offset(x - w / 2, c.y - s * 0.06f), Size(w, s * 0.5f), CornerRadius(s * 0.1f))
            drawLine(glint, Offset(x - w * 0.25f, c.y + s * 0.06f), Offset(x - w * 0.25f, c.y + s * 0.3f), s * 0.05f, StrokeCap.Round)
            drawCircle(color.copy(alpha = 0.7f), s * 0.05f, Offset(x + s * 0.17f, c.y - s * 0.36f))
            drawCircle(color.copy(alpha = 0.5f), s * 0.04f, Offset(x + s * 0.29f, c.y - s * 0.44f))
            drawCircle(color.copy(alpha = 0.5f), s * 0.04f, Offset(x + s * 0.3f, c.y - s * 0.28f))
        }
        // Inhaler: a canister in an L-shaped holder with a mouthpiece.
        PillShape.INHALER -> {
            val x = c.x + s * 0.03f
            drawRoundRect(pale, Offset(x + s * 0.01f, c.y - s * 0.46f), Size(s * 0.24f, s * 0.32f), CornerRadius(s * 0.08f))
            drawRoundRect(color, Offset(x - s * 0.04f, c.y - s * 0.22f), Size(s * 0.34f, s * 0.52f), CornerRadius(s * 0.1f))
            drawRoundRect(color, Offset(x - s * 0.36f, c.y + s * 0.08f), Size(s * 0.42f, s * 0.28f), CornerRadius(s * 0.07f))
            drawLine(shade, Offset(x - s * 0.29f, c.y + s * 0.15f), Offset(x - s * 0.29f, c.y + s * 0.29f), s * 0.045f, StrokeCap.Round)
            drawLine(glint, Offset(x + s * 0.04f, c.y - s * 0.13f), Offset(x + s * 0.04f, c.y + s * 0.01f), s * 0.05f, StrokeCap.Round)
        }
        // Injection: a syringe, plunger to needle.
        PillShape.SYRINGE -> rotate(-45f, c) {
            val l = c.x - s * 0.2f
            val r = c.x + s * 0.22f
            val h = s * 0.13f
            drawLine(pale, Offset(l - s * 0.18f, c.y), Offset(l, c.y), s * 0.07f, StrokeCap.Round)
            drawLine(pale, Offset(l - s * 0.2f, c.y - h), Offset(l - s * 0.2f, c.y + h), s * 0.07f, StrokeCap.Round)
            drawLine(deep, Offset(r, c.y), Offset(r + s * 0.25f, c.y), s * 0.03f, StrokeCap.Round)
            drawRoundRect(color, Offset(r - s * 0.01f, c.y - h * 0.45f), Size(s * 0.07f, h * 0.9f), CornerRadius(s * 0.015f))
            drawRoundRect(color, Offset(l, c.y - h), Size(r - l, h * 2), CornerRadius(s * 0.04f))
            drawLine(color, Offset(l, c.y - h * 1.7f), Offset(l, c.y + h * 1.7f), s * 0.06f, StrokeCap.Round)
            for (i in 1..3) {
                val mx = l + (r - l) * i / 4f
                drawLine(shade, Offset(mx, c.y - h), Offset(mx, c.y - h * 0.3f), s * 0.035f, StrokeCap.Round)
            }
        }
        // Skin patch: a soft square with one corner peeling back.
        PillShape.PATCH -> rotate(-10f, c) {
            val w = s * 0.74f
            val l = c.x - w / 2
            val t = c.y - w / 2
            val fold = w * 0.34f
            val r = w * 0.18f
            val body = Path().apply {
                moveTo(l + r, t)
                lineTo(l + w - r, t)
                arcTo(Rect(l + w - 2 * r, t, l + w, t + 2 * r), -90f, 90f, false)
                lineTo(l + w, t + w - fold)
                lineTo(l + w - fold, t + w)
                lineTo(l + r, t + w)
                arcTo(Rect(l, t + w - 2 * r, l + 2 * r, t + w), 90f, 90f, false)
                lineTo(l, t + r)
                arcTo(Rect(l, t, l + 2 * r, t + 2 * r), 180f, 90f, false)
                close()
            }
            drawPath(body, color)
            val flap = Path().apply {
                moveTo(l + w, t + w - fold)
                lineTo(l + w - fold, t + w)
                lineTo(l + w - fold, t + w - fold)
                close()
            }
            drawPath(flap, pale)
            drawLine(glint, Offset(l + w * 0.2f, t + w * 0.2f), Offset(l + w * 0.5f, t + w * 0.2f), s * 0.05f, StrokeCap.Round)
        }
    }
}

/**
 * Glyph on a pale tile shaped like one of the chart's squares. The tint is mixed from white
 * (or the dark surface), never from the background, so cool colours stay clean on cream.
 */
fun DrawScope.drawGlyphBadge(shape: PillShape, color: Color, dark: Boolean = false) {
    val tile = if (dark) lerp(Color(0xFF1C1D23), color, 0.24f) else lerp(Color.White, color, 0.17f)
    drawRoundRect(tile, cornerRadius = CornerRadius(size.minDimension * 0.32f))
    inset(size.minDimension * 0.2f) { drawPillGlyph(shape, color) }
}

@Composable
fun PillGlyph(shape: PillShape, color: Color, modifier: Modifier = Modifier, badge: Boolean = true) {
    val dark = P1ll.colors.dark
    Canvas(modifier) { if (badge) drawGlyphBadge(shape, color, dark) else drawPillGlyph(shape, color) }
}
