package io.github.sammeeeeeeee.p1ll.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.TransformOrigin
import io.github.sammeeeeeeee.p1ll.ui.components.rememberFirstTime
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.domain.Phase
import io.github.sammeeeeeeee.p1ll.domain.PillToday
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.ui.Describe
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.HomeState
import io.github.sammeeeeeeee.p1ll.ui.UndoInfo
import io.github.sammeeeeeeee.p1ll.ui.components.ChartWave
import io.github.sammeeeeeeee.p1ll.ui.components.CircleButton
import io.github.sammeeeeeeee.p1ll.ui.components.DoseChart
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.RollingNumber
import io.github.sammeeeeeeee.p1ll.ui.components.Surface
import io.github.sammeeeeeeee.p1ll.ui.components.reveal
import io.github.sammeeeeeeee.p1ll.ui.components.tappable
import io.github.sammeeeeeeee.p1ll.ui.nav.morph
import io.github.sammeeeeeeee.p1ll.ui.theme.Motion
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import io.github.sammeeeeeeee.p1ll.ui.AppViewModel
import java.time.temporal.ChronoUnit

class HomeActions(
    val take: (PillToday, Slot) -> Unit,
    val skip: (PillToday, Slot) -> Unit,
    val untake: (Slot) -> Unit,
    val log: (PillToday) -> Unit,
    val open: (Pill) -> Unit,
    val add: () -> Unit,
    val settings: () -> Unit,
    val day: (Long) -> Unit,
    val undo: () -> Unit,
)

@Composable
fun HomeScreen(
    state: HomeState,
    undo: UndoInfo?,
    wave: ChartWave,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val c = P1ll.colors
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var showFinished by remember { mutableStateOf(false) }

    val rows = remember(state.cards) { doseRows(state.cards) }
    val nowMinute = state.now.hour * 60 + state.now.minute
    val split = rows.indexOfFirst { it.slot.minute > nowMinute }.let { if (it < 0) rows.size else it }
    val asNeeded = state.cards.filter { it.phase == Phase.AS_NEEDED }
    val notToday = state.cards.filter { it.phase == Phase.NOT_TODAY }

    val list = rememberLazyListState()
    val railPhase = rememberInfiniteTransition(label = "rail").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "railPhase",
    )

    Box(modifier.fillMaxSize().background(c.bg)) {
        CompositionLocalProvider(LocalRailPhase provides railPhase) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = list,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 12.dp, bottom = bottom + 128.dp),
            ) {
                item("header") { Header(state, actions.settings, Modifier.reveal("header", 0).leaving(list, "header")) }
                item("hero") { Hero(state, Modifier.reveal("hero", 1).leaving(list, "hero").padding(top = 26.dp, bottom = 26.dp)) }
                item("chart") {
                    Surface(Modifier.reveal("chart", 2).leaving(list, "chart", shrink = 0.05f), radius = 28.dp) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)) {
                            DoseChart(state.cells, c.accent, wave = wave, onDayClick = actions.day)
                            if (state.cards.isNotEmpty() || state.finished.isNotEmpty()) {
                                Spacer(Modifier.height(18.dp))
                                StatsRow(state)
                            }
                        }
                    }
                }
                if (rows.isNotEmpty()) {
                    item("today") { GroupHeader("Today", Modifier.animateItem().reveal("today", 3)) }
                    rows.forEachIndexed { i, row ->
                        if (i == split) {
                            item("now") { NowRow(state.now, hasAbove = i > 0, hasBelow = true, Modifier.animateItem().reveal("now", 4 + i)) }
                        }
                        item("dose-${row.card.pill.id}-${row.slot.minute}") {
                            DoseRowView(
                                row = row,
                                now = state.now,
                                first = i == 0 && split != 0,
                                last = i == rows.lastIndex && split != rows.size,
                                onTake = { actions.take(row.card, it) },
                                onSkip = { actions.skip(row.card, it) },
                                onUntake = actions.untake,
                                onOpen = actions.open,
                                modifier = Modifier
                                    .animateItem(fadeInSpec = tween(220), placementSpec = spring(0.8f, 380f))
                                    .reveal("dose-${row.card.pill.id}-${row.slot.minute}", 4 + i + if (i >= split) 1 else 0),
                            )
                        }
                    }
                    if (split == rows.size) {
                        item("now") { NowRow(state.now, hasAbove = true, hasBelow = false, Modifier.animateItem().reveal("now", 4 + rows.size)) }
                    }
                }
                if (asNeeded.isNotEmpty()) {
                    item("as-needed") { GroupHeader("As needed", Modifier.animateItem().reveal("as-needed", 9)) }
                    items(asNeeded, key = { "an-${it.pill.id}" }) { card ->
                        QuietRow(card, state.now, actions.log, actions.open, Modifier.animateItem().reveal("an-${card.pill.id}", 9))
                    }
                }
                if (notToday.isNotEmpty()) {
                    item("not-today") { GroupHeader("Not today", Modifier.animateItem().reveal("not-today", 9)) }
                    items(notToday, key = { "nt-${it.pill.id}" }) { card ->
                        QuietRow(card, state.now, null, actions.open, Modifier.animateItem().reveal("nt-${card.pill.id}", 9))
                    }
                }
                if (state.finished.isNotEmpty()) {
                    item("finished") {
                        FinishedHeader(state.finished.size, showFinished, Modifier.animateItem().reveal("finished", 9)) { showFinished = !showFinished }
                    }
                    if (showFinished) {
                        items(state.finished, key = { "f-${it.id}" }) { pill ->
                            QuietRow(
                                PillToday(pill, emptyList(), Phase.FINISHED, null, null, 0, null),
                                state.now,
                                null,
                                actions.open,
                                Modifier.animateItem().reveal("f-${pill.id}", 9),
                            )
                        }
                    }
                }
            }

        }
        Fab(actions.add, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).reveal("fab", 5))
        UndoToast(
            undo,
            actions.undo,
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 20.dp, end = 104.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun Header(state: HomeState, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(Fmt.dayMonth(state.today), style = Type.title, modifier = Modifier.weight(1f))
        CircleButton(Icons.Sliders, "Settings", onSettings)
    }
}

