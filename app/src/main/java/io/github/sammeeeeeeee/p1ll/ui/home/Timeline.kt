package io.github.sammeeeeeeee.p1ll.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.domain.Phase
import io.github.sammeeeeeeee.p1ll.domain.PillToday
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.ui.Describe
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
import io.github.sammeeeeeeee.p1ll.ui.components.tappable
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.nav.shared
import io.github.sammeeeeeeee.p1ll.ui.theme.InstrumentNarrow
import io.github.sammeeeeeeee.p1ll.ui.theme.Motion
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.onColor
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/*
 * Today, laid out like a commit log: a rail down the left, one node per dose in time order.
 * Taken doses fill their node in the pill's colour; a "now" marker cuts across the rail.
 */

private val TimeWidth = 54.dp
private val RailWidth = 30.dp
private val NodeY = 23.dp

private val TimeStyle = TextStyle(
    fontFamily = InstrumentNarrow,
    fontWeight = FontWeight.SemiBold,
    fontSize = 19.sp,
    lineHeight = 22.sp,
    fontFeatureSettings = "tnum",
)

data class DoseRow(
    val card: PillToday,
    val slot: Slot,
    /** Only the first dose at a given minute prints the time, like a timetable. */
    val showTime: Boolean,
    /** The pill's first row carries its shared elements into the detail screen. */
    val primary: Boolean,
)

fun doseRows(cards: List<PillToday>): List<DoseRow> {
    val rows = cards
        .filter { it.phase == Phase.DUE || it.phase == Phase.LATER || it.phase == Phase.DONE }
        .flatMap { card -> card.slots.map { card to it } }
        .sortedWith(compareBy({ it.second.minute }, { it.first.pill.name.lowercase() }))
    val seen = mutableSetOf<Long>()
    return rows.mapIndexed { i, (card, slot) ->
        DoseRow(
            card = card,
            slot = slot,
            showTime = i == 0 || rows[i - 1].second.minute != slot.minute,
            primary = seen.add(card.pill.id),
        )
    }
}

@Composable
fun GroupHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val c = P1ll.colors
    Row(modifier.fillMaxWidth().padding(top = 34.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = Type.headline.copy(fontSize = 21.sp, lineHeight = 26.sp))
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            AnimatedContent(trailing, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "count") {
                Text(it, style = Type.label, color = c.ink2)
            }
        }
    }
}

private enum class Rail { SOLID, DASHED, NONE }

