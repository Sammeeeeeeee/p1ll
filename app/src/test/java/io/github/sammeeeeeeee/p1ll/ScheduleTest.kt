package io.github.sammeeeeeeee.p1ll

import io.github.sammeeeeeeee.p1ll.data.Dose
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.ScheduleType
import io.github.sammeeeeeeee.p1ll.domain.DayCell
import io.github.sammeeeeeeee.p1ll.domain.Phase
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.buildCells
import io.github.sammeeeeeeee.p1ll.domain.buildToday
import io.github.sammeeeeeeee.p1ll.domain.indexDoses
import io.github.sammeeeeeeee.p1ll.domain.isActiveOn
import io.github.sammeeeeeeee.p1ll.domain.matchSlots
import io.github.sammeeeeeeee.p1ll.domain.nextDose
import io.github.sammeeeeeeee.p1ll.domain.pastDoses
import io.github.sammeeeeeeee.p1ll.domain.slotsOn
import io.github.sammeeeeeeee.p1ll.domain.stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val zone = ZoneId.of("Europe/London")
    private val monday = LocalDate.of(2026, 10, 19).toEpochDay()
    private val longAgo = ZonedDateTime.of(2026, 1, 1, 9, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun pill(
        id: Long = 1,
        times: List<Int> = listOf(8 * 60, 20 * 60),
        schedule: ScheduleType = ScheduleType.DAILY,
        weekdays: Int = Pill.ALL_WEEK,
        interval: Int = 2,
        start: Long = monday - 30,
        end: Long? = null,
        optional: Boolean = false,
        createdAt: Long = longAgo,
    ) = Pill(
        id = id, name = "P$id", times = times, schedule = schedule, weekdays = weekdays,
        intervalDays = interval, startDay = start, endDay = end, optional = optional, createdAt = createdAt,
    )

    private fun dose(day: Long, minute: Int, status: DoseStatus = DoseStatus.TAKEN, pillId: Long = 1, id: Long = minute.toLong()) =
        Dose(id = id, pillId = pillId, day = day, minute = minute, status = status, at = 0)

    private fun at(day: Long, h: Int, m: Int = 0) = LocalDate.ofEpochDay(day).atTime(h, m).atZone(zone)

    @Test
    fun weekdaysScheduleOnlyOnChosenDays() {
        val p = pill(schedule = ScheduleType.WEEKDAYS, weekdays = 0b0010101) // Mon, Wed, Fri
        assertTrue(p.isActiveOn(monday))
        assertFalse(p.isActiveOn(monday + 1))
        assertTrue(p.isActiveOn(monday + 2))
        assertTrue(p.isActiveOn(monday + 4))
        assertFalse(p.isActiveOn(monday + 6))
    }

    @Test
    fun intervalCountsFromStartDay() {
        val p = pill(schedule = ScheduleType.INTERVAL, interval = 3, start = monday)
        assertTrue(p.isActiveOn(monday))
        assertFalse(p.isActiveOn(monday + 1))
        assertTrue(p.isActiveOn(monday + 3))
        assertFalse(p.isActiveOn(monday - 3))
    }

    @Test
    fun endDayIsInclusive() {
        val p = pill(end = monday)
        assertTrue(p.isActiveOn(monday))
        assertFalse(p.isActiveOn(monday + 1))
    }

    @Test
    fun firstDayDropsSlotsLongPast() {
        val created = at(monday, 23, 0).toInstant().toEpochMilli()
        val p = pill(times = listOf(8 * 60, 14 * 60, 22 * 60), start = monday, createdAt = created)
        assertEquals(listOf(22 * 60), p.slotsOn(monday, zone))
        assertEquals(3, p.slotsOn(monday + 1, zone).size)
    }

    @Test
    fun matchingKeepsHistoryWhenTimesMove() {
        // Logged at 08:00 and 20:00; times later edited to 09:00 and 21:00.
        val slots = matchSlots(1, monday, listOf(9 * 60, 21 * 60), listOf(dose(monday, 8 * 60), dose(monday, 20 * 60)))
        assertTrue(slots.all { it.status == SlotStatus.TAKEN })
    }

    @Test
    fun exactMinuteWinsOverOrder() {
        val slots = matchSlots(1, monday, listOf(8 * 60, 20 * 60), listOf(dose(monday, 20 * 60)))
        assertEquals(SlotStatus.PENDING, slots[0].status)
        assertEquals(SlotStatus.TAKEN, slots[1].status)
    }

    @Test
    fun cellsExcludeOptionalAndAsNeeded() {
        val pills = listOf(
            pill(1),
            pill(2, optional = true),
            pill(3, schedule = ScheduleType.AS_NEEDED, times = emptyList()),
        )
        val cells = buildCells(pills, indexDoses(listOf(dose(monday, 8 * 60))), monday, monday, zone)
        assertEquals(listOf(SlotStatus.TAKEN, SlotStatus.PENDING), cells.single().slots)
    }

    @Test
    fun streakSkipsEmptyDaysAndCountsTodayOnlyWhenKept() {
        val t = SlotStatus.TAKEN
        val p = SlotStatus.PENDING
        val cells = listOf(
            DayCell(monday - 4, listOf(p)),
            DayCell(monday - 3, listOf(t, t)),
            DayCell(monday - 2, emptyList()),
            DayCell(monday - 1, listOf(t)),
            DayCell(monday, listOf(t, p)),
        )
        assertEquals(2, stats(cells, monday, 0).streak)
        val kept = cells.dropLast(1) + DayCell(monday, listOf(t, SlotStatus.SKIPPED))
        assertEquals(3, stats(kept, monday, 0).streak)
    }

    @Test
    fun nextDoseSkipsHandledSlots() {
        val p = pill()
        val now = at(monday, 7, 30)
        val taken = indexDoses(listOf(dose(monday, 8 * 60)))
        assertEquals(at(monday, 20), p.nextDose(now, taken))
        assertEquals(at(monday, 8), p.nextDose(now, emptyMap()))
    }

    @Test
    fun nextDoseStopsAtEnd() {
        val p = pill(end = monday)
        assertNull(p.nextDose(at(monday, 21), emptyMap()))
    }

    @Test
    fun todayPhases() {
        val now = at(monday, 12, 0)
        val pills = listOf(
            pill(1, times = listOf(8 * 60)),
            pill(2, times = listOf(18 * 60)),
            pill(3, times = listOf(9 * 60)),
            pill(4, schedule = ScheduleType.AS_NEEDED, times = emptyList()),
            pill(5, end = monday - 1),
            pill(6, schedule = ScheduleType.WEEKDAYS, weekdays = 0b0000010),
        )
        val index = indexDoses(listOf(dose(monday, 9 * 60, pillId = 3)))
        val phases = buildToday(pills, index, now).associate { it.pill.id to it.phase }
        assertEquals(Phase.DUE, phases[1])
        assertEquals(Phase.LATER, phases[2])
        assertEquals(Phase.DONE, phases[3])
        assertEquals(Phase.AS_NEEDED, phases[4])
        assertEquals(Phase.FINISHED, phases[5])
        assertEquals(Phase.NOT_TODAY, phases[6])
    }

    @Test
    fun backdatedPillCountsEverySlotOnTheDayItWasAdded() {
        val created = at(monday, 23, 0).toInstant().toEpochMilli()
        val p = pill(times = listOf(8 * 60, 20 * 60), start = monday - 3, createdAt = created)
        assertEquals(listOf(8 * 60, 20 * 60), p.slotsOn(monday, zone))
    }

    @Test
    fun pastDosesFillUpToNowAndSkipLogged() {
        val p = pill(times = listOf(8 * 60, 20 * 60), start = monday - 3)
        val doses = p.pastDoses(at(monday, 12, 0), setOf((monday - 1) to 20 * 60))
        // Three full days less one already logged, plus this morning.
        assertEquals(6, doses.size)
        assertTrue(doses.all { it.status == DoseStatus.TAKEN })
        assertEquals(at(monday, 8).toInstant().toEpochMilli(), doses.last().at)
    }
}
