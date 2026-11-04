package io.github.sammeeeeeeee.p1ll.ui

import android.content.Context
import io.github.sammeeeeeeee.p1ll.data.DoseUnit
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.PillShape
import io.github.sammeeeeeeee.p1ll.data.ScheduleType
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Plain-language descriptions of schedules and times, shared by every screen. */
object Describe {
    private const val WEEKDAYS = 0b0011111
    private const val WEEKEND = 0b1100000

    fun schedule(p: Pill): String = when (p.schedule) {
        ScheduleType.DAILY -> perDay(p.times.size)
        ScheduleType.WEEKDAYS -> when (p.weekdays) {
            Pill.ALL_WEEK -> perDay(p.times.size)
            WEEKDAYS -> "Weekdays"
            WEEKEND -> "Weekends"
            else -> (0..6).filter { p.weekdays and (1 shl it) != 0 }
                .joinToString(", ") { DayOfWeek.of(it + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        }
        ScheduleType.INTERVAL -> if (p.intervalDays == 2) "Every other day" else "Every ${p.intervalDays} days"
        ScheduleType.AS_NEEDED -> "As needed"
    }

    fun perDay(n: Int) = when (n) {
        1 -> "Once a day"
        2 -> "Twice a day"
        else -> "$n× a day"
    }

    fun meta(p: Pill): String {
        val daily = p.schedule == ScheduleType.DAILY || (p.schedule == ScheduleType.WEEKDAYS && p.weekdays == Pill.ALL_WEEK)
        // Day names keep their capital; "Every other day" and the like don't, mid-line.
        val dayList = p.schedule == ScheduleType.WEEKDAYS && p.weekdays !in setOf(WEEKDAYS, WEEKEND)
        val pattern = if (daily) "" else schedule(p).let { if (dayList) it else it.replaceFirstChar(Char::lowercaseChar) }
        return listOf(amount(p.amount, p.unit, p.shape), p.strength.trim(), pattern).filter { it.isNotEmpty() }.joinToString(", ")
    }

    /** "1 pill", "7.5 ml", "5 g", "2 puffs". */
    fun amount(amount: Double, unit: DoseUnit, shape: PillShape): String = "${Fmt.amount(amount)} ${unit(unit, shape, amount)}"

    fun unit(unit: DoseUnit, shape: PillShape, amount: Double = 2.0): String {
        if (unit == DoseUnit.ML) return "ml"
        if (unit == DoseUnit.G) return "g"
        val (one, many) = when (shape) {
            PillShape.ROUND, PillShape.CAPSULE, PillShape.OVAL, PillShape.TABLET -> "pill" to "pills"
            PillShape.DROP -> "drop" to "drops"
            PillShape.SACHET -> "sachet" to "sachets"
            PillShape.TUBE -> "application" to "applications"
            PillShape.BOTTLE -> "dose" to "doses"
            PillShape.SPRAY -> "spray" to "sprays"
            PillShape.INHALER -> "puff" to "puffs"
            PillShape.SYRINGE -> "injection" to "injections"
            PillShape.PATCH -> "patch" to "patches"
        }
        return if (amount == 1.0) one else many
    }

    /** Supply in its unit: "12 pills", "97.5 ml". */
    fun supply(p: Pill): String = amount(p.stock ?: 0.0, p.unit, p.shape)

    /** Whole days the supply lasts on schedule; null for as-needed pills. */
    fun daysOfSupply(p: Pill): Int? {
        val stock = p.stock ?: return null
        val perDay = p.times.size * p.amount
        return if (p.asNeeded || perDay <= 0) null else (stock / perDay).toInt()
    }

    /** "today 14:00", "tomorrow 08:00", "Fri 08:00", "12 Nov 08:00". */
    fun whenNext(context: Context, at: ZonedDateTime, now: ZonedDateTime): String {
        val days = at.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay()
        val time = Fmt.time(context, at)
        return when {
            days <= 0L -> time
            days == 1L -> "tomorrow $time"
            days < 7L -> "${Fmt.weekday(at.toLocalDate().toEpochDay(), TextStyle.SHORT)} $time"
            else -> "${Fmt.shortDate(at.toLocalDate().toEpochDay())} $time"
        }
    }

    fun times(context: Context, p: Pill): String = p.times.joinToString(", ") { Fmt.time(context, it) }
}
