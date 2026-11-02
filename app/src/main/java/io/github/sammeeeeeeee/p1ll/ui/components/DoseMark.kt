package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.DoseUnit
import io.github.sammeeeeeeee.p1ll.data.PillShape
import io.github.sammeeeeeeee.p1ll.ui.Describe
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import kotlin.math.ceil

private val Each = 18.dp
private val Step = 14.dp

/**
 * One dose, drawn: a glyph per pill, drop, puff or patch, the last cut in half for a half. Liquids
 * get a part-filled cup (or the syringe) and the volume, weights a weight and the grams. More than
 * four, or an odd fraction, is one glyph and a count. The [strength] sits under it, right-aligned,
 * so everything about the dose stays on the right of the row.
 */
@Composable
fun DoseMark(amount: Double, unit: DoseUnit, shape: PillShape, color: Color, modifier: Modifier = Modifier, strength: String = "") {
    val c = P1ll.colors
    val described = listOf(Describe.amount(amount, unit, shape), strength.trim()).filter { it.isNotEmpty() }.joinToString(", ")
    Column(modifier.clearAndSetSemantics { contentDescription = described }, horizontalAlignment = Alignment.End) {
        Amount(amount, unit, shape, color)
        if (strength.isNotBlank()) {
            Text(
                strength.trim(),
                style = Type.small,
                color = c.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp).widthIn(max = 96.dp),
            )
        }
    }
}

@Composable
private fun Amount(amount: Double, unit: DoseUnit, shape: PillShape, color: Color) {
    val c = P1ll.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (unit != DoseUnit.COUNT) {
            Canvas(Modifier.size(Each)) {
                when {
                    unit == DoseUnit.G -> drawWeight(color)
                    shape == PillShape.SYRINGE -> drawPillGlyph(shape, color)
                    else -> drawCup(color, c.ink3)
                }
            }
            Spacer(Modifier.width(3.dp))
            Text(Describe.amount(amount, unit, shape), style = Type.label, color = c.ink2, maxLines = 1)
            return@Row
        }
        val halves = amount * 2
        val drawn = halves == Math.rint(halves) && amount in 0.5..4.0
        val count = if (drawn) ceil(amount).toInt() else 1
        val half = drawn && halves.toInt() % 2 == 1
        Canvas(Modifier.size(Each + Step * (count - 1), Each)) {
            val each = Each.toPx()
            val step = Step.toPx()
            repeat(count) { i ->
                val left = i * step
                val right = size.width - left - each
                val width = if (half && i == count - 1) each / 2 else each
                clipRect(left, 0f, left + width, each) {
                    inset(left, 0f, right, 0f) { drawUnit(shape, color) }
                }
            }
        }
        if (!drawn) {
            Spacer(Modifier.width(2.dp))
            Text("×${Fmt.amount(amount)}", style = Type.label, color = c.ink2, maxLines = 1)
        }
    }
}

/** Inhalers and sprays are counted in puffs, so draw puffs rather than devices. */
private fun DrawScope.drawUnit(shape: PillShape, color: Color) = when (shape) {
    PillShape.INHALER, PillShape.SPRAY -> drawPuff(color)
    else -> drawPillGlyph(shape, color)
}

/** A small cloud with a flat base. */
private fun DrawScope.drawPuff(color: Color) {
    val s = size.minDimension
    val c = center
    drawCircle(color, s * 0.2f, Offset(c.x - s * 0.2f, c.y + s * 0.1f))
    drawCircle(color, s * 0.26f, Offset(c.x + s * 0.02f, c.y - s * 0.06f))
    drawCircle(color, s * 0.18f, Offset(c.x + s * 0.24f, c.y + s * 0.12f))
    drawRect(color, Offset(c.x - s * 0.2f, c.y + s * 0.1f), Size(s * 0.44f, s * 0.2f))
}

/** A weight: a squat trapezoid with a loop on top. */
private fun DrawScope.drawWeight(color: Color) {
    val s = size.minDimension
    val cx = center.x
    val top = s * 0.36f
    val bottom = s * 0.9f
    drawCircle(color, s * 0.13f, Offset(cx, s * 0.24f), style = Stroke(s * 0.09f))
    val body = Path().apply {
        moveTo(cx - s * 0.26f, top)
        lineTo(cx + s * 0.26f, top)
        lineTo(cx + s * 0.4f, bottom)
        lineTo(cx - s * 0.4f, bottom)
        close()
    }
    drawPath(body, color)
    drawPath(body, color, style = Stroke(s * 0.08f, join = StrokeJoin.Round))
}

/** A dosing cup, outlined, with liquid most of the way up. */
private fun DrawScope.drawCup(color: Color, line: Color) {
    val s = size.minDimension
    val cx = center.x
    val top = s * 0.14f
    val bottom = s * 0.88f
    val cup = Path().apply {
        moveTo(cx - s * 0.36f, top)
        lineTo(cx + s * 0.36f, top)
        lineTo(cx + s * 0.25f, bottom)
        lineTo(cx - s * 0.25f, bottom)
        close()
    }
    clipPath(cup) { drawRect(color, Offset(0f, top + (bottom - top) * 0.38f), Size(size.width, bottom)) }
    drawPath(cup, line, style = Stroke(s * 0.08f, join = StrokeJoin.Round))
}
