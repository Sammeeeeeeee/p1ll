package io.github.sammeeeeeeee.p1ll

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.reminders.Reminders
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.remind.TAKE_SHEET_MS
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
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
class TakeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = RuntimeEnvironment.getApplication() as P1llApp
    private val zone = ZoneId.systemDefault()
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val now = ZonedDateTime.now(zone)
    private val today = now.toLocalDate().toEpochDay()
    private val minute = (now.hour * 60 + now.minute - 1).coerceAtLeast(0)

    private val Notification.title get() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()

    /** Two pills due a minute ago, both reminded; returns the first one's id and its Take intent. */
    private fun remind(): Pair<Long, Intent> = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val created = now.minusDays(10).toInstant().toEpochMilli()
        val id = app.container.db.pills().insert(Pill(name = "Sertraline", times = listOf(minute), startDay = today - 10, createdAt = created))
        app.container.db.pills().insert(Pill(name = "Vitamin D", times = listOf(minute), startDay = today - 10, createdAt = created))
        val slot = now.withHour(minute / 60).withMinute(minute % 60).withSecond(0).withNano(0)
        Reminders.sync(app, slot.minusMinutes(1))
        Reminders.sync(app, now)
        val take = notifications.allNotifications.single { it.title == "Sertraline" }.actions
            .first { it.title == app.getString(R.string.notif_taken) }
        id to Intent(shadowOf(take.actionIntent).savedIntent)
    }

    private fun status(pillId: Long) = runBlocking {
        app.container.db.doses().forPillDay(pillId, today).singleOrNull()?.status
    }

    @Test
    fun takeCountsAtOnceEvenIfTheSheetIsClosedStraightAway() {
        val (pillId, intent) = remind()
        ActivityScenario.launch<TakeActivity>(intent).use { scenario ->
            // Closed before the sheet has shown anything.
            scenario.onActivity { it.finish() }
            waitFor("the dose to be stored") { status(pillId) == DoseStatus.TAKEN }
            waitFor("its reminder to go") { notifications.allNotifications.none { it.title == "Sertraline" } }
            assertEquals(listOf("Vitamin D"), notifications.allNotifications.map { it.title })
        }
    }

    @Test
    fun theSheetPlaysTheTakeOutThenGoesByItself() {
        val (pillId, intent) = remind()
        ActivityScenario.launch<TakeActivity>(intent).use { scenario ->
            waitFor("the sheet") { compose.onAllNodesWithText("Sertraline").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(DoseStatus.TAKEN, status(pillId))
            val at = runBlocking { app.container.db.doses().forPillDay(pillId, today).single().at }
            compose.onNodeWithText("Taken ${Fmt.time(app, Instant.ofEpochMilli(at).atZone(zone))}").assertExists()
            // Two were left; once the take plays, one is.
            waitFor("the count to drop") { compose.onAllNodesWithText(" left today").fetchSemanticsNodes().isNotEmpty() && compose.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty() }
            // The test clock only moves on its own while something animates, so the wait is stepped through.
            compose.mainClock.advanceTimeBy(TAKE_SHEET_MS - 800)
            scenario.onActivity { assertFalse(it.isFinishing) }
            compose.mainClock.advanceTimeBy(800)
            waitFor("the sheet to go by itself") {
                var finishing = false
                scenario.onActivity { finishing = it.isFinishing }
                finishing
            }
            assertEquals(DoseStatus.TAKEN, status(pillId))
        }
    }

    @Test
    fun undoPutsTheReminderBackQuietly() {
        val (pillId, intent) = remind()
        ActivityScenario.launch<TakeActivity>(intent).use { scenario ->
            waitFor("the sheet") { compose.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Undo").performClick()
            waitFor("the dose to be cleared") { status(pillId) == null }
            waitFor("the reminder to come back") { notifications.allNotifications.any { it.title == "Sertraline" } }
            val back = notifications.allNotifications.single { it.title == "Sertraline" }
            assertEquals(Notifications.CHANNEL_DOSES, back.channelId)
            assertTrue(back.flags and Notification.FLAG_ONGOING_EVENT != 0)
            // A group child that leaves alerting to its summary: back in the shade without a sound.
            assertEquals(Notification.GROUP_ALERT_SUMMARY, back.groupAlertBehavior)
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
