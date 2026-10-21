package io.github.sammeeeeeeee.p1ll.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.sammeeeeeeee.p1ll.P1llApp
import io.github.sammeeeeeeee.p1ll.data.Dose
import io.github.sammeeeeeeee.p1ll.data.DoseStatus
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.domain.DayCell
import io.github.sammeeeeeeee.p1ll.domain.DoseIndex
import io.github.sammeeeeeeee.p1ll.domain.Phase
import io.github.sammeeeeeeee.p1ll.domain.PillToday
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.Stats
import io.github.sammeeeeeeee.p1ll.domain.buildCells
import io.github.sammeeeeeeee.p1ll.domain.buildToday
import io.github.sammeeeeeeee.p1ll.domain.indexDoses
import io.github.sammeeeeeeee.p1ll.domain.stats
import io.github.sammeeeeeeee.p1ll.reminders.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Everything the screens derive from: one consistent read of the database at one moment. */
data class Snapshot(
    val pills: List<Pill>,
    val doses: List<Dose>,
    val index: DoseIndex,
    val now: ZonedDateTime,
    val loaded: Boolean,
) {
    val today: Long get() = now.toLocalDate().toEpochDay()

    companion object {
        fun empty() = Snapshot(emptyList(), emptyList(), emptyMap(), ZonedDateTime.now(), loaded = false)
    }
}

data class HomeState(
    val now: ZonedDateTime,
    val cards: List<PillToday>,
    val finished: List<Pill>,
    /** Required doses still to take today. Optional pills don't count. */
    val left: Int,
    val required: Int,
    val takenToday: Int,
    val nextUp: Pair<Pill, ZonedDateTime>?,
    val cells: List<DayCell>,
    val stats: Stats,
    val loaded: Boolean,
) {
    val today: Long get() = now.toLocalDate().toEpochDay()
    val due: List<PillToday> get() = cards.filter { it.phase == Phase.DUE }
}

const val CHART_DAYS = 364L

fun buildHome(s: Snapshot): HomeState {
    val today = s.today
    val all = buildToday(s.pills, s.index, s.now)
    val cards = all.filter { it.phase != Phase.FINISHED }
    val required = cards.filter { !it.pill.optional }.flatMap { it.slots }
    val cells = buildCells(s.pills, s.index, today - CHART_DAYS, today, s.now.zone)
    val nextUp = cards.mapNotNull { c -> c.next?.let { c.pill to it } }.minByOrNull { it.second }
    return HomeState(
        now = s.now,
        cards = cards,
        finished = all.filter { it.phase == Phase.FINISHED }.map { it.pill },
        left = required.count { it.status == SlotStatus.PENDING },
        required = required.size,
        takenToday = s.index.values.sumOf { days -> days[today].orEmpty().count { it.status == DoseStatus.TAKEN } },
        nextUp = nextUp,
        cells = cells,
        stats = stats(cells, today, s.doses.count { it.status == DoseStatus.TAKEN }),
        loaded = s.loaded,
    )
}

data class UndoInfo(val doseId: Long, val label: String, val serial: Long = System.nanoTime())

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as P1llApp).container
    private val store = container.store
    val feedback get() = container.feedback

    val prefs: StateFlow<Prefs> = container.settings.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    private val clock = MutableStateFlow(ZonedDateTime.now())

    val snapshot: StateFlow<Snapshot> = combine(store.pills, store.doses, clock) { pills, doses, now ->
        Snapshot(pills, doses, indexDoses(doses), now, loaded = true)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, Snapshot.empty())

    val home: StateFlow<HomeState> = snapshot.map(::buildHome)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildHome(Snapshot.empty()))

    private val _undo = MutableStateFlow<UndoInfo?>(null)
    val undo: StateFlow<UndoInfo?> = _undo.asStateFlow()
    private var undoJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                val now = ZonedDateTime.now()
                clock.value = now
                delay(60_000L - now.second * 1000L - now.nano / 1_000_000L + 20)
            }
        }
        viewModelScope.launch {
            prefs.collect {
                container.feedback.sounds = it.sounds
                container.feedback.haptics = it.haptics
            }
        }
        viewModelScope.launch(Dispatchers.IO) { Reminders.sync(app) }
    }

    fun tick() {
        clock.value = ZonedDateTime.now()
    }

    fun take(slot: Slot, label: String) = viewModelScope.launch {
        val id = store.take(slot)
        offerUndo(UndoInfo(id, label))
    }

    fun skip(slot: Slot) = viewModelScope.launch { store.skip(slot) }

    fun logAsNeeded(pill: Pill, label: String) = viewModelScope.launch {
        val id = store.logAsNeeded(pill.id)
        offerUndo(UndoInfo(id, label))
    }

    fun set(slot: Slot, status: SlotStatus) = viewModelScope.launch {
        when (status) {
            SlotStatus.TAKEN -> store.take(slot)
            SlotStatus.SKIPPED -> store.skip(slot)
            SlotStatus.PENDING -> slot.doseId?.let { store.clear(it) }
        }
    }

    fun clearDose(id: Long) = viewModelScope.launch { store.clear(id) }

    fun undo() {
        val info = _undo.value ?: return
        _undo.value = null
        container.feedback.undo()
        viewModelScope.launch { store.clear(info.doseId) }
    }

    fun dismissUndo() {
        _undo.value = null
    }

    private fun offerUndo(info: UndoInfo) {
        _undo.value = info
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(UNDO_MS)
            if (_undo.value?.serial == info.serial) _undo.value = null
        }
    }

    fun save(pill: Pill, backfill: Boolean = false) = viewModelScope.launch { store.save(pill, backfill) }

    fun logAt(pillId: Long, day: Long, minute: Int) = viewModelScope.launch { store.logAt(pillId, day, minute) }

    fun delete(pill: Pill) = viewModelScope.launch { store.delete(pill) }

    fun finish(pill: Pill) = viewModelScope.launch { store.finish(pill) }

    fun refill(pill: Pill, amount: Int) = viewModelScope.launch { store.refill(pill, amount) }

    fun updatePrefs(transform: (Prefs) -> Prefs) = viewModelScope.launch { store.updateSettings(transform) }

    companion object {
        const val UNDO_MS = 4500L
    }
}
