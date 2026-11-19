package io.github.sammeeeeeeee.p1ll

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.reminders.Reminders
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.remind.nextAt
import io.github.sammeeeeeeee.p1ll.ui.remind.presetLabel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemindTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = RuntimeEnvironment.getApplication() as P1llApp
    private val zone = ZoneId.systemDefault()

    @Test
    fun aPickedTimeIsTheNextOneOnTheClock() {
        val now = ZonedDateTime.of(2026, 11, 9, 14, 0, 0, 0, zone)
        assertEquals(now.withHour(15).withMinute(30), nextAt(now, 15 * 60 + 30))
        assertEquals(now.plusDays(1).withHour(9), nextAt(now, 9 * 60))
        assertEquals(now.plusDays(1), nextAt(now, 14 * 60))
        assertEquals(listOf("+15 min", "+30 min", "+1 h", "+2 h", "+4 h"), listOf(15, 30, 60, 120, 240).map(::presetLabel))
    }

    @Test
    fun aPresetPutsTheReminderOffAndCloses() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifications = shadowOf(app.getSystemService(NotificationManager::class.java))
        val now = ZonedDateTime.now(zone)
        val today = now.toLocalDate().toEpochDay()
        val minute = (now.hour * 60 + now.minute - 1).coerceAtLeast(0)
        val pillId = app.container.db.pills().insert(
            Pill(name = "Sertraline", times = listOf(minute), startDay = today - 10, createdAt = now.minusDays(10).toInstant().toEpochMilli()),
        )
        val slot = now.withHour(minute / 60).withMinute(minute % 60).withSecond(0).withNano(0)
        Reminders.sync(app, slot.minusMinutes(1))
        Reminders.sync(app, now)
        val remind = notifications.allNotifications.single().actions.first { it.title == app.getString(R.string.notif_remind) }

        ActivityScenario.launch<RemindActivity>(Intent(shadowOf(remind.actionIntent).savedIntent)).use { scenario ->
            // The sheet waits for the pill and settings to load, so it opens in the user's theme.
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Sertraline").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Due ${Fmt.time(app, minute)}").assertExists()
            val before = System.currentTimeMillis()
            compose.onNodeWithText("+1 h").performClick()

            waitFor("the reminder to clear") { notifications.allNotifications.isEmpty() }
            val armed = shadowOf(app.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()!!.triggerAtMs
            assertTrue(armed - before in 3_600_000L..3_600_000L + 60_000L)
            val at = Instant.ofEpochMilli(armed).atZone(zone)
            val expected = if (at.toLocalDate() == now.toLocalDate()) "Reminder at ${Fmt.time(app, at)}" else "Reminder tomorrow at ${Fmt.time(app, at)}"
            assertEquals(expected, ShadowToast.getTextOfLatestToast())
            // The sheet slides away, then the activity closes. (Robolectric leaves a finished activity
            // resumed rather than destroying it, so finishing is what's checked.)
            waitFor("the sheet to close") {
                var finishing = false
                scenario.onActivity { finishing = it.isFinishing }
                finishing
            }
        }
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(600) {
            compose.waitForIdle()
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("Timed out waiting for $what")
    }
}
