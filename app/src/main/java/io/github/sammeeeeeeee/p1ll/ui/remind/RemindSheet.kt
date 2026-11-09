package io.github.sammeeeeeeee.p1ll.ui.remind

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
import io.github.sammeeeeeeee.p1ll.ui.components.TimeDialog
import io.github.sammeeeeeeee.p1ll.ui.components.enter
import io.github.sammeeeeeeee.p1ll.ui.components.tappable
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Remind me's presets, in minutes from now. */
val REMIND_PRESETS = listOf(15, 30, 60, 120, 240)

fun presetLabel(minutes: Int) = if (minutes < 60) "+$minutes min" else "+${minutes / 60} h"

/** The next time the clock reads [minute] after [now]: later today, or tomorrow. */
fun nextAt(now: ZonedDateTime, minute: Int): ZonedDateTime {
    val at = now.toLocalDate().atTime(minute / 60, minute % 60).atZone(now.zone)
    return if (at.isAfter(now)) at else at.plusDays(1)
}

/** Remind me on a reminder: when it should come back. [onDismiss] runs once the sheet has slid away, picked or not. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindSheet(pill: Pill, minute: Int, now: () -> ZonedDateTime, onPick: (ZonedDateTime) -> Unit, onDismiss: () -> Unit) {
    val c = P1ll.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.surface,
        contentColor = c.ink,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(36.dp, 4.dp).clip(CircleShape).background(c.line))
        },
    ) {
        RemindOptions(pill, minute, now, onPick = { at ->
            onPick(at)
            scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
        })
    }
}

@Composable
fun RemindOptions(pill: Pill, minute: Int, now: () -> ZonedDateTime, onPick: (ZonedDateTime) -> Unit) {
    val c = P1ll.colors
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    var picking by remember { mutableStateOf(false) }
    val opened = remember { now() }

    fun pick(at: ZonedDateTime) {
        feedback.tick()
        onPick(at)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
            .padding(bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(pill.name, style = Type.headline, maxLines = 1)
                Text("Due ${Fmt.time(context, minute)}", style = Type.small, color = c.ink2)
            }
        }
        Spacer(Modifier.height(20.dp))
        // The presets, then null for a time of your own.
        (REMIND_PRESETS + null).chunked(3).forEachIndexed { row, items ->
            if (row > 0) Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { i, minutes ->
                    val modifier = Modifier.enter(row * 3 + i + 1).weight(1f)
                    if (minutes == null) {
                        Tile("Pick time", null, modifier) {
                            feedback.tick()
                            picking = true
                        }
                    } else {
                        Tile(presetLabel(minutes), Fmt.time(context, opened.plusMinutes(minutes.toLong())), modifier) {
                            pick(now().plusMinutes(minutes.toLong()))
                        }
                    }
                }
            }
        }
    }

    if (picking) {
        val initial = opened.plusHours(1).let { it.hour * 60 + it.minute }
        TimeDialog(initial, onDismiss = { picking = false }) { m ->
            picking = false
            pick(nextAt(now(), m))
        }
    }
}

@Composable
private fun Tile(label: String, time: String?, modifier: Modifier, onClick: () -> Unit) {
    val c = P1ll.colors
    Column(
        modifier
            .tappable(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(c.raised)
            .height(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (time == null) {
            Icon(Icons.Clock, null, tint = c.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, style = Type.small, color = c.ink2)
        } else {
            Text(label, style = Type.bodyStrong)
            Text(time, style = Type.small, color = c.ink2)
        }
    }
}
