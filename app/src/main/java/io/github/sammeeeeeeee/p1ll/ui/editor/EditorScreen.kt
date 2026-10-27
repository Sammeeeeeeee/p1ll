package io.github.sammeeeeeeee.p1ll.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.PillShape
import io.github.sammeeeeeeee.p1ll.data.ScheduleType
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.components.Chip
import io.github.sammeeeeeeee.p1ll.ui.components.CircleButton
import io.github.sammeeeeeeee.p1ll.ui.components.DateDialog
import io.github.sammeeeeeeee.p1ll.ui.components.TimeDialog
import io.github.sammeeeeeeee.p1ll.ui.components.Divider
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
import io.github.sammeeeeeeee.p1ll.ui.components.PrimaryButton
import io.github.sammeeeeeeee.p1ll.ui.components.Segmented
import io.github.sammeeeeeeee.p1ll.ui.components.SettingRow
import io.github.sammeeeeeeee.p1ll.ui.components.Stepper
import io.github.sammeeeeeeee.p1ll.ui.components.Surface
import io.github.sammeeeeeeee.p1ll.ui.components.enter
import io.github.sammeeeeeeee.p1ll.ui.components.Toggle
import io.github.sammeeeeeeee.p1ll.ui.components.tappable
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.nav.morph
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.PillColorOrder
import io.github.sammeeeeeeee.p1ll.ui.theme.PillColors
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Stable
class EditorState(private val base: Pill?, today: Long, usedColors: Set<Int>) {
    var name by mutableStateOf(base?.name ?: "")
    var dosage by mutableStateOf(base?.dosage ?: "")
    var color by mutableIntStateOf(base?.color ?: (PillColors.indices.firstOrNull { it !in usedColors } ?: 0))
    var shape by mutableStateOf(base?.shape ?: PillShape.ROUND)
    var schedule by mutableStateOf(base?.schedule ?: ScheduleType.DAILY)
    var weekdays by mutableIntStateOf(base?.weekdays ?: Pill.ALL_WEEK)
    var interval by mutableIntStateOf(base?.intervalDays ?: 2)
    val times = mutableStateListOf<Int>().apply { addAll(base?.times?.takeIf { it.isNotEmpty() } ?: listOf(8 * 60)) }
    var startDay by mutableLongStateOf(base?.startDay ?: today)
    var endDay by mutableStateOf(base?.endDay)
    var optional by mutableStateOf(base?.optional ?: false)
    var notes by mutableStateOf(base?.notes ?: "")
    var trackStock by mutableStateOf(base?.stock != null)
    var stock by mutableStateOf(base?.stock?.toString() ?: "30")
    var perDose by mutableIntStateOf(base?.perDose ?: 1)
    var refillAt by mutableStateOf(base?.refillAt?.toString() ?: "7")
    var backfill by mutableStateOf(true)

    val isNew get() = base == null

    val valid: Boolean
        get() = name.isNotBlank() &&
            (schedule == ScheduleType.AS_NEEDED || times.isNotEmpty()) &&
            (schedule != ScheduleType.WEEKDAYS || weekdays != 0)

    fun addTime() {
        val next = times.maxOrNull()?.let { (it + 6 * 60).coerceAtMost(22 * 60) } ?: (8 * 60)
        var t = next
        while (t in times) t = (t + 60) % (24 * 60)
        times.add(t)
        times.sort()
    }

