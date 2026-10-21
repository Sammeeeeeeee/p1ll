package io.github.sammeeeeeeee.p1ll.ui

import android.content.Context
import io.github.sammeeeeeeee.p1ll.data.Pill
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

    fun meta(p: Pill): String = listOf(p.dosage.trim(), schedule(p).replaceFirstChar { if (p.dosage.isBlank()) it else it.lowercaseChar() })
        .filter { it.isNotEmpty() }.joinToString(", ")

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
