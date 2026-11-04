package io.github.sammeeeeeeee.p1ll.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class ExportFormat(val mime: String, val extension: String) {
    CSV("text/csv", "csv"),
    JSON("application/json", "json"),
}

object Export {
    fun fileName(format: ExportFormat, today: LocalDate = LocalDate.now()) = "p1ll-$today.${format.extension}"

    fun render(format: ExportFormat, pills: List<Pill>, doses: List<Dose>, zone: ZoneId = ZoneId.systemDefault()): String =
        when (format) {
            ExportFormat.CSV -> csv(pills, doses, zone)
            ExportFormat.JSON -> json(pills, doses)
        }

    /** One row per logged dose, with its pill's name and strength, in local time. */
    fun csv(pills: List<Pill>, doses: List<Dose>, zone: ZoneId): String {
        val byId = pills.associateBy { it.id }
        val rows = doses.sortedWith(compareBy({ it.day }, { it.minute })).map { d ->
            val pill = byId[d.pillId]
            listOf(
                LocalDate.ofEpochDay(d.day).toString(),
                "%02d:%02d".format(Locale.ROOT, d.minute / 60, d.minute % 60),
                pill?.name.orEmpty(),
                pill?.strength.orEmpty(),
                d.status.name.lowercase(),
                Instant.ofEpochMilli(d.at).atZone(zone).toOffsetDateTime().toString(),
            )
        }
        return (listOf(listOf("date", "time", "pill", "strength", "status", "logged_at")) + rows)
            .joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",", transform = ::cell) }
    }

    /** Every stored field, unconverted: days are epoch days, minutes are after midnight, times are epoch millis. */
    fun json(pills: List<Pill>, doses: List<Dose>): String = JSONObject()
        .put("exportedAt", System.currentTimeMillis())
        .put("pills", JSONArray(pills.map { it.toJson() }))
        .put("doses", JSONArray(doses.map { it.toJson() }))
        .toString(2)

    private fun Pill.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("strength", strength)
        .put("color", color)
        .put("shape", shape.name)
        .put("schedule", schedule.name)
        .put("weekdays", weekdays)
        .put("intervalDays", intervalDays)
        .put("times", JSONArray(times))
        .put("startDay", startDay)
        .put("endDay", endDay ?: JSONObject.NULL)
        .put("optional", optional)
        .put("notes", notes)
        .put("stock", stock ?: JSONObject.NULL)
        .put("amount", amount)
        .put("unit", unit.name)
        .put("refillAt", refillAt ?: JSONObject.NULL)
        .put("createdAt", createdAt)

    private fun Dose.toJson() = JSONObject()
        .put("id", id)
        .put("pillId", pillId)
        .put("day", day)
        .put("minute", minute)
        .put("status", status.name)
        .put("at", at)

    private fun cell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
