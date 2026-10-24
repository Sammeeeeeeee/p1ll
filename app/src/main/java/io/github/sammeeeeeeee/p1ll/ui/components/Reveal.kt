package io.github.sammeeeeeeee.p1ll.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Remembers which pieces of the home screen have already made their entrance this session. */
@Stable
class Reveal internal constructor(private val startedAt: Long) {
    private val seen = HashSet<Any>()

    internal fun firstTime(key: Any) = seen.add(key)

    /** The first moments after launch, when entrances are staggered top to bottom. */
    internal val opening get() = System.nanoTime() - startedAt < 1_000_000_000L
}

@Composable
fun rememberReveal() = remember { Reveal(System.nanoTime()) }

/** True the first time [key] is composed this session, for one-off flourishes like counting up. */
@Composable
fun rememberFirstTime(key: Any): Boolean {
    val reveal = LocalReveal.current ?: return false
    return remember(key) { reveal.firstTime(key) }
}

val LocalReveal = staticCompositionLocalOf<Reveal?> { null }

/**
 * Rise, unblur and settle into place with a small bounce, once per [key] per session. On opening
 * the app items follow each other by [order]; later (scrolling down, a new pill) they arrive alone.
 * It waits for the item to be placed, so rows composed ahead of the viewport keep their entrance.
 */
@Composable
fun Modifier.reveal(key: Any, order: Int = 0): Modifier {
    val reveal = LocalReveal.current ?: return this
    val first = remember(key) { reveal.firstTime(key) }
    if (!first) return this
    val opening = remember { reveal.opening }
    val progress = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(placed) {
        if (!placed) return@LaunchedEffect
        if (opening) delay(order.coerceAtMost(9) * 55L)
        progress.animateTo(1f, spring(dampingRatio = 0.66f, stiffness = 240f))
    }
    return onPlaced { placed = true }.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * (if (opening) 26 else 44).dp.toPx()
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
        if (Build.VERSION.SDK_INT >= 31) {
            val r = (1f - p).coerceAtLeast(0f) * 8.dp.toPx()
            renderEffect = if (r > 0.5f) BlurEffect(r, r) else null
        }
    }
}

/**
 * The same rise-and-settle as [reveal], but every time the element is composed: for screens and
 * sheets that open on demand, where each opening deserves its entrance. [order] staggers siblings.
 */
@Composable
fun Modifier.enter(order: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order.coerceAtMost(10) * 45L)
        progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 260f))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 30.dp.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
        if (Build.VERSION.SDK_INT >= 31) {
            val r = (1f - p).coerceAtLeast(0f) * 8.dp.toPx()
            renderEffect = if (r > 0.5f) BlurEffect(r, r) else null
        }
    }
}

/**
 * Paging between days: the new day's pieces slide in from the side the day lies on (earlier
 * days from the left), a beat apart, and settle with a little give.
 */
@Composable
fun Modifier.pageIn(direction: Int, order: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order.coerceAtMost(8) * 32L)
        progress.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 320f))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationX = (1f - p) * 56.dp.toPx() * direction
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
        if (Build.VERSION.SDK_INT >= 31) {
            val r = (1f - p).coerceAtLeast(0f) * 6.dp.toPx()
            renderEffect = if (r > 0.5f) BlurEffect(r, r) else null
        }
    }
}
