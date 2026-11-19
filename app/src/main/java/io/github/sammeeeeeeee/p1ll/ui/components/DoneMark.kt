package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.sammeeeeeeee.p1ll.ui.theme.Motion
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll

/** An accent disc whose check draws itself in when it appears. */
@Composable
fun DoneMark(modifier: Modifier) {
    val c = P1ll.colors
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(520, delayMillis = 120, easing = Motion.decelerate)) }
    val path = remember { Path() }
    val seg = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(c.accent, r)
        path.reset()
        path.moveTo(center.x - r * 0.38f, center.y + r * 0.02f)
        path.lineTo(center.x - r * 0.1f, center.y + r * 0.3f)
        path.lineTo(center.x + r * 0.4f, center.y - r * 0.26f)
        measure.setPath(path, false)
        seg.reset()
        measure.getSegment(0f, measure.length * draw.value, seg, true)
        drawPath(seg, c.onAccent, style = Stroke(r * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
