package io.github.sammeeeeeeee.p1ll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.reminders.ActionReceiver
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.P1llTheme

/**
 * A sheet over whatever is on screen, opened by one of a reminder's actions. Translucent, so Android
 * draws no splash screen first; its own task, so the app's screens stay where they were. Not
 * exported: only the reminder's PendingIntent can open it.
 */
abstract class ReminderSheetActivity : ComponentActivity() {
    protected val container: Container get() = (application as P1llApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pillId = intent.getLongExtra(ActionReceiver.EXTRA_PILL, -1)
        val day = intent.getLongExtra(ActionReceiver.EXTRA_DAY, -1)
        val minute = intent.getIntExtra(ActionReceiver.EXTRA_MINUTE, -1)
        if (pillId < 0 || day < 0 || minute < 0) return finish()
        enableEdgeToEdge()
        onDose(pillId, day, minute)
    }

    protected abstract fun onDose(pillId: Long, day: Long, minute: Int)

    /** Settings, with sound and touch feedback set to match before anything plays. */
    protected suspend fun prefs(): Prefs = container.settings.current().also {
        container.feedback.sounds = it.sounds
        container.feedback.haptics = it.haptics
    }

    @Composable
    protected fun Themed(prefs: Prefs, content: @Composable () -> Unit) = P1llTheme(prefs) {
        CompositionLocalProvider(LocalFeedback provides container.feedback, content = content)
    }

    override fun finish() {
        super.finish()
        // The sheet has already slid away; the window just goes.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
