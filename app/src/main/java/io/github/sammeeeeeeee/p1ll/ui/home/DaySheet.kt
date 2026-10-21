package io.github.sammeeeeeeee.p1ll.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.buildCells
import io.github.sammeeeeeeee.p1ll.domain.isActiveOn
import io.github.sammeeeeeeee.p1ll.domain.on
import io.github.sammeeeeeeee.p1ll.domain.plan
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.Snapshot
import io.github.sammeeeeeeee.p1ll.ui.components.Chip
import io.github.sammeeeeeeee.p1ll.ui.components.DayPie
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
import io.github.sammeeeeeeee.p1ll.ui.components.TimeDialog
import io.github.sammeeeeeeee.p1ll.ui.components.enter
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import java.time.Instant
import java.time.ZoneId

/** Tapping a square opens its day: every scheduled dose, editable after the fact. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    day: Long,
    snapshot: Snapshot,
    onSet: (Slot, SlotStatus) -> Unit,
    onRemoveDose: (Long) -> Unit,
    /** Log an as-needed dose on this day at the given minute. */
    onAddDose: (Long, Int) -> Unit,
    onDismiss: () -> Unit,
    pillId: Long? = null,
) {
    val c = P1ll.colors
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val zone = snapshot.now.zone
    val pills = snapshot.pills.filter { pillId == null || it.id == pillId }
    val rows = pills.filter { !it.asNeeded }.flatMap { p -> p.plan(day, zone, snapshot.index).map { p to it } }
        .sortedBy { it.second.minute }
    val extras = pills.filter { it.asNeeded }.flatMap { p ->
        snapshot.index.on(p.id, day).filter { it.status == DoseStatus.TAKEN }.map { p to it }
    }.sortedBy { it.second.at }
    val cell = buildCells(pills, snapshot.index, day, day, zone, includeOptional = pillId != null).first()
    val accent = if (pillId != null && pills.isNotEmpty()) pillColor(pills.first().color, c.dark) else c.accent
    val editable = day <= snapshot.today
    val loggable = if (editable) pills.filter { it.asNeeded && it.isActiveOn(day) } else emptyList()
    var addingFor by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.ink,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(36.dp, 4.dp).clip(CircleShape).background(c.line))
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Fmt.dayMonth(day), style = Type.title)
                    if (rows.isEmpty() && extras.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Nothing scheduled", style = Type.label, color = c.ink2)
                    }
                }
                DayPie(cell, accent, Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            rows.forEachIndexed { i, (pill, slot) ->
                Row(
                    Modifier.enter(i + 1).fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pill.name, style = Type.bodyStrong, maxLines = 1)
                        val at = slot.at?.takeIf { slot.status == SlotStatus.TAKEN }?.let {
                            ", taken ${Fmt.time(context, Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))}"
                        } ?: ""
                        Text(Fmt.time(context, slot.minute) + at, style = Type.small, color = c.ink2)
                    }
                    if (editable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip("Taken", slot.status == SlotStatus.TAKEN, {
                                val next = if (slot.status == SlotStatus.TAKEN) SlotStatus.PENDING else SlotStatus.TAKEN
                                if (next == SlotStatus.TAKEN) feedback.taken(1) else feedback.undo()
                                onSet(slot, next)
                            }, leading = if (slot.status == SlotStatus.TAKEN) Icons.Check else null)
                            Chip("Skipped", slot.status == SlotStatus.SKIPPED, {
                                feedback.tick()
                                onSet(slot, if (slot.status == SlotStatus.SKIPPED) SlotStatus.PENDING else SlotStatus.SKIPPED)
                            })
                        }
                    }
                }
            }
            extras.forEachIndexed { i, (pill, dose) ->
                Row(Modifier.enter(rows.size + i + 1).fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pill.name, style = Type.bodyStrong, maxLines = 1)
                        val t = Instant.ofEpochMilli(dose.at).atZone(ZoneId.systemDefault())
                        Text("Taken ${Fmt.time(context, t)}", style = Type.small, color = c.ink2)
                    }
                    Chip("Remove", false, {
                        feedback.undo()
                        onRemoveDose(dose.id)
                    })
                }
            }
            loggable.forEachIndexed { i, pill ->
                Row(Modifier.enter(rows.size + extras.size + i + 1).fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(pill.name, style = Type.bodyStrong, maxLines = 1, modifier = Modifier.weight(1f))
                    Chip("Log", false, { addingFor = pill.id }, leading = Icons.Plus)
                }
            }
        }
    }
    addingFor?.let { id ->
        val now = snapshot.now
        TimeDialog(
            initial = if (day == snapshot.today) now.hour * 60 + now.minute else 12 * 60,
            onDismiss = { addingFor = null },
            onPick = { minute ->
                feedback.taken(1)
                onAddDose(id, minute)
                addingFor = null
            },
        )
    }
}
