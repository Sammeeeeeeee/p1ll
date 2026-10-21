package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** Every scrollable in the app bounces at its edges instead of stretching. */
object BounceOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = BounceOverscroll()
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 0x0b0c
}

/**
 * Past an edge the content follows the finger with rising resistance; let go and it springs
 * back. A fling that hits an edge carries a little of its speed into the same spring.
 */
private class BounceOverscroll : OverscrollEffect {
    private var offset by mutableStateOf(Offset.Zero)
    private var limit = Offset(1f, 1f)

    override val isInProgress: Boolean get() = offset != Offset.Zero

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        // Reversing direction first unwinds the overscroll, then scrolls the content.
        val relaxed = Offset(relax(delta.x, offset.x), relax(delta.y, offset.y))
        offset += relaxed
        val left = delta - relaxed
        val consumed = performScroll(left)
        val over = left - consumed
        if (source == NestedScrollSource.UserInput && (abs(over.x) > 0.5f || abs(over.y) > 0.5f)) {
            offset = Offset(pull(offset.x, over.x, limit.x), pull(offset.y, over.y, limit.y))
        }
        return relaxed + consumed
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        if (offset != Offset.Zero) {
            settle(velocity)
            return
        }
        val leftover = velocity - performFling(velocity)
        // Hitting an edge at speed: a small rebound proportional to what was left.
        settle(leftover * 0.12f)
    }

    private suspend fun settle(velocity: Velocity) {
        val spec = spring<Float>(dampingRatio = 0.62f, stiffness = 380f)
        val start = offset
        if (start == Offset.Zero && velocity == Velocity.Zero) return
        var x = start.x
        var y = start.y
        coroutineScope {
            launch { animate(start.x, 0f, velocity.x, spec) { v, _ -> x = v; offset = Offset(x, y) } }
            launch { animate(start.y, 0f, velocity.y, spec) { v, _ -> y = v; offset = Offset(x, y) } }
        }
        offset = Offset.Zero
    }

    /** The part of [delta] spent unwinding [current] when they point opposite ways. */
    private fun relax(delta: Float, current: Float): Float {
        if (current == 0f || delta == 0f || sign(delta) == sign(current)) return 0f
        return if (abs(delta) >= abs(current)) -current else delta
    }

    private fun pull(current: Float, over: Float, max: Float): Float {
        val resistance = 0.55f * (1f - (abs(current) / max).coerceIn(0f, 1f))
        return current + over * resistance
    }

    override val node: DelegatableNode = object : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val placeable = measurable.measure(constraints)
            limit = Offset(placeable.width * 0.3f, placeable.height * 0.22f).let {
                Offset(it.x.coerceAtLeast(1f), it.y.coerceAtLeast(1f))
            }
            return layout(placeable.width, placeable.height) {
                placeable.placeRelativeWithLayer(offset.x.roundToInt(), offset.y.roundToInt())
            }
        }
    }
}