private enum class HeroMode { COUNT, DONE, NONE }

@Composable
private fun Hero(state: HomeState, modifier: Modifier = Modifier) {
    val c = P1ll.colors
    val context = LocalContext.current
    val mode = when {
        state.left > 0 -> HeroMode.COUNT
        state.required > 0 -> HeroMode.DONE
        else -> HeroMode.NONE
    }
    // Each change to the count lands with a squash-and-bounce.
    val squash = remember { Animatable(1f) }
    var lastLeft by remember { mutableIntStateOf(state.left) }
    LaunchedEffect(state.left) {
        if (state.left != lastLeft) {
            lastLeft = state.left
            squash.snapTo(0.82f)
            squash.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 520f))
        }
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            modifier = Modifier.graphicsLayer {
                scaleX = squash.value
                scaleY = squash.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
            targetState = mode,
            transitionSpec = {
                (scaleIn(spring(0.55f, 380f), 0.6f) + fadeIn(tween(200)))
                    .togetherWith(scaleOut(tween(180), 0.8f) + fadeOut(tween(150)))
            },
            label = "hero",
        ) { m ->
            when (m) {
                HeroMode.COUNT -> RollingNumber(state.left, Type.hero, color = c.accent)
                HeroMode.DONE -> DoneMark(Modifier.size(92.dp))
                HeroMode.NONE -> Text("0", style = Type.hero, color = c.ink3)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            val headline = when (mode) {
                HeroMode.COUNT -> if (state.left == 1) "dose left" else "doses left"
                HeroMode.DONE -> "All done"
                HeroMode.NONE -> if (state.cards.isEmpty()) "No pills" else "Nothing due"
            }
            AnimatedContent(headline, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "h") {
                Text(it, style = Type.headline)
            }
            val due = state.due
            val sub: String? = when {
                due.isNotEmpty() -> due.take(2).joinToString(if (due.size > 2) ", " else " and ") { it.pill.name } +
                    (if (due.size > 2) " and ${due.size - 2} more" else "") + " due"
                state.nextUp != null -> {
                    val (pill, at) = state.nextUp
                    val mins = ChronoUnit.MINUTES.between(state.now, at)
                    if (mins < 12 * 60) "${pill.name} ${Fmt.until(mins)}"
                    else "${pill.name} ${Describe.whenNext(context, at, state.now)}"
                }
                else -> null
            }
            if (sub != null) {
                Spacer(Modifier.height(4.dp))
                AnimatedContent(sub, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "s") {
                    Text(it, style = Type.label, color = if (due.isNotEmpty()) c.ink else c.ink2, maxLines = 2)
                }
            }
        }
    }
}

