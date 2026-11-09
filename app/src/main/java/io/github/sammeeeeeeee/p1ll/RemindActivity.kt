package io.github.sammeeeeeeee.p1ll

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.reminders.ActionReceiver
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.remind.RemindSheet
import io.github.sammeeeeeeee.p1ll.ui.theme.P1llTheme
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Remind me on a reminder: a sheet over whatever is on screen asks when, and the reminder goes away
 * until then. Translucent, so Android draws no splash screen first; its own task, so the app's
 * screens stay where they were. Not exported: only the reminder's PendingIntent can open it.
 */
class RemindActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pillId = intent.getLongExtra(ActionReceiver.EXTRA_PILL, -1)
        val day = intent.getLongExtra(ActionReceiver.EXTRA_DAY, -1)
        val minute = intent.getIntExtra(ActionReceiver.EXTRA_MINUTE, -1)
        if (pillId < 0 || day < 0 || minute < 0) return finish()
        val container = (application as P1llApp).container
        enableEdgeToEdge()
        setContent {
            // Wait for both, so the sheet opens in the user's theme instead of switching to it.
            val loaded = produceState<Pair<Prefs, Pill?>?>(null) {
                val prefs = container.settings.current()
                container.feedback.haptics = prefs.haptics
                value = prefs to container.db.pills().get(pillId)
            }.value ?: return@setContent
            val (prefs, pill) = loaded
            if (pill == null) {
                LaunchedEffect(Unit) { finish() }
                return@setContent
            }
            P1llTheme(prefs) {
                CompositionLocalProvider(LocalFeedback provides container.feedback) {
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
        }
    }

    private fun confirmation(at: ZonedDateTime): String {
        val time = Fmt.time(this, at)
        return if (at.toLocalDate() == ZonedDateTime.now(at.zone).toLocalDate()) "Reminder at $time" else "Reminder tomorrow at $time"
    }

    override fun finish() {
        super.finish()
        // The sheet has already slid away; the window just goes.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
