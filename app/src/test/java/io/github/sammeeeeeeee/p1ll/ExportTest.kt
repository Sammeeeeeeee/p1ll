package io.github.sammeeeeeeee.p1ll

import io.github.sammeeeeeeee.p1ll.data.Dose
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Export
import io.github.sammeeeeeeee.p1ll.data.Pill
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExportTest {
    private val day = LocalDate.of(2026, 3, 1).toEpochDay()
    private val pill = Pill(id = 1, name = "Iron, \"slow\"", strength = "65 mg", startDay = day, stock = 20.5, createdAt = 0)
    private val doses = listOf(
        Dose(id = 2, pillId = 1, day = day, minute = 20 * 60 + 5, status = DoseStatus.SKIPPED, at = 1_000),
        Dose(id = 1, pillId = 1, day = day, minute = 8 * 60, status = DoseStatus.TAKEN, at = 0),
    )

    @Test
    fun csvIsSortedAndEscaped() {
        val lines = Export.csv(listOf(pill), doses, ZoneOffset.UTC).trimEnd().split("\r\n")
        assertEquals("date,time,pill,strength,status,logged_at", lines[0])
        assertEquals("2026-03-01,08:00,\"Iron, \"\"slow\"\"\",65 mg,taken,1970-01-01T00:00Z", lines[1])
        assertTrue(lines[2].startsWith("2026-03-01,20:05,"))
        assertEquals(3, lines.size)
    }

    @Test
    fun jsonKeepsRawFields() {
        val root = JSONObject(Export.json(listOf(pill), doses))
        val p = root.getJSONArray("pills").getJSONObject(0)
        assertEquals("Iron, \"slow\"", p.getString("name"))
        assertEquals(day, p.getLong("startDay"))
        assertTrue(p.isNull("endDay"))
        assertEquals(20.5, p.getDouble("stock"), 0.0)
        assertEquals("COUNT", p.getString("unit"))
        assertEquals(480, p.getJSONArray("times").getInt(0))
        assertEquals(2, root.getJSONArray("doses").length())
    }
}
