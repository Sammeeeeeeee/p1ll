package io.github.sammeeeeeeee.p1ll

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sammeeeeeeee.p1ll.reminders.ActionReceiver
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.ui.App
import io.github.sammeeeeeeee.p1ll.ui.AppViewModel
import io.github.sammeeeeeeee.p1ll.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) {
            // The splash icon pulls back, then zooms away as the home screen arrives behind it.
            splashScreen.setOnExitAnimationListener { splash ->
                splash.iconView?.animate()
                    ?.scaleX(2.2f)?.scaleY(2.2f)?.alpha(0f)
                    ?.setInterpolator(AnticipateInterpolator(1.8f))
                    ?.setDuration(520)
                    ?.start()
                splash.animate()
                    .alpha(0f)
                    .setStartDelay(220)
                    .setDuration(320)
                    .withEndAction { splash.remove() }
                    .start()
            }
        }
        enableEdgeToEdge()
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            val dark = isDark(prefs.theme)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            App(vm)
        }
        if (savedInstanceState == null) handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /**
     * Take on a reminder: the home screen records the dose in view. This activity is exported (it's
     * the launcher), so only intents carrying the reminder's private token count.
     */
    private fun handle(intent: Intent?) {
        if (intent?.action != ACTION_TAKE || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        setIntent(Intent(this, MainActivity::class.java))
        if (!Notifications.isOwnTake(this, intent)) return
        val pillId = intent.getLongExtra(ActionReceiver.EXTRA_PILL, -1)
        val day = intent.getLongExtra(ActionReceiver.EXTRA_DAY, -1)
        val minute = intent.getIntExtra(ActionReceiver.EXTRA_MINUTE, -1)
        if (pillId < 0 || day < 0 || minute < 0) return
        vm.takeFromReminder(pillId, day, minute)
    }

    override fun onResume() {
        super.onResume()
        vm.tick()
    }

    companion object {
        const val ACTION_TAKE = "io.github.sammeeeeeeee.p1ll.TAKE_IN_APP"
    }
}
