package io.github.sammeeeeeeee.p1ll.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.sammeeeeeeee.p1ll.BuildConfig
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.data.ThemeMode
import io.github.sammeeeeeeee.p1ll.domain.DayCell
import io.github.sammeeeeeeee.p1ll.domain.SlotStatus
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.reminders.Reminders
import io.github.sammeeeeeeee.p1ll.ui.components.CircleButton
import io.github.sammeeeeeeee.p1ll.ui.components.Divider
import io.github.sammeeeeeeee.p1ll.ui.components.DoseChart
import io.github.sammeeeeeeee.p1ll.ui.components.GhostButton
import io.github.sammeeeeeeee.p1ll.ui.components.Icons
import io.github.sammeeeeeeee.p1ll.ui.components.Segmented
import io.github.sammeeeeeeee.p1ll.ui.components.SettingRow
import io.github.sammeeeeeeee.p1ll.ui.components.Surface
import io.github.sammeeeeeeee.p1ll.ui.components.enter
import io.github.sammeeeeeeee.p1ll.ui.components.Toggle
import io.github.sammeeeeeeee.p1ll.ui.components.tappable
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.Accents
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import java.time.LocalDate

@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit, onChange: ((Prefs) -> Prefs) -> Unit) {
    val c = P1ll.colors
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Re-read permission state whenever we come back from the system settings.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val notificationsOk = remember(resumes) { Notifications.allowed(context) }
    val health = remember(resumes) { Notifications.health(context) }
    val exactOk = remember(resumes) { Reminders.canExact(context) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumes++ }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = top + 8.dp, bottom = bottom + 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Icons.Back, "Back", onBack)
            Spacer(Modifier.width(14.dp))
            Text("Settings", style = Type.title)
        }

        Spacer(Modifier.height(20.dp))
        if (health != Notifications.Health.OK || !exactOk) {
            val appSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            Surface(Modifier.enter(0), color = c.warn.copy(alpha = 0.12f)) {
                Column(Modifier.padding(18.dp)) {
                    val (title, detail) = when (health) {
                        Notifications.Health.OFF -> "Notifications off" to null
                        Notifications.Health.SILENT -> "Reminders are silent" to "They won't sound or pop up."
                        Notifications.Health.OK -> "Exact alarms off" to "Reminders may be a few minutes late."
                    }
                    Text(title, style = Type.headline)
                    if (detail != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(detail, style = Type.body, color = c.ink2)
                    }
                    Spacer(Modifier.height(14.dp))
                    GhostButton(if (health == Notifications.Health.OFF || health == Notifications.Health.OK) "Allow" else "Open settings", {
                        when {
                            health == Notifications.Health.OFF && !notificationsOk && Build.VERSION.SDK_INT >= 33 ->
                                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            health == Notifications.Health.SILENT -> context.startActivity(
                                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .putExtra(Settings.EXTRA_CHANNEL_ID, Notifications.CHANNEL_DOSES),
                            )
                            health != Notifications.Health.OK -> context.startActivity(appSettings)
                            Build.VERSION.SDK_INT >= 31 ->
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }
                    }, icon = Icons.Bell)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Surface(Modifier.enter(1)) {
            Column(Modifier.padding(14.dp)) {
                Segmented(
                    ThemeMode.entries,
                    prefs.theme,
                    label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelect = { mode -> onChange { it.copy(theme = mode) } },
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Accents.forEachIndexed { i, accent ->
                        val selected = i == prefs.accent
                        val ring by animateFloatAsState(if (selected) 1f else 0f, spring(0.5f, 500f), label = "accent")
                        val swatch = accent.tone.pick(c.dark)
                        Box(
                            Modifier
                                .size(40.dp)
                                .tappable(pressedScale = 0.8f) {
                                    feedback.tick()
                                    onChange { it.copy(accent = i) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .graphicsLayer { alpha = ring; scaleX = 0.7f + 0.3f * ring; scaleY = 0.7f + 0.3f * ring }
                                    .border(2.dp, swatch, CircleShape),
                            )
                            Box(Modifier.size(28.dp - 6.dp * ring).clip(CircleShape).background(swatch))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                DoseChart(sampleCells(), c.accent, cellSize = 13.dp, gap = 3.dp, background = c.surface)
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(Modifier.enter(2)) {
            Column(Modifier.padding(14.dp)) {
                Text("Remind again", style = Type.label, color = c.ink2, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                Segmented(
                    listOf(0, 10, 15, 30),
                    prefs.nagMinutes,
                    label = { if (it == 0) "Never" else "$it min" },
                    onSelect = { m -> onChange { it.copy(nagMinutes = m) } },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(Modifier.enter(3)) {
            SettingRow("Sounds") {
                Toggle(prefs.sounds, { on ->
                    onChange { it.copy(sounds = on) }
                })
            }
            Divider()
            SettingRow("Haptics") { Toggle(prefs.haptics, { on -> onChange { it.copy(haptics = on) } }) }
        }

        Spacer(Modifier.height(12.dp))
        Surface(Modifier.enter(4)) {
            SettingRow("Version") { Text(BuildConfig.VERSION_NAME, style = Type.label, color = c.ink2) }
            Divider()
            Box(
                Modifier.tappable(pressedScale = 0.98f) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sammeeeeeeee/p1ll")))
                },
            ) {
                SettingRow("Source code") {
                    Icon(Icons.ChevronRight, null, tint = c.ink3, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** A made-up few months for previewing chart colours. */
private fun sampleCells(): List<DayCell> {
    val today = LocalDate.now().toEpochDay()
    val random = kotlin.random.Random(21)
    return (0 until 18 * 7).map { i ->
        val taken = if (random.nextFloat() < 0.72f) 3 else random.nextInt(0, 3)
        DayCell(today - 18 * 7 + 1 + i, List(3) { if (it < taken) SlotStatus.TAKEN else SlotStatus.PENDING })
    }
}
