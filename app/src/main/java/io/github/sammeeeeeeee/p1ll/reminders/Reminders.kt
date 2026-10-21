package io.github.sammeeeeeeee.p1ll.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import io.github.sammeeeeeeee.p1ll.P1llApp
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.handledLookup
import io.github.sammeeeeeeee.p1ll.domain.indexDoses
import io.github.sammeeeeeeee.p1ll.domain.nextOccurrence
import io.github.sammeeeeeeee.p1ll.domain.plan
import io.github.sammeeeeeeee.p1ll.domain.slotTime
import io.github.sammeeeeeeee.p1ll.domain.slotsOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

/**
 * One exact alarm at a time: when it fires, everything due is posted together and the next
 * alarm is armed. Grouping keeps simultaneous doses out of Doze's per-app alarm throttling
 * and lets a late wake-up (reboot, dead battery) catch up on what it missed.
 */
object Reminders {
    private const val PREFS = "reminders"
    private const val MAX_NAGS = 6
    private const val CATCH_UP_MS = 3 * 60 * 60 * 1000L
    private val lock = Mutex()

    private data class FollowUp(
        val pillId: Long,
        val day: Long,
        val minute: Int,
        val dueAt: Long,
        val attempt: Int,
        val snoozed: Boolean,
    ) {
        fun encode() = "$pillId|$day|$minute|$dueAt|$attempt|${if (snoozed) 1 else 0}"

        companion object {
            fun parse(raw: String): FollowUp? = runCatching {
                val p = raw.split('|')
                FollowUp(p[0].toLong(), p[1].toLong(), p[2].toInt(), p[3].toLong(), p[4].toInt(), p[5] == "1")
            }.getOrNull()
        }
    }

    suspend fun snooze(context: Context, pillId: Long, day: Long, minute: Int, minutes: Int) {
        Notifications.cancel(context, pillId, minute)
        lock.withLock {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val follow = readFollowUps(prefs).filterNot { it.pillId == pillId && it.day == day && it.minute == minute }
            val dueAt = System.currentTimeMillis() + minutes * 60_000L
            val next = follow + FollowUp(pillId, day, minute, dueAt, 0, snoozed = true)
            prefs.edit { putStringSet("follow", next.map { it.encode() }.toSet()) }
        }
        sync(context)
    }

    /** Posts anything that has come due, then arms one alarm for whatever is next. Safe to call any time. */
    suspend fun sync(context: Context, now: ZonedDateTime = ZonedDateTime.now()) = lock.withLock {
        val container = (context.applicationContext as P1llApp).container
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = container.settings.current()
        val zone = now.zone
        val nowMs = now.toInstant().toEpochMilli()
        val today = now.toLocalDate().toEpochDay()
        val nag = settings.nagMinutes * 60_000L

        val pills = container.db.pills().all()
        val index = indexDoses(container.db.doses().between(today - 1, today + 1))
        val since = maxOf(prefs.getLong("lastRun", nowMs), nowMs - CATCH_UP_MS)
        val notified = prefs.getStringSet("notified", emptySet())!!.toMutableSet()
        val follow = readFollowUps(prefs).toMutableList()

        fun pending(pillId: Long, day: Long, minute: Int): Boolean {
            val pill = pills.find { it.id == pillId } ?: return false
            return pill.plan(day, zone, index).any { it.minute == minute && it.status == SlotStatus.PENDING }
        }

        for (pill in pills) for (day in today - 1..today) for (minute in pill.slotsOn(day, zone)) {
            val at = slotTime(day, minute, zone).toInstant().toEpochMilli()
            val key = "$day|${pill.id}|$minute"
            if (at > nowMs || at <= since || key in notified || !pending(pill.id, day, minute)) continue
            Notifications.dose(context, pill, day, minute, settings.snoozeMinutes, Notifications.Kind.DUE)
            notified += key
            if (nag > 0) follow += FollowUp(pill.id, day, minute, nowMs + nag, 1, snoozed = false)
        }

        val fired = follow.filter { it.dueAt <= nowMs }
        follow.removeAll(fired)
        for (f in fired) {
            if (!pending(f.pillId, f.day, f.minute)) continue
            val pill = pills.first { it.id == f.pillId }
            val kind = if (f.snoozed) Notifications.Kind.SNOOZED else Notifications.Kind.NAG
            Notifications.dose(context, pill, f.day, f.minute, settings.snoozeMinutes, kind)
            if (nag > 0 && f.attempt < MAX_NAGS) {
                follow += f.copy(dueAt = nowMs + nag, attempt = f.attempt + 1, snoozed = false)
            }
        }
        follow.removeAll { it.day < today - 1 || !pending(it.pillId, it.day, it.minute) }
        notified.removeAll { (it.substringBefore('|').toLongOrNull() ?: 0) < today - 1 }

        var next = follow.minOfOrNull { it.dueAt }
        for (pill in pills) {
            if (pill.asNeeded) continue
            val handled = handledLookup(pill, zone, index)
            for (minute in pill.times) {
                val at = pill.nextOccurrence(minute, now, handled)?.toInstant()?.toEpochMilli() ?: continue
                if (next == null || at < next) next = at
            }
        }
        arm(context, next)

        prefs.edit {
            putLong("lastRun", nowMs)
            putStringSet("notified", notified)
            putStringSet("follow", follow.map { it.encode() }.toSet())
        }
    }

    private fun readFollowUps(prefs: android.content.SharedPreferences) =
        prefs.getStringSet("follow", emptySet())!!.mapNotNull(FollowUp::parse)

    private fun arm(context: Context, at: Long?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (at == null) {
            alarms.cancel(intent)
            return
        }
        if (canExact(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    fun canExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}
