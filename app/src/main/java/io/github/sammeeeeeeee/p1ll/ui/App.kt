package io.github.sammeeeeeeee.p1ll.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.ui.components.rememberChartWave
import io.github.sammeeeeeeee.p1ll.ui.detail.DetailActions
import io.github.sammeeeeeeee.p1ll.ui.detail.DetailScreen
import io.github.sammeeeeeeee.p1ll.ui.editor.EditorScreen
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.home.DaySheet
import io.github.sammeeeeeeee.p1ll.ui.home.HomeActions
import io.github.sammeeeeeeee.p1ll.ui.home.HomeScreen
import io.github.sammeeeeeeee.p1ll.ui.nav.NavHost
import io.github.sammeeeeeeee.p1ll.ui.nav.Screen
import io.github.sammeeeeeeee.p1ll.ui.settings.SettingsScreen
import io.github.sammeeeeeeee.p1ll.ui.theme.P1llTheme
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

    P1llTheme(prefs) {
        CompositionLocalProvider(LocalFeedback provides feedback) {
            NavHost { screen, nav ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        state = home,
                        undo = undo,
                        wave = wave,
                        actions = HomeActions(
                            take = { card, slot ->
                                val completes = !card.pill.optional && home.left == 1 && slot.status == SlotStatus.PENDING
                                if (completes) feedback.complete() else feedback.taken(home.takenToday + 1)
                                if (!card.pill.optional) scope.launch { wave.play(strong = completes) }
                                vm.take(slot, "${card.pill.name} taken")
                            },
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
                            day = { openDay = OpenDay(it, null) },
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
                        onSave = { pill ->
                            vm.save(pill)
                            if (pill.id == 0L && Build.VERSION.SDK_INT >= 33 && !Notifications.allowed(context)) {
                                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            nav.pop()
                        },
                        onDelete = { pill ->
                            feedback.undo()
                            vm.delete(pill)
                            nav.popTo(Screen.Home)
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
                    onDismiss = { openDay = null },
                )
            }
        }
    }
}
