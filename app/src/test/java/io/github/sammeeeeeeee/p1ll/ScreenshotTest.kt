package io.github.sammeeeeeeee.p1ll

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sammeeeeeeee.p1ll.data.Dose
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.PillShape
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.data.ScheduleType
import io.github.sammeeeeeeee.p1ll.data.ThemeMode
import io.github.sammeeeeeeee.p1ll.domain.indexDoses
import io.github.sammeeeeeeee.p1ll.domain.slotsOn
import io.github.sammeeeeeeee.p1ll.ui.Snapshot
import io.github.sammeeeeeeee.p1ll.ui.buildHome
import io.github.sammeeeeeeee.p1ll.ui.components.rememberChartWave
import io.github.sammeeeeeeee.p1ll.ui.detail.DetailActions
import io.github.sammeeeeeeee.p1ll.ui.detail.DetailScreen
import io.github.sammeeeeeeee.p1ll.ui.editor.EditorScreen
import io.github.sammeeeeeeee.p1ll.ui.home.HomeActions
import io.github.sammeeeeeeee.p1ll.ui.home.HomeScreen
import io.github.sammeeeeeeee.p1ll.ui.settings.SettingsScreen
import io.github.sammeeeeeeee.p1ll.ui.theme.P1llTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/** Renders each screen with believable data into docs/screenshots. Run: ./gradlew recordRoborazziDebug */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rGB-w411dp-h1600dp-xxhdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val zone = ZoneId.of("Europe/London")
    private val now = ZonedDateTime.of(2026, 10, 21, 15, 10, 0, 0, zone)
    private val today = now.toLocalDate().toEpochDay()

    private fun snapshot(): Snapshot {
        val created = now.minusDays(400).toInstant().toEpochMilli()
        val pills = listOf(
            Pill(1, "Sertraline", "50 mg", color = 5, shape = PillShape.OVAL, times = listOf(480, 1200), startDay = today - 300, createdAt = created),
            Pill(2, "Metformin", "500 mg", color = 4, shape = PillShape.CAPSULE, times = listOf(480, 780, 1140), startDay = today - 120, stock = 6, refillAt = 10, createdAt = created),
            Pill(3, "Vitamin D", "1000 IU", color = 2, shape = PillShape.ROUND, times = listOf(540), optional = true, startDay = today - 300, createdAt = created),
            Pill(4, "Ibuprofen", "200 mg", color = 0, shape = PillShape.TABLET, schedule = ScheduleType.AS_NEEDED, times = emptyList(), startDay = today - 300, createdAt = created),
            Pill(5, "Amoxicillin", "250 mg", color = 3, shape = PillShape.CAPSULE, times = listOf(480, 1320), startDay = today - 3, endDay = today + 4, createdAt = created),
            Pill(6, "Iron", "", color = 7, shape = PillShape.DROP, schedule = ScheduleType.WEEKDAYS, weekdays = 0b0101010, times = listOf(600), startDay = today - 60, createdAt = created),
        )
        val random = Random(4)
        var id = 1L
        val doses = mutableListOf<Dose>()
        for (day in today - 300 until today) {
            val streakZone = day > today - 12
            for (p in pills) for (m in p.slotsOn(day, zone)) {
                val chance = if (streakZone) 1.0 else 0.86
                if (random.nextDouble() < chance) doses += Dose(id++, p.id, day, m, DoseStatus.TAKEN, 0)
            }
            if (random.nextDouble() < 0.12) doses += Dose(id++, 4, day, 700, DoseStatus.TAKEN, 0)
        }
        val atToday = now.withHour(9).toInstant().toEpochMilli()
        doses += Dose(id++, 1, today, 480, DoseStatus.TAKEN, atToday)
        doses += Dose(id++, 2, today, 480, DoseStatus.TAKEN, atToday)
        doses += Dose(id++, 3, today, 540, DoseStatus.TAKEN, atToday)
        doses += Dose(id++, 5, today, 480, DoseStatus.TAKEN, atToday)
        doses += Dose(id++, 4, today, 680, DoseStatus.TAKEN, now.withHour(11).withMinute(20).toInstant().toEpochMilli())
        return Snapshot(pills, doses, indexDoses(doses), now, loaded = true)
    }

    private val noHome = HomeActions({ _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {}, {}, {})

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            P1llTheme(Prefs(theme = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)) { content() }
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    @Test
    fun homeLight() = shot("home-light") {
        HomeScreen(buildHome(snapshot()), null, rememberChartWave(), noHome)
    }

    @Test
    fun homeDark() = shot("home-dark", dark = true) {
        HomeScreen(buildHome(snapshot()), null, rememberChartWave(), noHome)
    }

    @Test
    fun homeEmpty() = shot("home-empty") {
        HomeScreen(buildHome(Snapshot(emptyList(), emptyList(), emptyMap(), now, loaded = true)), null, rememberChartWave(), noHome)
    }

    @Test
    fun detail() = shot("detail-light") {
        DetailScreen(2, snapshot(), DetailActions({}, {}, {}, {}, { _, _ -> }, {}))
    }

    @Test
    fun detailDark() = shot("detail-dark", dark = true) {
        DetailScreen(1, snapshot(), DetailActions({}, {}, {}, {}, { _, _ -> }, {}))
    }

    @Test
    fun editor() = shot("editor-light") {
        val s = snapshot()
        EditorScreen(s.pills[1], today, emptySet(), fromFab = false, onClose = {}, onSave = { _, _ -> })
    }

    @Test
    fun settings() = shot("settings-dark", dark = true) {
        SettingsScreen(Prefs(theme = ThemeMode.DARK), {}, {})
    }
}
