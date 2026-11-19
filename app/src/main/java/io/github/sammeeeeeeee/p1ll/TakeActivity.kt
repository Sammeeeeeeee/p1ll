package io.github.sammeeeeeeee.p1ll

import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.data.TakeOutcome
import io.github.sammeeeeeeee.p1ll.ui.remind.TakeSheet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Take on a reminder: the dose is recorded at once, then the sheet plays it out like the app does. */
class TakeActivity : ReminderSheetActivity() {
    override fun onDose(pillId: Long, day: Long, minute: Int) {
        container.feedback // start loading the sounds while the dose is stored
        val taken = container.scope.async { container.store.takeFromReminder(pillId, day, minute) }
        setContent {
            val loaded = produceState<Pair<Prefs, TakeOutcome?>?>(null) {
                value = prefs() to taken.await()
            }.value ?: return@setContent
            val (prefs, outcome) = loaded
            if (outcome == null) {
                LaunchedEffect(Unit) { finish() }
                return@setContent
            }
            Themed(prefs) {
                TakeSheet(
                    outcome = outcome,
                    today = ZonedDateTime.now().toLocalDate().toEpochDay(),
                    onUndo = { container.scope.launch { container.store.untake(outcome.doseId) } },
                    onDismiss = ::finish,
                )
            }
        }
    }
}