@Composable
fun DoseRowView(
    row: DoseRow,
    now: ZonedDateTime,
    first: Boolean,
    last: Boolean,
    onTake: (Slot) -> Unit,
    onSkip: (Slot) -> Unit,
    onUntake: (Slot) -> Unit,
    onOpen: (Pill) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = P1ll.colors
    val context = LocalContext.current
    val pill = row.card.pill
    val slot = row.slot
    val color = pillColor(pill.color, c.dark)
    val nowMinute = now.hour * 60 + now.minute
    val past = slot.minute <= nowMinute
    val due = slot.status == SlotStatus.PENDING && past
    val done = slot.status != SlotStatus.PENDING
    val rail = if (past) Rail.SOLID else Rail.DASHED

    val nameColor by animateColorAsState(if (done) c.ink2 else c.ink, tween(300), label = "name")
    val timeColor by animateColorAsState(
        when {
            due -> c.ink
            done -> c.ink3
            else -> c.ink2
        },
        tween(300),
        label = "time",
    )
    val glyphAlpha by animateFloatAsState(if (done) 0.5f else 1f, tween(300), label = "glyphAlpha")

    SwipeRow(
        color = color,
        onTake = if (slot.status == SlotStatus.PENDING) ({ onTake(slot) }) else null,
        onSkip = if (slot.status == SlotStatus.PENDING) ({ onSkip(slot) }) else null,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(c.bg)
                .tappable(pressedScale = 0.985f) { onOpen(pill) },
        ) {
            Box(Modifier.width(TimeWidth).padding(top = 12.dp)) {
                if (row.showTime) Text(Fmt.time(context, slot.minute), style = TimeStyle, color = timeColor)
            }
            Node(
                status = slot.status,
                due = due,
                color = color,
                top = if (first) Rail.NONE else rail,
                bottom = if (last) Rail.NONE else rail,
                onTap = {
                    if (slot.status == SlotStatus.PENDING) onTake(slot) else onUntake(slot)
                },
                modifier = Modifier.width(RailWidth).fillMaxHeight(),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(top = 11.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillGlyph(
                        pill.shape,
                        color,
                        (if (row.primary) Modifier.shared("glyph-${pill.id}") else Modifier)
                            .size(20.dp)
                            .graphicsLayer { alpha = glyphAlpha },
                        badge = false,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        pill.name,
                        style = Type.headline,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (row.primary) Modifier.shared("name-${pill.id}") else Modifier,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(meta(pill), style = Type.label, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.heightIn(min = 46.dp).padding(top = 5.dp), contentAlignment = Alignment.TopEnd) {
                val trailing = when {
                    due -> Trailing.Take
                    slot.status == SlotStatus.TAKEN -> Trailing.Text(
                        slot.at?.let { Fmt.time(context, Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) } ?: "taken",
                    )
                    slot.status == SlotStatus.SKIPPED -> Trailing.Text("skipped")
                    else -> Trailing.Text(Fmt.until((slot.minute - nowMinute).toLong()))
                }
                AnimatedContent(
                    trailing,
                    transitionSpec = {
                        (scaleIn(spring(0.6f, 500f), 0.7f) + fadeIn(tween(160)))
                            .togetherWith(scaleOut(tween(140), 0.7f) + fadeOut(tween(120)))
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "trailing",
                ) { t ->
                    when (t) {
                        Trailing.Take -> ActionChip("Take", Icons.Check, color, onColor(color)) { onTake(slot) }
                        is Trailing.Text -> Text(t.text, style = Type.small, color = c.ink3, modifier = Modifier.padding(top = 11.dp))
                    }
                }
            }
        }
    }
}

private sealed interface Trailing {
    data object Take : Trailing
    data class Text(val text: String) : Trailing
}

@Composable
private fun meta(pill: Pill): String {
    val parts = mutableListOf<String>()
    if (pill.dosage.isNotBlank()) parts += pill.dosage.trim()
    if (pill.optional) parts += "optional"
    val stock = pill.stock
    if (stock != null && pill.refillAt != null && stock <= pill.refillAt) parts += "$stock left"
    if (parts.isEmpty()) parts += Describe.schedule(pill)
    return parts.joinToString(" · ")
}

@Composable
fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .tappable(pressedScale = 0.9f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .height(36.dp)
            .padding(start = 10.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = Type.label.copy(fontWeight = FontWeight.SemiBold), color = content)
    }
}

/** A dose on the rail. Tapping it takes (or un-takes) that dose directly. */
@Composable
private fun Node(
    status: SlotStatus,
    due: Boolean,
    color: Color,
    top: Rail,
    bottom: Rail,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = P1ll.colors
    val railColor = lerp(c.line, c.ink3, 0.35f)
    val fill by animateFloatAsState(if (status == SlotStatus.TAKEN) 1f else 0f, spring(0.55f, 420f), label = "node")
    val check by animateFloatAsState(
        if (status == SlotStatus.TAKEN) 1f else 0f,
        if (status == SlotStatus.TAKEN) tween(320, 80, Motion.decelerate) else tween(100),
        label = "check",
    )
    val burst = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (previous == SlotStatus.PENDING && status == SlotStatus.TAKEN) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
        }
        previous = status
    }
    val breath: State<Float>? = if (due) {
        rememberInfiniteTransition(label = "due").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "b",
        )
    } else {
        null
    }
    val checkPath = remember { Path() }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 7f)) }

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val x = size.width / 2
            val y = NodeY.toPx()
            val gap = 11.dp.toPx()
            val w = 2.dp.toPx()
            fun segment(from: Float, to: Float, style: Rail) {
                if (style == Rail.NONE || to <= from) return
                drawLine(railColor, Offset(x, from), Offset(x, to), w, StrokeCap.Round, if (style == Rail.DASHED) dash else null)
            }
            segment(0f, y - gap, top)
            segment(y + gap, size.height, bottom)

            val center = Offset(x, y)
            val r = 7.5.dp.toPx()
            breath?.value?.let { b -> drawCircle(color.copy(alpha = 0.25f * (1f - b)), r * (1.3f + 1.2f * b), center) }
            when {
                status == SlotStatus.SKIPPED -> {
                    drawCircle(c.ink3, r - 1.dp.toPx(), center, style = Stroke(1.75.dp.toPx()))
                    drawLine(c.ink3, center - Offset(r * 0.45f, 0f), center + Offset(r * 0.45f, 0f), 1.75.dp.toPx(), StrokeCap.Round)
                }
                else -> {
                    drawCircle(c.bg, r, center)
                    drawCircle(
                        if (due) color else c.ink3,
                        r - 1.dp.toPx(),
                        center,
                        style = Stroke(if (due) 2.2.dp.toPx() else 1.75.dp.toPx()),
                    )
                    if (due) drawCircle(color.copy(alpha = 0.18f), r - 2.dp.toPx(), center)
                    if (fill > 0f) drawCircle(color, (r + 1.dp.toPx()) * fill, center)
                    if (check > 0f) drawCheck(checkPath, center, r, check, onColor(color))
                }
            }
            if (burst.value < 1f) drawBurst(center, r, burst.value, color)
        }
        Box(
            Modifier
                .offset(y = NodeY - 22.dp)
                .size(RailWidth, 44.dp)
                .tappable(pressedScale = 0.8f, onClick = onTap),
        )
    }
}

