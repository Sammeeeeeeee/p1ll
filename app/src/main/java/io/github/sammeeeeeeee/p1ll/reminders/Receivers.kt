package io.github.sammeeeeeeee.p1ll.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sammeeeeeeee.p1ll.P1llApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun BroadcastReceiver.work(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** The single reminder alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = work { Reminders.sync(context) }

    companion object {
        const val ACTION = "io.github.sammeeeeeeee.p1ll.ALARM"
    }
}

/** Snooze on a reminder. Take now opens the app; this still handles it for reminders posted before that. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pillId = intent.getLongExtra(EXTRA_PILL, -1)
        val day = intent.getLongExtra(EXTRA_DAY, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        if (pillId < 0 || minute < 0) return
        val store = (context.applicationContext as P1llApp).container.store
        work {
            when (intent.action) {
                ACTION_TAKE -> store.takeAt(pillId, day, minute)
                ACTION_SNOOZE -> store.snooze(pillId, day, minute)
            }
        }
    }

    companion object {
        const val ACTION_TAKE = "io.github.sammeeeeeeee.p1ll.TAKE"
        const val ACTION_SNOOZE = "io.github.sammeeeeeeee.p1ll.SNOOZE"
        const val EXTRA_PILL = "pill"
        const val EXTRA_DAY = "day"
        const val EXTRA_MINUTE = "minute"
    }
}

/** Reboots, app updates, clock and time-zone changes all invalidate the armed alarm. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        work { Reminders.sync(context) }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
