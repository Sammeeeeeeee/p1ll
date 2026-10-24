package io.github.sammeeeeeeee.p1ll.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.domain.Slot
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.domain.plan
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.ui.components.BounceOverscrollFactory
import io.github.sammeeeeeeee.p1ll.ui.components.LocalReveal
import io.github.sammeeeeeeee.p1ll.ui.components.rememberChartWave
import io.github.sammeeeeeeee.p1ll.ui.components.rememberReveal
import io.github.sammeeeeeeee.p1ll.ui.detail.DetailActions
import io.github.sammeeeeeeee.p1ll.ui.detail.DetailScreen
import io.github.sammeeeeeeee.p1ll.ui.editor.EditorScreen
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.home.DaySheet
import io.github.sammeeeeeeee.p1ll.ui.home.HomeActions
import io.github.sammeeeeeeee.p1ll.ui.home.HomeScreen
import io.github.sammeeeeeeee.p1ll.ui.nav.NavHost
import io.github.sammeeeeeeee.p1ll.ui.nav.Screen
import io.github.sammeeeeeeee.p1ll.ui.nav.rememberNavigator
import io.github.sammeeeeeeee.p1ll.ui.settings.SettingsScreen
import io.github.sammeeeeeeee.p1ll.ui.theme.P1llTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class OpenDay(val day: Long, val pillId: Long?)

@Composable
fun App(vm: AppViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val undo by vm.undo.collectAsStateWithLifecycle()
    val wave = rememberChartWave()
    val scope = rememberCoroutineScope()
    val feedback = vm.feedback
    val context = LocalContext.current
    var openDay by remember { mutableStateOf<OpenDay?>(null) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val reveal = rememberReveal()
    val navigator = rememberNavigator()

    fun take(pill: Pill, slot: Slot) {
        val state = vm.home.value
        val completes = !pill.optional && state.left == 1 && slot.status == SlotStatus.PENDING && slot.day == state.day
        if (completes) feedback.complete() else feedback.taken(state.takenToday + 1)
        // The ripple runs out from today's square, so it only plays for today.
        if (!pill.optional && state.isToday) scope.launch { wave.play(strong = completes) }
        vm.take(slot, "${pill.name} taken")
    }

    // Take on a reminder: come home to the dose's day, let the screen settle, then take it in view.
    // Requests queue, so a second Take tapped meanwhile waits its turn; the reminder itself stays
    // up until the dose is stored, which is what clears it.
    val reminderTakes by vm.reminderTakes.collectAsStateWithLifecycle()
    val request = reminderTakes.firstOrNull()
    LaunchedEffect(request) {
        if (request == null) return@LaunchedEffect
        // Don't throw away a half-edited pill: wait until the editor has been closed.
        snapshotFlow { navigator.stack.none { it is Screen.Editor } }.first { it }
        openDay = null
        val settled = navigator.current == Screen.Home && vm.home.value.day == request.day
        navigator.popTo(Screen.Home)
        // After a cold start the data has to load before the day can be checked and shown.
        vm.home.first { it.loaded }
        vm.showDay(request.day)
        delay(if (reveal.opening) 1100 else if (settled) 250 else 550)
        val latest = vm.snapshot.value
        val pill = latest.pills.find { it.id == request.pillId }
        val slot = pill?.let { p ->
            p.plan(request.day, latest.now.zone, latest.index).firstOrNull { it.minute == request.minute }
                ?: Slot(p.id, request.day, request.minute, SlotStatus.PENDING)
        }
        if (pill != null && slot != null && slot.status == SlotStatus.PENDING) take(pill, slot)
        // Already handled (or the pill is gone): taking stores nothing, so clear the reminder here.
        else Notifications.cancel(context, request.pillId, request.minute)
        vm.reminderTakeHandled(request)
    }

    P1llTheme(prefs) {
        CompositionLocalProvider(
            LocalFeedback provides feedback,
            LocalReveal provides reveal,
            LocalOverscrollFactory provides BounceOverscrollFactory,
        ) {
            NavHost(navigator) { screen, nav ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        state = home,
                        undo = undo,
                        wave = wave,
                        actions = HomeActions(
                            take = { card, slot -> take(card.pill, slot) },
                            skip = { _, slot ->
                                feedback.tick()
                                vm.skip(slot)
                            },
                            untake = { slot ->
                                feedback.undo()
                                slot.doseId?.let(vm::clearDose)
                            },
                            log = { card ->
                                feedback.taken(home.takenToday + 1)
                                vm.logAsNeeded(card.pill, "${card.pill.name} logged")
                            },
                            open = { nav.push(Screen.Detail(it.id)) },
                            add = { nav.push(Screen.Editor(null, fromFab = true)) },
                            settings = { nav.push(Screen.Settings) },
                            showDay = vm::showDay,
                            logAt = { card, minute ->
                                feedback.taken(home.takenToday + 1)
                                vm.logAt(card.pill.id, home.day, minute)
                            },
                            undo = vm::undo,
                        ),
                    )
                    is Screen.Detail -> DetailScreen(
                        screen.id,
                        snapshot,
                        DetailActions(
                            back = nav::pop,
                            edit = { nav.push(Screen.Editor(it.id, fromFab = false)) },
                            finish = {
                                feedback.saved()
                                vm.finish(it)
                            },
                            delete = {
                                feedback.undo()
                                vm.delete(it)
                                nav.popTo(Screen.Home)
                            },
                            refill = vm::refill,
                            day = { openDay = OpenDay(it, screen.id) },
                        ),
                    )
                    is Screen.Editor -> EditorScreen(
                        initial = screen.id?.let { id -> snapshot.pills.find { it.id == id } },
                        today = snapshot.today,
                        usedColors = snapshot.pills.map { it.color }.toSet(),
                        fromFab = screen.fromFab,
                        onClose = nav::pop,
                        onSave = { pill, backfill ->
                            vm.save(pill, backfill)
                            if (pill.id == 0L && Build.VERSION.SDK_INT >= 33 && !Notifications.allowed(context)) {
                                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            nav.pop()
                        },
                    )
                    Screen.Settings -> SettingsScreen(prefs, nav::pop) { vm.updatePrefs(it) }
                }
            }
            openDay?.let { open ->
                DaySheet(
                    day = open.day,
                    snapshot = snapshot,
                    pillId = open.pillId,
                    onSet = { slot, status -> vm.set(slot, status) },
                    onRemoveDose = vm::clearDose,
                    onAddDose = { pillId, minute -> vm.logAt(pillId, open.day, minute) },
                    onDismiss = { openDay = null },
                )
            }
        }
    }
}
