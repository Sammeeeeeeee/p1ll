package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.Motion
import io.github.sammeeeeeeee.p1ll.ui.theme.onColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class TakeState { IDLE, DUE, DONE, LOG }

/**
 * The round button on every pill. Taking a dose floods it with colour, draws a check stroke by
 * stroke and throws a ring and eight sparks outwards.
 */
@Composable
fun TakeButton(
    state: TakeState,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    description: String = "Take",
) {
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    val wiggle = remember { Animatable(0f) }
    var celebrate by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf(false) }
    val burst = remember { Animatable(1f) }
    val done = state == TakeState.DONE || flash

    val fill by animateFloatAsState(if (done) 1f else 0f, spring(0.6f, 360f), label = "fill")
    val check by animateFloatAsState(
        if (done) 1f else 0f,
        if (done) tween(360, delayMillis = 70, easing = Motion.decelerate) else tween(140),
        label = "check",
    )
    val due = state == TakeState.DUE
    // Only a due button breathes, so idle lists don't redraw every frame.
    val breath: State<Float>? = if (due) {
        rememberInfiniteTransition(label = "halo").animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "breath",
        )
    } else {
        null
    }

    LaunchedEffect(celebrate) {
        if (celebrate == 0) return@LaunchedEffect
        burst.snapTo(0f)
        burst.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(flash) {
        if (flash) {
            delay(1200)
            flash = false
        }
    }

    val checkPath = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = wiggle.value }
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .tappable(pressedScale = 0.84f) {
                if (state == TakeState.DONE) {
                    // Already taken: a small "no" shake instead of silently undoing.
                    feedback.tick()
                    scope.launch {
                        wiggle.snapTo(0f)
                        wiggle.animateTo(0f, keyframes {
                            durationMillis = 420
                            -12f at 60
                            10f at 140
                            -6f at 220
                            3f at 300
                        })
                    }
                    return@tappable
                }
                celebrate++
                if (state == TakeState.LOG) flash = true
                onClick()
            },
    ) {
        val r = this.size.minDimension / 2
        val stroke = 2.dp.toPx()

        breath?.value?.let { b ->
            drawCircle(color.copy(alpha = 0.22f * (1f - b)), r * (1f + 0.5f * b))
        }
        drawCircle(color.copy(alpha = if (state == TakeState.IDLE) 0.08f else 0.14f), r)
        drawCircle(
            color.copy(alpha = if (state == TakeState.IDLE) 0.4f else 1f),
            r - stroke / 2,
            style = Stroke(stroke),
        )
        if (fill > 0f) drawCircle(color, r * fill)

        if (state == TakeState.LOG && check < 0.5f) {
            val a = r * 0.36f * (1f - check * 2)
            val w = 2.2.dp.toPx()
            drawLine(color, Offset(center.x - a, center.y), Offset(center.x + a, center.y), w, StrokeCap.Round)
            drawLine(color, Offset(center.x, center.y - a), Offset(center.x, center.y + a), w, StrokeCap.Round)
        }
        if (check > 0f) {
            checkPath.reset()
            checkPath.moveTo(center.x - r * 0.36f, center.y + r * 0.02f)
            checkPath.lineTo(center.x - r * 0.1f, center.y + r * 0.28f)
            checkPath.lineTo(center.x + r * 0.38f, center.y - r * 0.24f)
            measure.setPath(checkPath, false)
            segment.reset()
            measure.getSegment(0f, measure.length * check, segment, true)
            drawPath(
                segment,
                onColor(color),
                style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        if (burst.value < 1f) drawBurst(color, r, burst.value)
    }
}

private fun DrawScope.drawBurst(color: Color, r: Float, t: Float) {
    val fade = 1f - t
    drawCircle(
        color.copy(alpha = 0.55f * fade),
        r * (1f + 0.85f * t),
        style = Stroke((2.5.dp.toPx() * fade).coerceAtLeast(0.5f)),
    )
    repeat(8) { i ->
        val angle = (i * 45f + 22.5f) * PI.toFloat() / 180f
        val dist = r * (1.1f + 0.8f * t)
        val dot = 2.8.dp.toPx() * fade
        drawCircle(color.copy(alpha = fade), dot, Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist))
    }
}
