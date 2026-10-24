package io.github.sammeeeeeeee.p1ll

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.reminders.ActionReceiver
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.reminders.Reminders
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReminderFlowTest {
    private val app = RuntimeEnvironment.getApplication() as P1llApp
    private val zone = ZoneId.systemDefault()
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(600) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("Timed out waiting for $what")
    }

    @Test
    fun snoozeClearsTheReminderAndBringsItBack() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val now = ZonedDateTime.now(zone)
        val today = now.toLocalDate().toEpochDay()
        val minute = (now.hour * 60 + now.minute - 1).coerceAtLeast(0)
        val db = app.container.db
        val pillId = db.pills().insert(
            Pill(name = "Sertraline", dosage = "50 mg", times = listOf(minute), startDay = today - 10, createdAt = now.minusDays(10).toInstant().toEpochMilli()),
        )

        // The first sync only records when it ran; the second posts what fell due in between.
        val slot = now.withHour(minute / 60).withMinute(minute % 60).withSecond(0).withNano(0)
        Reminders.sync(app, slot.minusMinutes(1))
        Reminders.sync(app, now)
        assertEquals(1, notifications.allNotifications.size)
        val posted = notifications.allNotifications.single()
        assertEquals("50 mg, due ${Fmt.time(app, minute)}", posted.extras.getString(Notification.EXTRA_TEXT))

        // Take opens the app (which records the dose in view) instead of acting in the background.
        val take = shadowOf(posted.actions.first { it.title == app.getString(R.string.notif_taken) }.actionIntent)
        assertTrue(take.isActivity)
        assertEquals(MainActivity.ACTION_TAKE, take.savedIntent.action)
        assertEquals(pillId, take.savedIntent.getLongExtra(ActionReceiver.EXTRA_PILL, -1))
        // MainActivity is exported, so it only honours a Take that carries this install's token.
        assertTrue(Notifications.isOwnTake(app, take.savedIntent))
        assertFalse(Notifications.isOwnTake(app, Intent(take.savedIntent).apply { removeExtra("token") }))

        // Robolectric can't route an explicit intent to a manifest receiver under the .debug app id,
        // so the receiver is registered by hand and gets the same intent the button would send.
        val snoozeIntent = shadowOf(posted.actions.first { it.title == app.getString(R.string.notif_snooze) }.actionIntent).savedIntent
        app.registerReceiver(ActionReceiver(), IntentFilter(snoozeIntent.action).apply { addDataScheme("p1ll") }, Context.RECEIVER_NOT_EXPORTED)
        app.sendBroadcast(Intent(snoozeIntent).setComponent(null))
        waitFor("the reminder to clear") { notifications.allNotifications.isEmpty() }

        // The receiver clears the reminder first and re-arms afterwards, so wait for the snooze alarm
        // itself rather than reading whatever was armed before (tomorrow's dose).
        val snooze = app.container.settings.current().snoozeMinutes * 60_000L
        fun snoozeAlarm() = alarms.peekNextScheduledAlarm()?.takeIf {
            kotlin.math.abs(it.triggerAtMs - System.currentTimeMillis() - snooze) < 60_000
        }
        waitFor("the snooze alarm") { snoozeAlarm() != null }
        val alarm = snoozeAlarm()
        assertNotNull(alarm)

        // The snoozed reminder comes back as a fresh post stamped with when it returned.
        val back = Instant.ofEpochMilli(alarm!!.triggerAtMs + 1_000)
        Reminders.sync(app, back.atZone(zone))
        assertEquals(1, notifications.allNotifications.size)
        assertTrue(notifications.allNotifications.single().`when` >= alarm.triggerAtMs - 5_000)
    }
}
