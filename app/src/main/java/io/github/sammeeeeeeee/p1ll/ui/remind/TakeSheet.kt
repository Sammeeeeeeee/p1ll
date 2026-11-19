package io.github.sammeeeeeeee.p1ll.ui.remind

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.TakeOutcome
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.components.DayPie
import io.github.sammeeeeeeee.p1ll.ui.components.DoneMark
import io.github.sammeeeeeeee.p1ll.ui.components.GhostButton
import io.github.sammeeeeeeee.p1ll.ui.components.PillGlyph
import io.github.sammeeeeeeee.p1ll.ui.components.RollingNumber
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** How long Take's sheet stays up on its own once it has opened. */
const val TAKE_SHEET_MS = 2600L

/**
 * Take on a reminder, played out: the dose is already stored, so this is only the show. The sheet
 * goes by itself after [TAKE_SHEET_MS], or sooner with a tap outside, a swipe or back.
 */
@Composable
fun TakeSheet(outcome: TakeOutcome, today: Long, onUndo: () -> Unit, onDismiss: () -> Unit) {
    ReminderSheet(onDismiss) { close ->
        LaunchedEffect(Unit) {
            delay(TAKE_SHEET_MS)
            close()
        }
        TakeContent(outcome, today, onUndo = if (outcome.newly) ({ onUndo(); close() }) else null)
    }
}

/** Opens as the day was before the tap, then ticks the dose off: check, slice, count and sound. */
@Composable
fun TakeContent(outcome: TakeOutcome, today: Long, onUndo: (() -> Unit)?) {
    val c = P1ll.colors
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val pill = outcome.pill
    var played by remember { mutableStateOf(!outcome.newly) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        delay(280) // let the sheet arrive first
        played = true
        if (outcome.completes) feedback.complete() else feedback.taken(outcome.takenOnDay)
    }
    val cell = if (played) outcome.after else outcome.before
    val left = cell.slots.count { it == SlotStatus.PENDING }
    val day = if (outcome.after.day == today) "today" else "on ${Fmt.weekday(outcome.after.day)}"

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
            .padding(bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp)) {
                PillGlyph(pill.shape, pillColor(pill.color, c.dark), Modifier.matchParentSize())
                if (played) {
                    val pop = remember { Animatable(if (outcome.newly) 0f else 1f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, spring(0.45f, 480f)) }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(6.dp, 6.dp)
                            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(c.surface)
                            .padding(2.dp),
                    ) {
                        DoneMark(Modifier.matchParentSize())
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pill.name, style = Type.headline, maxLines = 1)
                val at = Instant.ofEpochMilli(outcome.at).atZone(ZoneId.systemDefault())
                Text("Taken ${Fmt.time(context, at)}", style = Type.small, color = c.ink2)
            }
            if (cell.slots.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                DayPie(cell, c.accent, Modifier.size(48.dp))
            }
        }
        if (cell.slots.isNotEmpty() || onUndo != null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (cell.slots.isNotEmpty()) {
                        AnimatedContent(left == 0, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "left") { done ->
                            if (done) {
                                Text("All done $day", style = Type.bodyStrong)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RollingNumber(left, Type.bodyStrong)
                                    Text(" left $day", style = Type.body, color = c.ink2)
                                }
                            }
                        }
                    }
                }
                if (onUndo != null) {
                    GhostButton("Undo", {
                        feedback.undo()
                        onUndo()
                    })
                }
            }
        }
    }
}
