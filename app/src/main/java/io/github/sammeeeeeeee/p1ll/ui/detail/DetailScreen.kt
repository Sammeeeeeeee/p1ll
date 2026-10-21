package io.github.sammeeeeeeee.p1ll.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.domain.buildCells
import io.github.sammeeeeeeee.p1ll.domain.stats
import io.github.sammeeeeeeee.p1ll.ui.CHART_DAYS
import io.github.sammeeeeeeee.p1ll.ui.Describe
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.Snapshot
import io.github.sammeeeeeeee.p1ll.ui.components.Chip
import io.github.sammeeeeeeee.p1ll.ui.components.CircleButton
import io.github.sammeeeeeeee.p1ll.ui.components.DoseChart
import io.github.sammeeeeeeee.p1ll.ui.components.GhostButton
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
import io.github.sammeeeeeeee.p1ll.ui.components.RollingNumber
import io.github.sammeeeeeeee.p1ll.ui.components.Surface
import io.github.sammeeeeeeee.p1ll.ui.components.enter
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.nav.morph
import io.github.sammeeeeeeee.p1ll.ui.nav.shared
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor

class DetailActions(
    val back: () -> Unit,
    val edit: (Pill) -> Unit,
    val finish: (Pill) -> Unit,
    val delete: (Pill) -> Unit,
    val refill: (Pill, Int) -> Unit,
    val day: (Long) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(pillId: Long, snapshot: Snapshot, actions: DetailActions) {
    val c = P1ll.colors
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    // Keep showing the last known pill while the screen animates away after a delete.
    var last by remember { mutableStateOf<Pill?>(null) }
    snapshot.pills.find { it.id == pillId }?.let { last = it }
    val pill = last ?: return
    val color = pillColor(pill.color, c.dark)
    val today = snapshot.today
    val cells = remember(snapshot, pill) {
        buildCells(listOf(pill), snapshot.index, maxOf(today - CHART_DAYS, minOf(pill.startDay, today) - 7 * 8), today, snapshot.now.zone, includeOptional = true)
    }
    val stat = remember(cells) {
        stats(cells, today, snapshot.doses.count { it.pillId == pill.id && it.status == DoseStatus.TAKEN })
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .morph("card-${pill.id}")
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = top + 8.dp, bottom = bottom + 32.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Icons.Back, "Back", actions.back)
            Spacer(Modifier.weight(1f))
            CircleButton(Icons.Pencil, "Edit", { actions.edit(pill) })
        }
        Spacer(Modifier.height(24.dp))
        PillGlyph(pill.shape, color, Modifier.shared("glyph-${pill.id}").size(92.dp))
        Spacer(Modifier.height(18.dp))
        Text(pill.name, style = Type.title.copy(fontSize = 36.sp, lineHeight = 40.sp), modifier = Modifier.shared("name-${pill.id}"))
        val meta = Describe.meta(pill)
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(meta, style = Type.body, color = c.ink2)
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(Modifier.enter(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!pill.asNeeded) Fact(Icons.Clock, Describe.times(context, pill))
            when {
                pill.endDay != null && pill.endDay < today -> Fact(Icons.Flag, "Ended ${Fmt.shortDate(pill.endDay)}")
                pill.endDay != null -> Fact(Icons.Calendar, "Until ${Fmt.shortDate(pill.endDay)}")
                pill.startDay > today -> Fact(Icons.Calendar, "Starts ${Fmt.shortDate(pill.startDay)}")
            }
            if (pill.optional) Fact(Icons.Bell, "Optional")
        }
        Spacer(Modifier.height(22.dp))

        Surface(Modifier.enter(3), radius = 28.dp) {
            Column(Modifier.padding(16.dp)) {
                DoseChart(cells, color, onDayClick = actions.day)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth()) {
                    if (!pill.asNeeded) {
                        BigStat(stat.streak, "day streak", Modifier.weight(1f))
                        stat.adherence?.let { BigStat(it, "30 days", Modifier.weight(1f), "%") }
                    } else {
                        BigStat(stat.taken, "taken", Modifier.weight(1f))
                    }
                }
            }
        }

        val stock = pill.stock
        AnimatedVisibility(stock != null) {
            Surface(Modifier.enter(4).padding(top = 12.dp), radius = 28.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        RollingNumber(stock ?: 0, Type.number)
                        Text(" left", style = Type.body, color = c.ink2, modifier = Modifier.padding(bottom = 2.dp))
                        Spacer(Modifier.weight(1f))
                        val perDay = pill.times.size * pill.perDose
                        if (!pill.asNeeded && perDay > 0 && stock != null) {
                            val days = stock / perDay
                            if (days > 0) Text(if (days == 1) "1 day" else "$days days", style = Type.label, color = c.ink2)
                        }
                    }
                    val refillAt = pill.refillAt
                    if (refillAt != null && stock != null) {
                        Spacer(Modifier.height(10.dp))
                        val fill by animateFloatAsState(
                            (stock / (refillAt * 4f).coerceAtLeast(1f)).coerceIn(0f, 1f),
                            spring(0.8f, 200f),
                            label = "supply",
                        )
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.raised)) {
                            Box(
                                Modifier
                                    .fillMaxWidth(fill)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(if (stock <= refillAt) c.warn else color),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(14, 28, 30, 90).forEach { n ->
                            Chip("+$n", false, {
                                feedback.saved()
                                actions.refill(pill, n)
                            })
                        }
                    }
                }
            }
        }

        if (pill.notes.isNotBlank()) {
            Surface(Modifier.enter(5).padding(top = 12.dp), radius = 28.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(pill.notes, style = Type.body)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.enter(6), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pill.endDay == null || pill.endDay >= today) {
                GhostButton("Finish course", { actions.finish(pill) }, icon = Icons.Flag)
            }
            GhostButton("Delete", { confirmDelete = true }, color = c.danger, icon = Icons.Trash)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete ${pill.name}?", style = Type.headline) },
            text = {
                Text(
                    "This deletes its history too. To keep it, finish the course instead.",
                    style = Type.body,
                    color = c.ink2,
                )
            },
            confirmButton = {
                GhostButton("Delete", {
                    confirmDelete = false
                    actions.delete(pill)
                }, color = c.danger)
            },
            dismissButton = { GhostButton("Cancel", { confirmDelete = false }) },
        )
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    val c = P1ll.colors
    Row(
        Modifier.clip(CircleShape).background(c.raised).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.ink2, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = Type.label)
    }
}

@Composable
private fun BigStat(value: Int, label: String, modifier: Modifier, suffix: String = "") {
    val c = P1ll.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(value, Type.number)
            if (suffix.isNotEmpty()) Text(suffix, style = Type.number, color = c.ink2)
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = Type.small, color = c.ink2)
    }
}