/** A filled accent disc whose check draws itself in. */
@Composable
private fun DoneMark(modifier: Modifier) {
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

@Composable
private fun StatsRow(state: HomeState) {
    val countUp = rememberFirstTime("stats")
    Row(Modifier.fillMaxWidth()) {
        val c = P1ll.colors
        Stat(state.stats.streak, "day streak", Modifier.weight(1f), color = pillColor(0, c.dark), countUp = countUp)
        state.stats.adherence?.let { Stat(it, "30 days", Modifier.weight(1f), suffix = "%", color = c.accent, countUp = countUp) }
    }
}

@Composable
private fun Stat(
    value: Int,
    label: String,
    modifier: Modifier,
    suffix: String = "",
    color: Color = P1ll.colors.ink,
    countUp: Boolean = false,
) {
    val c = P1ll.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(value, Type.number, color = color, countUp = countUp)
            if (suffix.isNotEmpty()) Text(suffix, style = Type.number, color = color.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = Type.small, color = c.ink2)
    }
}

@Composable
private fun FinishedHeader(count: Int, open: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = P1ll.colors
    val turn by animateFloatAsState(if (open) 180f else 0f, spring(0.7f, 400f), label = "chev")
    Row(
        modifier.fillMaxWidth().padding(top = 26.dp).tappable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Finished", style = Type.headline.copy(fontSize = 21.sp, lineHeight = 26.sp), color = c.ink2)
        Spacer(Modifier.width(8.dp))
        Text("$count", style = Type.headline.copy(fontSize = 21.sp, lineHeight = 26.sp), color = c.ink3)
        Spacer(Modifier.weight(1f))
        Icon(Icons.ChevronDown, null, tint = c.ink2, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn })
    }
}

@Composable
private fun Fab(onClick: () -> Unit, modifier: Modifier) {
    val c = P1ll.colors
    Box(
        modifier
            .tappable(pressedScale = 0.88f, onClick = onClick)
            .morph("editor-new", radius = 22.dp)
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = c.accent, spotColor = c.accent)
            .clip(RoundedCornerShape(22.dp))
            .background(c.accent)
            .size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Plus, "Add a pill", tint = c.onAccent, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun UndoToast(info: UndoInfo?, onUndo: () -> Unit, modifier: Modifier) {
    val c = P1ll.colors
    var last by remember { mutableStateOf(info) }
    if (info != null) last = info
    AnimatedVisibility(
        visible = info != null,
        modifier = modifier,
        enter = slideInVertically(spring(0.7f, 420f)) { it * 2 } + fadeIn(tween(160)),
        exit = slideOutVertically(tween(220)) { it * 2 } + fadeOut(tween(160)),
    ) {
        val shown = last ?: return@AnimatedVisibility
        val drain = remember(shown.serial) { Animatable(1f) }
        LaunchedEffect(shown.serial) { drain.animateTo(0f, tween(AppViewModel.UNDO_MS.toInt(), easing = LinearEasing)) }
        Box(
            Modifier
                .shadow(10.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(c.ink),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(shown.label, style = Type.label, color = c.bg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                Row(
                    Modifier
                        .tappable(pressedScale = 0.9f, onClick = onUndo)
                        .clip(CircleShape)
                        .background(c.bg.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Undo, null, tint = c.bg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Undo", style = Type.label, color = c.bg)
                }
            }
            Canvas(Modifier.matchParentSize()) {
                val h = 2.dp.toPx()
                drawLine(
                    c.accent,
                    Offset(size.height / 2, size.height - h),
                    Offset(size.height / 2 + (size.width - size.height) * drain.value, size.height - h),
                    h,
                    StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * As a block scrolls off the top it lags behind the list, shrinks a little and fades, so the
 * page peels away in layers instead of sliding as one sheet.
 */
private fun Modifier.leaving(list: LazyListState, key: Any, shrink: Float = 0.1f) = graphicsLayer {
    val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
    val past = (-item.offset).coerceAtLeast(0).toFloat()
    if (past == 0f) return@graphicsLayer
    val p = (past / item.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    translationY = past * 0.45f
    alpha = 1f - 0.9f * p
    val s = 1f - shrink * p
    scaleX = s
    scaleY = s
    transformOrigin = TransformOrigin(0.5f, 1f)
}
