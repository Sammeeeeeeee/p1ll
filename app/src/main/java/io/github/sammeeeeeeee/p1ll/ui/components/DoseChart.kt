package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawWithContent
import io.github.sammeeeeeeee.p1ll.domain.DayCell
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/** A ripple that runs through the chart from today's square. */
@Stable
class ChartWave {
    internal val progress = Animatable(1f)
    internal var strength by mutableFloatStateOf(1f)

    suspend fun play(strong: Boolean = false) {
        strength = if (strong) 1.7f else 1f
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (strong) 1600 else 1150, easing = LinearEasing))
    }
}

@Composable
fun rememberChartWave() = remember { ChartWave() }

/**
 * The history grid. Each square is one day, cut into a slice per required dose, clockwise from
 * twelve o'clock in time order. A day with every dose taken closes into a solid square.
 */
@Composable
fun DoseChart(
    cells: List<DayCell>,
    accent: Color,
    modifier: Modifier = Modifier,
    background: Color = P1ll.colors.surface,
    wave: ChartWave? = null,
    cellSize: Dp = 15.dp,
    gap: Dp = 4.dp,
    onDayClick: ((Long) -> Unit)? = null,
) {
    if (cells.isEmpty()) return
    val c = P1ll.colors
    val today = cells.last().day
    val firstDow = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    fun rowOf(day: Long) = (LocalDate.ofEpochDay(day).dayOfWeek.value - firstDow.value + 7) % 7
    val gridStart = cells.first().day.let { it - rowOf(it) }
    val columns = ((today - gridStart) / 7).toInt() + 1
    val todayCol = columns - 1
    val todayRow = rowOf(today)
    val labelHeight = 20.dp
    val step = cellSize + gap
    val width = step * columns - gap
    val height = labelHeight + step * 7 - gap

    val measurer = rememberTextMeasurer()
    val labelStyle = Type.small.copy(color = c.ink3)
    val months = remember(gridStart, columns, labelStyle) {
        var lastCol = -10
        (0 until columns).mapNotNull { col ->
            val first = (0..6).map { gridStart + col * 7 + it }.firstOrNull { LocalDate.ofEpochDay(it).dayOfMonth == 1 }
                ?: if (col == 0) gridStart else null
            if (first == null || col - lastCol < 3) return@mapNotNull null
            lastCol = col
            val name = LocalDate.ofEpochDay(first).month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            col to measurer.measure(name, labelStyle)
        }
    }

    // Entry: squares bloom outwards from today.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    // Today's slices sweep in as doses are taken.
    val todaySlots = cells.last().slots
    val slices = remember(todaySlots.size) {
        List(todaySlots.size) { Animatable(if (todaySlots[it] == SlotStatus.TAKEN) 1f else 0f) }
    }
    LaunchedEffect(todaySlots) {
        coroutineScope {
            todaySlots.forEachIndexed { i, s ->
                launch { slices[i].animateTo(if (s == SlotStatus.TAKEN) 1f else 0f, spring(0.82f, 110f)) }
            }
        }
    }


    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    var popped by remember { mutableLongStateOf(Long.MIN_VALUE) }

    val path = remember { Path() }
    val scroll = rememberScrollState()
    val weekdayNames = remember(firstDow) {
        (0..6).map { firstDow.plus(it.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    Row(modifier) {
        Column(Modifier.padding(end = 8.dp)) {
            Spacer(Modifier.height(labelHeight))
            weekdayNames.forEachIndexed { row, name ->
                Box(Modifier.height(if (row < 6) step else cellSize)) {
                    if (row % 2 == 0) Text(name, style = Type.small, color = c.ink3, modifier = Modifier.padding(top = 0.dp))
                }
            }
        }
        Box(
            Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Fade the left edge only while there's older history scrolled off it.
                    val fade = 28.dp.toPx()
                    val hidden = (scroll.maxValue - scroll.value).toFloat()
                    if (hidden <= 0f) return@drawWithContent
                    val strength = (hidden / fade).coerceIn(0f, 1f)
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Black.copy(alpha = 1f - strength), 1f to Color.Black, endX = fade),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .horizontalScroll(scroll, reverseScrolling = true),
        ) {
            Box(Modifier.padding(end = 5.dp)) {
                Canvas(
                    Modifier
                        .size(width, height)
                        .pointerInput(gridStart, columns, onDayClick) {
                            if (onDayClick == null) return@pointerInput
                            detectTapGestures { pos ->
                                val stepPx = step.toPx()
                                val col = (pos.x / stepPx).toInt()
                                val row = ((pos.y - labelHeight.toPx()) / stepPx).toInt()
                                if (row !in 0..6 || col !in 0 until columns) return@detectTapGestures
                                val day = gridStart + col * 7 + row
                                if (day < cells.first().day || day > today) return@detectTapGestures
                                popped = day
                                scope.launch {
                                    pop.snapTo(0.7f)
                                    pop.animateTo(1f, spring(0.38f, 600f))
                                }
                                onDayClick(day)
                            }
                        },
                ) {
                    val cellPx = cellSize.toPx()
                    val stepPx = step.toPx()
                    val top = labelHeight.toPx()
                    val radius = CornerRadius(cellPx * 0.3f)
                    val maxDist = hypot(columns.toFloat(), 7f)
                    val front = reveal.value * (maxDist + 10f)
                    val waveT = wave?.progress?.value ?: 1f
                    val waveFront = waveT * 30f
                    val waveStrength = wave?.strength ?: 0f

                    months.forEach { (col, layout) -> drawText(layout, topLeft = Offset(col * stepPx, 0f)) }

                    cells.forEach { cell ->
                        val col = ((cell.day - gridStart) / 7).toInt()
                        val row = rowOf(cell.day)
                        val dist = hypot((todayCol - col).toFloat(), (todayRow - row).toFloat())
                        val shown = ((front - dist) / 10f).coerceIn(0f, 1f)
                        if (shown <= 0f) return@forEach
                        val d = dist - waveFront
                        val bump = if (waveT < 1f) exp(-(d * d) / 3.5f) * (1f - waveT) * waveStrength else 0f
                        val tl = Offset(col * stepPx, top + row * stepPx)
                        var s = (0.35f + 0.65f * easeOutBack(shown)) * (1f + 0.28f * bump)
                        if (cell.day == popped) s *= pop.value
                        val isToday = cell.day == today
                        scale(s, s, pivot = tl + Offset(cellPx / 2, cellPx / 2)) {
                            drawCell(
                                cell = cell,
                                tl = tl,
                                cellPx = cellPx,
                                radius = radius,
                                path = path,
                                accent = accent,
                                base = lerp(c.cell, accent, (bump * 0.4f).coerceAtMost(0.6f)),
                                void = lerp(c.cellVoid, accent, (bump * 0.25f).coerceAtMost(0.4f)),
                                skipped = c.ink3.copy(alpha = 0.45f),
                                separator = background,
                                progress = if (isToday) { i -> slices.getOrNull(i)?.value ?: 0f } else null,
                                alpha = shown,
                            )
                        }
                    }
                }
                TodayRing(
                    pending = todaySlots.any { it == SlotStatus.PENDING },
                    visible = { reveal.value },
                    modifier = Modifier.offset(x = step * todayCol - 4.dp, y = labelHeight + step * todayRow - 4.dp),
                    cellSize = cellSize,
                )
            }
        }
    }
}

/** Kept in its own layer so the breathing doesn't redraw the whole grid every frame. */
@Composable
private fun TodayRing(pending: Boolean, visible: () -> Float, cellSize: Dp, modifier: Modifier) {
    val ink = P1ll.colors.ink
    val breath = if (pending) {
        rememberInfiniteTransition(label = "today").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring",
        )
    } else {
        null
    }
    Canvas(modifier.size(cellSize + 8.dp)) {
        val inset = 1.5.dp.toPx()
        val b = breath?.value ?: 0.6f
        drawRoundRect(
            ink.copy(alpha = (0.16f + 0.26f * b) * visible().coerceIn(0f, 1f)),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius(cellSize.toPx() * 0.3f + 2.5.dp.toPx()),
            style = Stroke(1.5.dp.toPx()),
        )
    }
}

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

private fun DrawScope.drawCell(
    cell: DayCell,
    tl: Offset,
    cellPx: Float,
    radius: CornerRadius,
    path: Path,
    accent: Color,
    base: Color,
    void: Color,
    skipped: Color,
    separator: Color,
    progress: ((Int) -> Float)?,
    alpha: Float,
    separatorWidth: Float? = null,
) {
    val size = Size(cellPx, cellPx)
    val n = cell.slots.size
    if (n == 0) {
        drawRoundRect(void, tl, size, radius, alpha = alpha)
        return
    }
    val fill: (Int) -> Float = progress ?: { i -> if (cell.slots[i] == SlotStatus.TAKEN) 1f else 0f }
    val minFill = (0 until n).minOf(fill)
    if (minFill >= 1f) {
        drawRoundRect(accent, tl, size, radius, alpha = alpha)
        return
    }
    drawRoundRect(base, tl, size, radius, alpha = alpha)
    path.reset()
    path.addRoundRect(RoundRect(tl.x, tl.y, tl.x + cellPx, tl.y + cellPx, radius))
    val center = tl + Offset(cellPx / 2, cellPx / 2)
    val big = cellPx * 1.5f
    val arcTl = center - Offset(big / 2, big / 2)
    val sweep = 360f / n
    clipPath(path) {
        for (i in 0 until n) {
            val start = -90f + i * sweep
            if (cell.slots[i] == SlotStatus.SKIPPED) {
                drawArc(skipped, start, sweep, true, arcTl, Size(big, big), alpha = alpha)
            }
            val f = fill(i)
            if (f > 0f) drawArc(accent, start, sweep * f, true, arcTl, Size(big, big), alpha = alpha)
        }
        // Cut where a taken run meets a missed one; missed doses keep faint cuts so they can be counted.
        if (n in 2..12) {
            val w = separatorWidth ?: ((if (n > 6) 0.8f else 1.2f) * density)
            val sepAlpha = alpha * (1f - minFill)
            for (i in 0 until n) {
                val here = cell.slots[i]
                val before = cell.slots[(i - 1 + n) % n]
                val strength = when {
                    here != before -> 1f
                    here == SlotStatus.PENDING -> 0.5f
                    else -> continue
                }
                val a = Math.toRadians((-90f + i * sweep).toDouble())
                val end = center + Offset(cos(a).toFloat() * cellPx, sin(a).toFloat() * cellPx)
                drawLine(separator, center, end, w, alpha = sepAlpha * strength)
            }
        }
    }
}

/** One day's square on its own, drawn large. */
@Composable
fun DayPie(cell: DayCell, accent: Color, modifier: Modifier = Modifier, background: Color = P1ll.colors.surface) {
    val c = P1ll.colors
    val path = remember { Path() }
    val slices = remember(cell.slots.size) {
        List(cell.slots.size) { Animatable(if (cell.slots[it] == SlotStatus.TAKEN) 1f else 0f) }
    }
    LaunchedEffect(cell.slots) {
        coroutineScope {
            cell.slots.forEachIndexed { i, s ->
                launch { slices[i].animateTo(if (s == SlotStatus.TAKEN) 1f else 0f, spring(0.8f, 160f)) }
            }
        }
    }
    Canvas(modifier) {
        val px = size.minDimension
        drawCell(
            cell = cell,
            tl = Offset.Zero,
            cellPx = px,
            radius = CornerRadius(px * 0.3f),
            path = path,
            accent = accent,
            base = c.cell,
            void = c.cellVoid,
            skipped = c.ink3.copy(alpha = 0.45f),
            separator = background,
            progress = { i -> slices.getOrNull(i)?.value ?: 0f },
            alpha = 1f,
            separatorWidth = px * 0.04f,
        )
    }
}
