package io.github.sammeeeeeeee.p1ll.domain

import io.github.sammeeeeeeee.p1ll.data.Dose
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.ScheduleType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Slots this long before a pill was created still count on its first day: you may be about to take it. */
const val CREATION_GRACE_MIN = 90

enum class SlotStatus { PENDING, TAKEN, SKIPPED }

data class Slot(
    val pillId: Long,
    val day: Long,
    val minute: Int,
    val status: SlotStatus,
    val doseId: Long? = null,
    val at: Long? = null,
)

/** pillId → epoch day → doses logged that day. */
typealias DoseIndex = Map<Long, Map<Long, List<Dose>>>

fun indexDoses(doses: List<Dose>): DoseIndex =
    doses.groupBy { it.pillId }.mapValues { (_, list) -> list.groupBy { it.day } }

fun DoseIndex.on(pillId: Long, day: Long): List<Dose> = this[pillId]?.get(day).orEmpty()

/** Monday = 0 … Sunday = 6. */
fun weekdayIndex(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value - 1

fun Pill.isActiveOn(day: Long): Boolean {
    if (day < startDay) return false
    if (endDay != null && day > endDay) return false
    return when (schedule) {
        ScheduleType.DAILY, ScheduleType.AS_NEEDED -> true
        ScheduleType.WEEKDAYS -> weekdays and (1 shl weekdayIndex(day)) != 0
        ScheduleType.INTERVAL -> (day - startDay) % intervalDays.coerceAtLeast(1) == 0L
    }
}

/**
 * Scheduled minutes for [day]. A pill that starts on the day it was added skips slots that had
 * long passed by then; a backdated pill counts every slot.
 */
fun Pill.slotsOn(day: Long, zone: ZoneId): List<Int> {
    if (asNeeded || !isActiveOn(day)) return emptyList()
    val created = Instant.ofEpochMilli(createdAt).atZone(zone)
    val createdDay = created.toLocalDate().toEpochDay()
    if (createdDay == day && startDay >= createdDay) {
        val cutoff = created.hour * 60 + created.minute - CREATION_GRACE_MIN
        return times.filter { it >= cutoff }
    }
    return times
}

/**
 * Pairs logged doses with scheduled slots: exact minute first, then in order. Doses keep their
 * original minute when reminder times are edited, so this keeps old history readable.
 */
fun matchSlots(pillId: Long, day: Long, minutes: List<Int>, doses: List<Dose>): List<Slot> {
    val remaining = doses.sortedBy { it.minute }.toMutableList()
    val matched = arrayOfNulls<Dose>(minutes.size)
    minutes.forEachIndexed { i, m ->
        remaining.firstOrNull { it.minute == m }?.let { matched[i] = it; remaining.remove(it) }
    }
    for (i in minutes.indices) {
        if (matched[i] == null && remaining.isNotEmpty()) matched[i] = remaining.removeAt(0)
    }
    return minutes.mapIndexed { i, m ->
        val d = matched[i]
        if (d == null) Slot(pillId, day, m, SlotStatus.PENDING)
        else Slot(pillId, day, m, d.status.toSlot(), d.id, d.at)
    }
}

fun DoseStatus.toSlot() = when (this) {
    DoseStatus.TAKEN -> SlotStatus.TAKEN
    DoseStatus.SKIPPED -> SlotStatus.SKIPPED
}

fun Pill.plan(day: Long, zone: ZoneId, index: DoseIndex): List<Slot> =
    matchSlots(id, day, slotsOn(day, zone), index.on(id, day))

fun slotTime(day: Long, minute: Int, zone: ZoneId): ZonedDateTime =
    LocalDate.ofEpochDay(day).atTime(minute / 60, minute % 60).atZone(zone)

/** The next time [minute] is due for this pill after [now] that hasn't been handled already. */
fun Pill.nextOccurrence(
    minute: Int,
    now: ZonedDateTime,
    handled: (day: Long, minute: Int) -> Boolean,
): ZonedDateTime? {
    val zone = now.zone
    val today = now.toLocalDate().toEpochDay()
    for (day in today..today + 400) {
        if (endDay != null && day > endDay) return null
        if (minute !in slotsOn(day, zone)) continue
        val t = slotTime(day, minute, zone)
        if (!t.isAfter(now) || handled(day, minute)) continue
        return t
    }
    return null
}

/** Every dose from [Pill.startDay] up to [now] as taken at its scheduled time, skipping [logged] (day to minute). */
fun Pill.pastDoses(now: ZonedDateTime, logged: Set<Pair<Long, Int>>): List<Dose> {
    val zone = now.zone
    return (startDay..now.toLocalDate().toEpochDay()).flatMap { day ->
        slotsOn(day, zone)
            .filter { (day to it) !in logged && slotTime(day, it, zone).isBefore(now) }
            .map { Dose(pillId = id, day = day, minute = it, status = DoseStatus.TAKEN, at = slotTime(day, it, zone).toInstant().toEpochMilli()) }
    }
}

/** The next unhandled dose of this pill after [now], across all its slots. */
fun Pill.nextDose(now: ZonedDateTime, index: DoseIndex): ZonedDateTime? {
    if (asNeeded) return null
    val handled = handledLookup(this, now.zone, index)
    return times.mapNotNull { nextOccurrence(it, now, handled) }.minOrNull()
}

fun handledLookup(pill: Pill, zone: ZoneId, index: DoseIndex): (Long, Int) -> Boolean = { day, minute ->
    pill.plan(day, zone, index).any { it.minute == minute && it.status != SlotStatus.PENDING }
}

// ── Chart ────────────────────────────────────────────────────────────────────────────────────

/** One square of the history chart: the day's required doses in time order. */
data class DayCell(val day: Long, val slots: List<SlotStatus>) {
    val complete get() = slots.isNotEmpty() && slots.all { it == SlotStatus.TAKEN }
    val kept get() = slots.isNotEmpty() && slots.none { it == SlotStatus.PENDING } && slots.any { it == SlotStatus.TAKEN }
}

fun buildCells(
    pills: List<Pill>,
    index: DoseIndex,
    from: Long,
    to: Long,
    zone: ZoneId,
    includeOptional: Boolean = false,
): List<DayCell> {
    val counted = pills.filter { !it.asNeeded && (includeOptional || !it.optional) }
    return (from..to).map { day ->
        val slots = counted
            .flatMap { it.plan(day, zone, index) }
            .sortedBy { it.minute }
            .map { it.status }
        DayCell(day, slots)
    }
}

data class Stats(val streak: Int, val adherence: Int?, val taken: Int)

fun stats(cells: List<DayCell>, today: Long, taken: Int): Stats {
    val byDay = cells.associateBy { it.day }
    var streak = 0
    if (byDay[today]?.kept == true) streak++
    var day = today - 1
    while (true) {
        val cell = byDay[day] ?: break
        if (cell.slots.isEmpty()) { day--; continue }
        if (!cell.kept) break
        streak++
        day--
    }
    val window = cells.filter { it.day in (today - 30) until today }.flatMap { it.slots }
    val adherence = if (window.isEmpty()) null
    else (window.count { it == SlotStatus.TAKEN } * 100f / window.size).toInt()
    return Stats(streak, adherence, taken)
}

/**
 * An earlier day as it stands now: every pill that was due, with missed doses still pending so
 * they can be filled in. As-needed pills carry what was logged that day.
 */
fun buildPastDay(pills: List<Pill>, index: DoseIndex, day: Long, zone: ZoneId): List<PillToday> =
    pills.filter { it.isActiveOn(day) }.mapNotNull { pill ->
        val taken = index.on(pill.id, day).filter { it.status == DoseStatus.TAKEN }
        val last = taken.maxOfOrNull { it.at }
        if (pill.asNeeded) return@mapNotNull PillToday(pill, emptyList(), Phase.AS_NEEDED, null, null, taken.size, last)
        val slots = pill.plan(day, zone, index)
        if (slots.isEmpty()) return@mapNotNull null
        val target = slots.firstOrNull { it.status == SlotStatus.PENDING }
        PillToday(pill, slots, if (target == null) Phase.DONE else Phase.DUE, target, null, taken.size, last)
    }.sortedWith(compareBy<PillToday> { it.phase.ordinal }.thenBy { it.pill.name.lowercase() })

// ── Today ────────────────────────────────────────────────────────────────────────────────────

enum class Phase { DUE, LATER, AS_NEEDED, DONE, NOT_TODAY, FINISHED }

data class PillToday(
    val pill: Pill,
    val slots: List<Slot>,
    val phase: Phase,
    /** The slot the main button acts on: the earliest one still pending. */
    val target: Slot?,
    val next: ZonedDateTime?,
    val takenToday: Int,
    val lastTakenAt: Long?,
)

fun buildToday(pills: List<Pill>, index: DoseIndex, now: ZonedDateTime): List<PillToday> {
    val zone = now.zone
    val today = now.toLocalDate().toEpochDay()
    val nowMinute = now.hour * 60 + now.minute
    return pills.map { pill ->
        val slots = pill.plan(today, zone, index)
        val todayDoses = index.on(pill.id, today)
        val takenToday = todayDoses.count { it.status == DoseStatus.TAKEN }
        val lastTaken = index[pill.id]?.values?.flatten()
            ?.filter { it.status == DoseStatus.TAKEN }?.maxOfOrNull { it.at }
        val target = slots.firstOrNull { it.status == SlotStatus.PENDING }
        val phase = when {
            pill.endDay != null && pill.endDay < today -> Phase.FINISHED
            pill.asNeeded -> if (pill.isActiveOn(today)) Phase.AS_NEEDED else Phase.NOT_TODAY
            slots.isEmpty() -> Phase.NOT_TODAY
            target == null -> Phase.DONE
            target.minute <= nowMinute -> Phase.DUE
            else -> Phase.LATER
        }
        val next = if (phase == Phase.FINISHED) null else pill.nextDose(now, index)
        PillToday(pill, slots, phase, target, next, takenToday, lastTaken)
    }.sortedWith(
        compareBy<PillToday> { it.phase.ordinal }
            .thenBy { it.target?.minute ?: Int.MAX_VALUE }
            .thenBy { it.next?.toEpochSecond() ?: Long.MAX_VALUE }
            .thenBy { it.pill.name.lowercase() },
    )
}
