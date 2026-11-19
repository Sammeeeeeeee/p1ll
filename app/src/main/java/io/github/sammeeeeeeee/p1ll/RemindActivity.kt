package io.github.sammeeeeeeee.p1ll

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.remind.RemindSheet
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Remind me on a reminder: the sheet asks when, and the reminder goes away until then. */
class RemindActivity : ReminderSheetActivity() {
    override fun onDose(pillId: Long, day: Long, minute: Int) = setContent {
        // Wait for both, so the sheet opens in the user's theme instead of switching to it.
        val loaded = produceState<Pair<Prefs, Pill?>?>(null) {
            value = prefs() to container.db.pills().get(pillId)
        }.value ?: return@setContent
        val (prefs, pill) = loaded
        if (pill == null) {
            LaunchedEffect(Unit) { finish() }
            return@setContent
        }
        Themed(prefs) {
            RemindSheet(
                pill = pill,
                minute = minute,
                now = { ZonedDateTime.now() },
                onPick = { at ->
                    container.scope.launch { container.store.remindAt(pillId, day, minute, at.toInstant().toEpochMilli()) }
                    Toast.makeText(applicationContext, confirmation(at), Toast.LENGTH_SHORT).show()
                },
                onDismiss = ::finish,
            )
        }
    }

    private fun confirmation(at: ZonedDateTime): String {
        val time = Fmt.time(this, at)
        return if (at.toLocalDate() == ZonedDateTime.now(at.zone).toLocalDate()) "Reminder at $time" else "Reminder tomorrow at $time"
    }
}
