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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.buildCells
import io.github.sammeeeeeeee.p1ll.domain.on
import io.github.sammeeeeeeee.p1ll.domain.plan
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.Snapshot
import io.github.sammeeeeeeee.p1ll.ui.components.Chip
import io.github.sammeeeeeeee.p1ll.ui.components.DayPie
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
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
                    Text(
                        if (day == snapshot.today) "Today" else Fmt.weekday(day),
                        style = Type.body,
                        color = c.ink2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(Fmt.dayMonth(day), style = Type.title)
                    Spacer(Modifier.height(4.dp))
                    val taken = cell.slots.count { it == SlotStatus.TAKEN }
                    Text(
                        when {
                            cell.slots.isEmpty() && extras.isEmpty() -> "Nothing was scheduled"
                            cell.slots.isEmpty() -> "Nothing scheduled"
                            cell.complete -> "Every dose taken"
                            else -> "$taken of ${cell.slots.size} taken"
                        },
                        style = Type.label,
                        color = c.ink2,
                    )
                }
                DayPie(cell, accent, Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            rows.forEach { (pill, slot) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pill.name, style = Type.bodyStrong, maxLines = 1)
                        val at = slot.at?.let {
                            val t = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            ", ${if (slot.status == SlotStatus.TAKEN) "taken" else "skipped"} at ${Fmt.time(context, t)}"
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
                            Chip("Skip", slot.status == SlotStatus.SKIPPED, {
                                feedback.tick()
                                onSet(slot, if (slot.status == SlotStatus.SKIPPED) SlotStatus.PENDING else SlotStatus.SKIPPED)
                            })
                        }
                    }
                }
            }
            extras.forEach { (pill, dose) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pill.name, style = Type.bodyStrong, maxLines = 1)
                        val t = Instant.ofEpochMilli(dose.at).atZone(ZoneId.systemDefault())
                        Text("As needed, at ${Fmt.time(context, t)}", style = Type.small, color = c.ink2)
                    }
                    Chip("Remove", false, {
                        feedback.undo()
                        onRemoveDose(dose.id)
                    })
                }
            }
            if (rows.isEmpty() && extras.isEmpty()) {
                Text(
                    "Squares stay empty on days with nothing to take.",
                    style = Type.body,
                    color = c.ink2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.raised)
                        .padding(16.dp),
                )
            }
        }
    }
}