    fun build(now: Long): Pill = Pill(
        id = base?.id ?: 0,
        name = name.trim(),
        dosage = dosage.trim(),
        color = color,
        shape = shape,
        schedule = schedule,
        weekdays = weekdays,
        intervalDays = interval,
        times = if (schedule == ScheduleType.AS_NEEDED) emptyList() else times.distinct().sorted(),
        startDay = startDay,
        endDay = endDay,
        optional = optional,
        notes = notes.trim(),
        stock = if (trackStock) stock.toIntOrNull() ?: 0 else null,
        perDose = perDose,
        refillAt = if (trackStock) refillAt.toIntOrNull() else null,
        createdAt = base?.createdAt ?: now,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    initial: Pill?,
    today: Long,
    usedColors: Set<Int>,
    fromFab: Boolean,
    onClose: () -> Unit,
    /** The pill, and whether a backdated start should fill its past doses in as taken. */
    onSave: (Pill, Boolean) -> Unit,
) {
    val c = P1ll.colors
    val feedback = LocalFeedback.current
    val state = remember { EditorState(initial, today, usedColors) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val nameFocus = remember { FocusRequester() }
    var editingTime by remember { mutableStateOf<Int?>(null) }
    var pickingEnd by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    val color by animateColorAsState(pillColor(state.color, c.dark), spring(1f, 300f), label = "color")

    LaunchedEffect(Unit) {
        if (state.isNew) {
            delay(420)
            runCatching { nameFocus.requestFocus() }
        }
    }

    Column(
        Modifier
            .morph(if (fromFab) "editor-new" else "editor-${initial?.id}", radius = 22.dp)
            .fillMaxSize()
            .background(c.bg),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = top + 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(Icons.Close, "Close", onClose)
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                if (state.isNew) "Add" else "Save",
                {
                    feedback.saved()
                    onSave(state.build(System.currentTimeMillis()), state.isNew && state.backfill && state.startDay < today)
                },
                enabled = state.valid,
                icon = Icons.Check,
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottom + 40.dp),
        ) {
            AnimatedContent(
                state.shape,
                transitionSpec = {
                    (scaleIn(spring(0.5f, 420f), 0.5f) + fadeIn(tween(120)))
                        .togetherWith(scaleOut(tween(140), 0.6f) + fadeOut(tween(100)))
                },
                label = "glyph",
            ) { shape -> PillGlyph(shape, color, Modifier.size(84.dp)) }
            Spacer(Modifier.height(18.dp))
            Field(
                state.name, { state.name = it }, "Name", Type.title,
                Modifier.focusRequester(nameFocus),
                capitalization = KeyboardCapitalization.Words,
            )
            Spacer(Modifier.height(4.dp))
            Field(state.dosage, { state.dosage = it }, "Dose", Type.body.copy(color = c.ink2))

            Spacer(Modifier.height(24.dp))
            Surface(Modifier.enter(2)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillShape.entries.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { shape ->
                                val selected = shape == state.shape
                                val lift by animateFloatAsState(if (selected) 1.08f else 1f, spring(0.45f, 500f), label = "lift")
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .tappable(pressedScale = 0.88f) {
                                            feedback.tick()
                                            state.shape = shape
                                        }
                                        .graphicsLayer { scaleX = lift; scaleY = lift }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) color.copy(alpha = 0.12f) else c.surface)
                                        .border(if (selected) 2.5.dp else 0.dp, if (selected) color else c.surface, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PillGlyph(shape, if (selected) color else c.ink3, Modifier.fillMaxSize(0.64f), badge = false)
                                }
                            }
                        }
                    }
                }
                Divider()
                Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillColorOrder.chunked(6).forEach { row ->
                        Row {
                            row.forEach { i ->
                                val selected = i == state.color
                                val ring by animateFloatAsState(if (selected) 1f else 0f, spring(0.5f, 500f), label = "ring")
                                val swatch = pillColor(i, c.dark)
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .tappable(pressedScale = 0.8f) {
                                            feedback.tick()
                                            state.color = i
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(Modifier.size(34.dp).graphicsLayer { alpha = ring; scaleX = 0.7f + 0.3f * ring; scaleY = 0.7f + 0.3f * ring }
                                        .border(2.dp, swatch, CircleShape))
                                    Box(Modifier.size(24.dp - 4.dp * ring).clip(CircleShape).background(swatch))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(Modifier.enter(3)) {
                Column(Modifier.padding(14.dp)) {
                    Segmented(
                        listOf(ScheduleType.DAILY, ScheduleType.WEEKDAYS, ScheduleType.INTERVAL, ScheduleType.AS_NEEDED),
                        state.schedule,
                        label = {
                            when (it) {
                                ScheduleType.DAILY -> "Daily"
                                ScheduleType.WEEKDAYS -> "Some days"
                                ScheduleType.INTERVAL -> "Interval"
                                ScheduleType.AS_NEEDED -> "As needed"
                            }
                        },
                        onSelect = { state.schedule = it },
                    )
                    AnimatedVisibility(
                        state.schedule == ScheduleType.WEEKDAYS,
                        enter = expandVertically(spring(0.9f, 400f)) + fadeIn(),
                        exit = shrinkVertically(spring(0.9f, 400f)) + fadeOut(),
                    ) {
                        WeekdayPicker(state.weekdays, { state.weekdays = it }, Modifier.padding(top = 16.dp))
                    }
                    AnimatedVisibility(
                        state.schedule == ScheduleType.INTERVAL,
                        enter = expandVertically(spring(0.9f, 400f)) + fadeIn(),
                        exit = shrinkVertically(spring(0.9f, 400f)) + fadeOut(),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Every", style = Type.bodyStrong, modifier = Modifier.weight(1f))
                            Stepper(state.interval, { state.interval = it }, 2..30, suffix = "days")
                        }
                    }
                    AnimatedVisibility(
                        state.schedule != ScheduleType.AS_NEEDED,
                        enter = expandVertically(spring(0.9f, 400f)) + fadeIn(),
                        exit = shrinkVertically(spring(0.9f, 400f)) + fadeOut(),
                    ) {
                        Column(Modifier.padding(top = 16.dp)) {
                            TimeChips(state, onEdit = { editingTime = it })
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(Modifier.enter(4)) {
                if (state.schedule != ScheduleType.AS_NEEDED) {
                    SettingRow("Optional", hint = "Never counts as missed") {
                        Toggle(state.optional, { state.optional = it })
                    }
                    Divider()
                }
                StartDate(state, today, onPick = { pickingStart = true })
                AnimatedVisibility(
                    state.isNew && state.startDay < today && state.schedule != ScheduleType.AS_NEEDED,
                    enter = expandVertically(spring(0.9f, 400f)) + fadeIn(),
                    exit = shrinkVertically(spring(0.9f, 400f)) + fadeOut(),
                ) {
                    SettingRow("Mark past doses taken") { Toggle(state.backfill, { state.backfill = it }) }
                }
                Divider()
                EndDate(state, onPick = { pickingEnd = true })
                Divider()
                SettingRow("Track supply") {
                    Toggle(state.trackStock, { state.trackStock = it })
                }
                AnimatedVisibility(
                    state.trackStock,
                    enter = expandVertically(spring(0.9f, 400f)) + fadeIn(),
                    exit = shrinkVertically(spring(0.9f, 400f)) + fadeOut(),
                ) {
                    Column(Modifier.padding(start = 18.dp, end = 14.dp, bottom = 14.dp)) {
                        NumberRow("Pills left", state.stock) { state.stock = it }
                        NumberRow("Refill at", state.refillAt) { state.refillAt = it }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Per dose", style = Type.bodyStrong, modifier = Modifier.weight(1f))
                            Stepper(state.perDose, { state.perDose = it }, 1..10)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(Modifier.enter(5)) {
                Field(
                    state.notes, { state.notes = it }, "Notes", Type.body,
                    Modifier.padding(18.dp),
                    singleLine = false,
                )
            }
        }
    }

    editingTime?.let { index ->
        TimeDialog(
            initial = state.times.getOrElse(index) { 8 * 60 },
            onDismiss = { editingTime = null },
            onPick = { minute ->
                if (index < state.times.size) state.times[index] = minute
                state.times.sort()
                editingTime = null
            },
        )
    }
    if (pickingEnd) {
        DateDialog(state.endDay ?: (today + 13), { pickingEnd = false }, {
            state.endDay = it
            pickingEnd = false
        }, min = maxOf(today, state.startDay))
    }
    if (pickingStart) {
        DateDialog(state.startDay, { pickingStart = false }, {
            state.startDay = it
            pickingStart = false
        })
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val c = P1ll.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style.copy(color = if (style.color.isSpecifiedColor()) style.color else c.ink),
        cursorBrush = SolidColor(c.ink),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = c.ink3))
                inner()
            }
        },
    )
}

private fun androidx.compose.ui.graphics.Color.isSpecifiedColor() = this != androidx.compose.ui.graphics.Color.Unspecified

@Composable
private fun NumberRow(label: String, value: String, onChange: (String) -> Unit) {
    val c = P1ll.colors
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.bodyStrong, modifier = Modifier.weight(1f))
        BasicTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter(Char::isDigit).take(4)) },
            textStyle = Type.headline.copy(color = c.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            cursorBrush = SolidColor(c.ink),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(84.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.raised)
                .padding(vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeChips(state: EditorState, onEdit: (Int) -> Unit) {
    val context = LocalContext.current
    val c = P1ll.colors
    val feedback = LocalFeedback.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.times.forEachIndexed { i, minute ->
            androidx.compose.runtime.key(minute) {
                val appear = remember { androidx.compose.animation.core.Animatable(0.6f) }
                LaunchedEffect(Unit) { appear.animateTo(1f, spring(0.45f, 500f)) }
                Row(
                    Modifier
                        .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                        .tappable(pressedScale = 0.92f) { onEdit(i) }
                        .clip(CircleShape)
                        .background(c.raised)
                        .padding(start = 14.dp, end = if (state.times.size > 1) 6.dp else 14.dp)
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Clock, null, tint = c.ink2, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Fmt.time(context, minute), style = Type.bodyStrong)
                    if (state.times.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(28.dp)
                                .tappable(pressedScale = 0.8f) {
                                    feedback.tick()
                                    state.times.remove(minute)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Close, "Remove", tint = c.ink3, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        Chip("Add time", false, {
            feedback.tick()
            state.addTime()
        }, leading = Icons.Plus)
    }
}

@Composable
private fun WeekdayPicker(mask: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = P1ll.colors
    val feedback = LocalFeedback.current
    val first = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (0..6).map { (first.value - 1 + it) % 7 }.forEach { bit ->
            val on = mask and (1 shl bit) != 0
            val bg by animateColorAsState(if (on) c.accent else c.raised, tween(180), label = "wd")
            val fg by animateColorAsState(if (on) c.onAccent else c.ink2, tween(180), label = "wdText")
            Box(
                Modifier
                    .size(40.dp)
                    .tappable(pressedScale = 0.85f) {
                        feedback.tick()
                        onChange(mask xor (1 shl bit))
                    }
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(DayOfWeek.of(bit + 1).getDisplayName(DateTextStyle.NARROW, Locale.getDefault()), style = Type.label, color = fg)
            }
        }
    }
}

@Composable
private fun StartDate(state: EditorState, today: Long, onPick: () -> Unit) {
    val c = P1ll.colors
    Row(
        Modifier.fillMaxWidth().tappable(pressedScale = 0.98f, onClick = onPick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Starts", style = Type.bodyStrong, modifier = Modifier.weight(1f))
        AnimatedContent(state.startDay, label = "start") { start ->
            Text(if (start == today) "Today" else Fmt.dayMonth(start), style = Type.label, color = if (start == today) c.ink2 else c.ink)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EndDate(state: EditorState, onPick: () -> Unit) {
    val c = P1ll.colors
    val today = LocalDate.now().toEpochDay()
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ends", style = Type.bodyStrong, modifier = Modifier.weight(1f))
            AnimatedContent(state.endDay, label = "end") { end ->
                Text(end?.let { Fmt.dayMonth(it) }.orEmpty(), style = Type.label, color = c.ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Never", state.endDay == null, { state.endDay = null })
            listOf(7 to "7 days", 14 to "14 days", 30 to "30 days").forEach { (n, label) ->
                Chip(label, state.endDay == today + n - 1, { state.endDay = today + n - 1 })
            }
            Chip("Pick…", false, onPick, leading = Icons.Calendar)
        }
    }
}
