package io.github.sammeeeeeeee.p1ll

import android.app.Application
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.data.Dose
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.indexDoses
import io.github.sammeeeeeeee.p1ll.domain.slotTime
import io.github.sammeeeeeeee.p1ll.ui.Snapshot
import io.github.sammeeeeeeee.p1ll.ui.buildHome
import io.github.sammeeeeeeee.p1ll.ui.components.rememberChartWave
import io.github.sammeeeeeeee.p1ll.ui.home.HomeActions
import io.github.sammeeeeeeee.p1ll.ui.home.HomeScreen
import io.github.sammeeeeeeee.p1ll.ui.nav.NavHost
import io.github.sammeeeeeeee.p1ll.ui.nav.Navigator
import io.github.sammeeeeeeee.p1ll.ui.nav.Screen
import io.github.sammeeeeeeee.p1ll.ui.nav.rememberNavigator
import io.github.sammeeeeeeee.p1ll.ui.theme.P1llTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h1600dp", application = Application::class)
class HomeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeKeepsItsPlaceUnderAnotherScreen() {
        lateinit var nav: Navigator
        lateinit var list: LazyListState
        compose.setContent {
            nav = rememberNavigator()
            NavHost(nav) { screen, _ ->
                if (screen == Screen.Home) {
                    list = rememberLazyListState()
                    LazyColumn(Modifier.testTag("list"), state = list) {
                        items(100) { Text("Row $it", Modifier.height(48.dp)) }
                    }
                } else {
                    Text("Detail")
                }
            }
        }
        compose.onNodeWithTag("list").performScrollToIndex(40)
        compose.runOnIdle { nav.push(Screen.Detail(1)) }
        compose.onNodeWithText("Detail").assertExists()
        compose.runOnIdle { nav.pop() }
        compose.runOnIdle { assertEquals(40, list.firstVisibleItemIndex) }
    }

    @Test
    fun anEarlierDayCanBeChanged() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val yesterday = now.toLocalDate().toEpochDay() - 1
        val pill = Pill(1, "Sertraline", "50 mg", times = listOf(480), startDay = yesterday - 10, createdAt = now.minusDays(11).toInstant().toEpochMilli())
        val doses = listOf(Dose(1, 1, yesterday, 480, DoseStatus.TAKEN, slotTime(yesterday, 480, zone).toInstant().toEpochMilli()))
        val snapshot = Snapshot(listOf(pill), doses, indexDoses(doses), now, loaded = true)
        var skipped: Slot? = null
        var edited: Long? = null
        val actions = HomeActions(
            take = { _, _ -> }, skip = { _, slot -> skipped = slot }, untake = {}, log = {}, open = {},
            editDay = { edited = it }, add = {}, settings = {}, showDay = {}, logAt = { _, _ -> }, undo = {},
        )
        compose.setContent {
            P1llTheme(Prefs()) { HomeScreen(buildHome(snapshot, yesterday), null, rememberChartWave(), actions) }
        }

        // A dose already ticked off can still be swiped to skipped...
        compose.onNodeWithText("Sertraline").performTouchInput { swipe(centerLeft, Offset(centerLeft.x - 600f, centerLeft.y)) }
        compose.runOnIdle { assertEquals(SlotStatus.TAKEN, skipped?.status) }
        // ...and tapping it opens that day for editing rather than the pill.
        compose.onNodeWithText("Sertraline").performClick()
        compose.runOnIdle { assertEquals(yesterday, edited) }
    }
}
