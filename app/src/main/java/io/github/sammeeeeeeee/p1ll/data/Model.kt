package io.github.sammeeeeeeee.p1ll.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ScheduleType { DAILY, WEEKDAYS, INTERVAL, AS_NEEDED }

enum class PillShape { ROUND, CAPSULE, OVAL, TABLET, DROP }

enum class DoseStatus { TAKEN, SKIPPED }

@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String = "",
    /** Index into [io.github.sammeeeeeeee.p1ll.ui.theme.PillColors]. */
    val color: Int = 0,
    val shape: PillShape = PillShape.ROUND,
    val schedule: ScheduleType = ScheduleType.DAILY,
    /** Bit 0 = Monday … bit 6 = Sunday. Only read for [ScheduleType.WEEKDAYS]. */
    val weekdays: Int = ALL_WEEK,
    /** Only read for [ScheduleType.INTERVAL]; counted from [startDay]. */
    val intervalDays: Int = 2,
    /** Reminder times as minutes after midnight, sorted. Empty for [ScheduleType.AS_NEEDED]. */
    val times: List<Int> = listOf(8 * 60),
    val startDay: Long,
    /** Last day the pill is taken (inclusive), as an epoch day. Null = no end. */
    val endDay: Long? = null,
    /** Optional pills are reminded but never count as missed. */
    val optional: Boolean = false,
    val notes: String = "",
    /** Pills left; null when supply isn't tracked. */
    val stock: Int? = null,
    val perDose: Int = 1,
    val refillAt: Int? = null,
    val createdAt: Long,
) {
    val asNeeded get() = schedule == ScheduleType.AS_NEEDED

    companion object {
        const val ALL_WEEK = 0b1111111
    }
}

@Entity(
    tableName = "doses",
    indices = [Index("pillId", "day")],
    foreignKeys = [ForeignKey(Pill::class, ["id"], ["pillId"], onDelete = ForeignKey.CASCADE)],
)
data class Dose(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pillId: Long,
    val day: Long,
    /** The scheduled slot this dose answers (minutes after midnight), or the log time for as-needed pills. */
    val minute: Int,
    val status: DoseStatus,
    /** When it was logged, epoch millis. */
    val at: Long,
)

class Converters {
    @TypeConverter
    fun timesToString(times: List<Int>): String = times.joinToString(",")

    @TypeConverter
    fun stringToTimes(raw: String): List<Int> = raw.split(',').mapNotNull { it.trim().toIntOrNull() }
}