private fun DrawScope.drawCheck(path: Path, center: Offset, r: Float, t: Float, color: Color) {
    path.reset()
    path.moveTo(center.x - r * 0.42f, center.y + r * 0.02f)
    path.lineTo(center.x - r * 0.1f, center.y + r * 0.32f)
    path.lineTo(center.x + r * 0.45f, center.y - r * 0.3f)
    val measure = androidx.compose.ui.graphics.PathMeasure()
    measure.setPath(path, false)
    val seg = Path()
    measure.getSegment(0f, measure.length * t, seg, true)
    drawPath(seg, color, style = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawBurst(center: Offset, r: Float, t: Float, color: Color) {
    val fade = 1f - t
    drawCircle(color.copy(alpha = 0.5f * fade), r * (1.2f + 1.6f * t), center, style = Stroke((2.dp.toPx() * fade).coerceAtLeast(0.5f)))
    repeat(6) { i ->
        val a = (i * 60f - 90f) * PI.toFloat() / 180f
        val d = r * (1.5f + 1.4f * t)
        drawCircle(color.copy(alpha = fade), 2.2.dp.toPx() * fade, Offset(center.x + cos(a) * d, center.y + sin(a) * d))
    }
}

/** The "now" marker: where the solid past ends and the dashed rest of the day begins. */
@Composable
fun NowRow(now: ZonedDateTime, hasAbove: Boolean, hasBelow: Boolean, modifier: Modifier = Modifier) {
    val c = P1ll.colors
    val context = LocalContext.current
    val railColor = lerp(c.line, c.ink3, 0.35f)
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 7f)) }
    Row(modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(TimeWidth)) {
            Text(Fmt.time(context, now), style = TimeStyle.copy(fontSize = 15.sp), color = c.accent)
        }
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val x = RailWidth.toPx() / 2
            val y = size.height / 2
            val w = 2.dp.toPx()
            if (hasAbove) drawLine(railColor, Offset(x, 0f), Offset(x, y - 6.dp.toPx()), w, StrokeCap.Round)
            if (hasBelow) drawLine(railColor, Offset(x, y + 6.dp.toPx()), Offset(x, size.height), w, StrokeCap.Round, dash)
            drawCircle(c.accent, 4.dp.toPx(), Offset(x, y))
            drawLine(c.accent.copy(alpha = 0.45f), Offset(x + 9.dp.toPx(), y), Offset(size.width, y), 1.5.dp.toPx(), StrokeCap.Round)
        }
    }
}

