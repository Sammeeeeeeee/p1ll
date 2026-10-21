package io.github.sammeeeeeeee.p1ll.ui.nav

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.ui.theme.Motion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface Screen {
    val depth: Int

    data object Home : Screen { override val depth = 0 }
    data object Settings : Screen { override val depth = 1 }
    data class Detail(val id: Long) : Screen { override val depth = 1 }

    /** [fromFab] = morph out of the add button rather than slide up. */
    data class Editor(val id: Long?, val fromFab: Boolean) : Screen { override val depth = 2 }
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalShared = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val springBounds = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.86f, stiffness = 340f, visibilityThreshold = Rect.VisibilityThreshold)
}

/** Container morph between two screens. A no-op outside navigation (previews, screenshots). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.morph(key: String, radius: Dp = 24.dp): Modifier {
    val shared = LocalShared.current ?: return this
    val scope = LocalNavScope.current ?: return this
    return with(shared) {
        this@morph.sharedBounds(
            rememberSharedContentState(key),
            scope,
            enter = fadeIn(tween(220, delayMillis = 60)),
            exit = fadeOut(tween(120)),
            boundsTransform = springBounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(radius)),
        )
    }
}

/** The same element on both screens, flying between them. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shared(key: String): Modifier {
    val shared = LocalShared.current ?: return this
    val scope = LocalNavScope.current ?: return this
    return with(shared) {
        this@shared.sharedElement(rememberSharedContentState(key), scope, boundsTransform = springBounds)
    }
}

class Navigator internal constructor(
    internal val stack: MutableList<Screen>,
    private val go: (Screen) -> Unit,
) {
    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        stack.add(screen)
        go(screen)
    }

    fun pop() {
        if (stack.size <= 1) return
        stack.removeAt(stack.lastIndex)
        go(stack.last())
    }

    /** Replace the top screen without animating back through it (used after deleting). */
    fun popTo(screen: Screen) {
        while (stack.size > 1 && stack.last() != screen) stack.removeAt(stack.lastIndex)
        go(stack.last())
    }
}

/**
 * Screens live in one seekable transition. A predictive back swipe scrubs that transition
 * directly, so the shared-element morphs follow the thumb and finish or spring back from
 * wherever the gesture let go.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavHost(content: @Composable (Screen, Navigator) -> Unit) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val state = remember { SeekableTransitionState<Screen>(Screen.Home) }
    val transition = rememberTransition(state, label = "nav")
    val scope = rememberCoroutineScope()
    val navigator = remember { Navigator(stack) { target -> scope.launch { state.animateTo(target) } } }

    PredictiveBackHandler(enabled = stack.size > 1) { events ->
        val target = stack[stack.size - 2]
        try {
            events.collect { e -> state.seekTo(e.progress, target) }
            stack.removeAt(stack.lastIndex)
            scope.launch { state.animateTo(target) }
        } catch (e: CancellationException) {
            scope.launch { state.animateTo(stack.last()) }
            throw e
        }
    }

    SharedTransitionLayout(Modifier.fillMaxSize()) {
        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { spec() },
        ) { screen ->
            CompositionLocalProvider(
                LocalShared provides this@SharedTransitionLayout,
                LocalNavScope provides this@AnimatedContent,
            ) {
                Box(Modifier.fillMaxSize()) { content(screen, navigator) }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Screen>.spec(): ContentTransform {
    val forward = targetState.depth > initialState.depth
    val slide = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 420f)
    val transform = when {
        targetState is Screen.Settings || initialState is Screen.Settings -> if (forward) {
            (slideInHorizontally(slide) { it / 3 } + fadeIn(tween(220)))
                .togetherWith(slideOutHorizontally(slide) { -it / 6 } + fadeOut(tween(160)))
        } else {
            (slideInHorizontally(slide) { -it / 6 } + fadeIn(tween(220)))
                .togetherWith(slideOutHorizontally(slide) { it / 3 } + fadeOut(tween(160)))
        }
        targetState is Screen.Editor && !(targetState as Screen.Editor).fromFab && forward ->
            (slideInVertically(slide) { it / 8 } + fadeIn(tween(240)))
                .togetherWith(scaleOut(tween(300, easing = Motion.emphasized), 0.94f) + fadeOut(tween(200)))
        initialState is Screen.Editor && !(initialState as Screen.Editor).fromFab && !forward ->
            (scaleIn(tween(300, easing = Motion.emphasized), 0.94f) + fadeIn(tween(200)))
                .togetherWith(slideOutVertically(slide) { it / 8 } + fadeOut(tween(180)))
        forward -> fadeIn(tween(260, delayMillis = 40))
            .togetherWith(scaleOut(tween(380, easing = Motion.emphasized), 0.94f) + fadeOut(tween(260)))
        else -> (scaleIn(tween(380, easing = Motion.emphasized), 0.94f) + fadeIn(tween(260)))
            .togetherWith(fadeOut(tween(180)))
    }
    transform.targetContentZIndex = if (forward) 1f else 0f
    return transform
}
