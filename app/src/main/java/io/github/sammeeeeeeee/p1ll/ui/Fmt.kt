package io.github.sammeeeeeeee.p1ll.ui

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formatting that follows the phone's 12/24-hour setting rather than just the locale. */
object Fmt {
    private fun pattern(context: Context) = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"

    fun time(context: Context, minute: Int): String =
        LocalTime.of(minute / 60 % 24, minute % 60).format(DateTimeFormatter.ofPattern(pattern(context), Locale.getDefault()))

    fun time(context: Context, at: ZonedDateTime): String = time(context, at.hour * 60 + at.minute)

    fun weekday(day: Long, style: TextStyle = TextStyle.FULL): String =
        LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(style, Locale.getDefault())

    fun dayMonth(day: Long): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))

    fun shortDate(day: Long): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

    /** "in 2 h 5 min", "in 12 min", "now". */
    fun until(minutes: Long): String = when {
        minutes <= 0 -> "now"
        minutes < 60 -> "in $minutes min"
        minutes % 60 == 0L -> "in ${minutes / 60} h"
        else -> "in ${minutes / 60} h ${minutes % 60} min"
    }

    /** "3 min ago", "2 h ago", "yesterday", "4 days ago". */
    fun ago(minutes: Long): String = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        minutes < 48 * 60 -> "yesterday"
        else -> "${minutes / (24 * 60)} days ago"
    }
}
