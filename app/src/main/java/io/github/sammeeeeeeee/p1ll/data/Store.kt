package io.github.sammeeeeeeee.p1ll.data

import android.content.Context
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.matchSlots
import io.github.sammeeeeeeee.p1ll.domain.slotsOn
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.reminders.Reminders
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId
import java.time.ZonedDateTime

/** Every write goes through here so reminders and supply stay in step with the data. */
class Store(private val context: Context, val db: P1llDb, val settings: SettingsStore) {
    val pills: Flow<List<Pill>> = db.pills().observeAll()
    val doses: Flow<List<Dose>> = db.doses().observeAll()

    suspend fun save(pill: Pill): Long {
        val id = if (pill.id == 0L) db.pills().insert(pill) else pill.id.also { db.pills().update(pill) }
        Reminders.sync(context)
        return id
    }

    suspend fun delete(pill: Pill) {
        pill.times.forEach { Notifications.cancel(context, pill.id, it) }
        db.pills().delete(pill)
        Reminders.sync(context)
    }

    /** Ends the course today if something was logged today, otherwise yesterday. */
    suspend fun finish(pill: Pill) {
        val today = ZonedDateTime.now().toLocalDate().toEpochDay()
        val loggedToday = db.doses().forPillDay(pill.id, today).isNotEmpty()
        save(pill.copy(endDay = if (loggedToday) today else today - 1))
    }

    suspend fun refill(pill: Pill, amount: Int) {
        db.pills().setStock(pill.id, (pill.stock ?: 0) + amount)
    }

    suspend fun take(slot: Slot): Long = setStatus(slot, DoseStatus.TAKEN)

    suspend fun skip(slot: Slot): Long = setStatus(slot, DoseStatus.SKIPPED)

    /** Used by notification actions, which only know the pill, day and slot minute. */
    suspend fun takeAt(pillId: Long, day: Long, minute: Int): Long? {
        val slot = resolve(pillId, day, minute) ?: return null
        if (slot.status == SlotStatus.TAKEN) return slot.doseId
        return take(slot)
    }

    suspend fun clear(doseId: Long) {
        val dose = db.doses().get(doseId) ?: return
        db.doses().delete(doseId)
        if (dose.status == DoseStatus.TAKEN) adjustStock(dose.pillId, +1)
        Reminders.sync(context)
    }

    suspend fun logAsNeeded(pillId: Long): Long {
        val now = ZonedDateTime.now()
        val id = db.doses().insert(
            Dose(
                pillId = pillId,
                day = now.toLocalDate().toEpochDay(),
                minute = now.hour * 60 + now.minute,
                status = DoseStatus.TAKEN,
                at = now.toInstant().toEpochMilli(),
            ),
        )
        adjustStock(pillId, -1)
        return id
    }

    suspend fun snooze(pillId: Long, day: Long, minute: Int) {
        Reminders.snooze(context, pillId, day, minute, settings.current().snoozeMinutes)
    }

    suspend fun updateSettings(transform: (Prefs) -> Prefs) {
        settings.update(transform)
        Reminders.sync(context)
    }

    private suspend fun resolve(pillId: Long, day: Long, minute: Int): Slot? {
        val pill = db.pills().get(pillId) ?: return null
        val minutes = pill.slotsOn(day, ZoneId.systemDefault()).ifEmpty { listOf(minute) }
        return matchSlots(pillId, day, minutes, db.doses().forPillDay(pillId, day))
            .firstOrNull { it.minute == minute }
            ?: Slot(pillId, day, minute, SlotStatus.PENDING)
    }

    private suspend fun setStatus(slot: Slot, status: DoseStatus): Long {
        val now = System.currentTimeMillis()
        val existing = slot.doseId?.let { db.doses().get(it) }
        val id = if (existing != null) {
            db.doses().update(existing.copy(status = status, at = now))
            existing.id
        } else {
            db.doses().insert(Dose(pillId = slot.pillId, day = slot.day, minute = slot.minute, status = status, at = now))
        }
        val wasTaken = existing?.status == DoseStatus.TAKEN
        val isTaken = status == DoseStatus.TAKEN
        if (wasTaken != isTaken) adjustStock(slot.pillId, if (isTaken) -1 else +1)
        Notifications.cancel(context, slot.pillId, slot.minute)
        Reminders.sync(context)
        return id
    }

    private suspend fun adjustStock(pillId: Long, direction: Int) {
        val pill = db.pills().get(pillId) ?: return
        val stock = pill.stock ?: return
        val next = (stock + direction * pill.perDose).coerceAtLeast(0)
        db.pills().setStock(pillId, next)
        val threshold = pill.refillAt ?: return
        if (direction < 0 && stock > threshold && next <= threshold) {
            Notifications.lowStock(context, pill.copy(stock = next))
        }
    }
}
