package io.github.sammeeeeeeee.p1ll

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
    }

    override fun onResume() {
        super.onResume()
        vm.tick()
    }
}