/** As-needed and not-today pills: same grid as the timeline, glyph where the node would be. */
@Composable
fun QuietRow(
    card: PillToday,
    now: ZonedDateTime,
    onLog: ((PillToday) -> Unit)?,
    onOpen: (Pill) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = P1ll.colors
    val context = LocalContext.current
    val pill = card.pill
    val color = pillColor(pill.color, c.dark)
    val dim = if (onLog == null) 0.6f else 1f
    Row(
        modifier
            .fillMaxWidth()
            .tappable(pressedScale = 0.985f) { onOpen(pill) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(TimeWidth))
        Box(Modifier.width(RailWidth), contentAlignment = Alignment.Center) {
            PillGlyph(pill.shape, color, Modifier.shared("glyph-${pill.id}").size(24.dp).graphicsLayer { alpha = dim }, badge = false)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).graphicsLayer { alpha = dim }) {
            Text(pill.name, style = Type.headline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.shared("name-${pill.id}"))
            val sub = when {
                card.phase == Phase.FINISHED -> pill.endDay?.let { "Ended ${Fmt.shortDate(it)}" } ?: "Finished"
                onLog != null -> {
                    val last = card.lastTakenAt
                    val lastText = last?.let { "last ${Fmt.ago((now.toInstant().toEpochMilli() - it) / 60_000)}" }
                    listOfNotNull(pill.dosage.takeIf { it.isNotBlank() }, lastText, if (card.takenToday > 1) "${card.takenToday} today" else null)
                        .joinToString(" · ").ifEmpty { "As needed" }
                }
                else -> listOfNotNull(Describe.schedule(pill), card.next?.let { "next ${Describe.whenNext(context, it, now)}" }).joinToString(" · ")
            }
            Text(sub, style = Type.label, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onLog != null) {
            Spacer(Modifier.width(8.dp))
            var logged by remember { mutableIntStateOf(0) }
            LaunchedEffect(logged) {
                if (logged > 0) {
                    kotlinx.coroutines.delay(1300)
                    logged = 0
                }
            }
            AnimatedContent(logged > 0, transitionSpec = {
                (scaleIn(spring(0.6f, 500f), 0.7f) + fadeIn(tween(160))) togetherWith (scaleOut(tween(140), 0.7f) + fadeOut(tween(120)))
            }, label = "log") { flashed ->
                if (flashed) ActionChip("Logged", Icons.Check, color, onColor(color)) {}
                else ActionChip("Log", Icons.Plus, c.raised, c.ink) {
                    logged++
                    onLog(card)
                }
            }
        }
    }
}

/**
 * Swipe right to take, left to skip. Past the threshold the action arms with a tick; beyond it
 * the row drags with resistance; letting go springs it home.
 */
@Composable
fun SwipeRow(
    color: Color,
    onTake: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = P1ll.colors
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var drag by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(1) }
    var armed by remember { mutableStateOf(false) }
    val threshold = width * 0.26f

    fun shape(raw: Float): Float {
        val a = abs(raw)
        return if (a <= threshold) raw else sign(raw) * (threshold + (a - threshold) * 0.3f)
    }

    Box(
        modifier
            .onSizeChanged { width = it.width }
            .pointerInput(onTake != null, onSkip != null) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = offset.value },
                    onDragEnd = {
                        when {
                            drag > threshold -> onTake?.invoke()
                            drag < -threshold -> onSkip?.invoke()
                        }
                        armed = false
                        drag = 0f
                        scope.launch { offset.animateTo(0f, spring(0.55f, 360f)) }
                    },
                    onDragCancel = {
                        armed = false
                        drag = 0f
                        scope.launch { offset.animateTo(0f, spring(0.55f, 360f)) }
                    },
                ) { change, dx ->
                    val next = drag + dx
                    drag = when {
                        next > 0 && onTake == null -> 0f
                        next < 0 && onSkip == null -> 0f
                        else -> next
                    }
                    if (drag != 0f) change.consume()
                    scope.launch { offset.snapTo(shape(drag)) }
                    val nowArmed = abs(drag) > threshold
                    if (nowArmed != armed) {
                        armed = nowArmed
                        if (nowArmed) feedback.threshold()
                    }
                }
            },
    ) {
        val o = offset.value
        if (o != 0f) {
            val progress = (abs(o) / threshold).coerceIn(0f, 1f)
            val taking = o > 0
            val tint = if (taking) color else c.ink2
            val iconScale by animateFloatAsState(if (armed) 1.15f else 0.6f + 0.4f * progress, spring(0.45f, 600f), label = "swipeIcon")
            Row(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.copy(alpha = if (armed) 0.9f else 0.1f + 0.2f * progress))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (taking) Arrangement.Start else Arrangement.End,
            ) {
                val fg = if (armed) onColor(tint) else tint
                if (!taking) Text("Skip", style = Type.label, color = fg, modifier = Modifier.padding(end = 8.dp))
                Icon(
                    if (taking) Icons.Check else Icons.Skip,
                    null,
                    tint = fg,
                    modifier = Modifier.size(22.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                )
                if (taking) Text("Take", style = Type.label, color = fg, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Box(Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }) { content() }
    }
}
