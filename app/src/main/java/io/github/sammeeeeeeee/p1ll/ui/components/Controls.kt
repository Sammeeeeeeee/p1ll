package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import io.github.sammeeeeeeee.p1ll.ui.feedback.LocalFeedback
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import io.github.sammeeeeeeee.p1ll.ui.theme.Type
import kotlinx.coroutines.flow.collectLatest

/**
 * Click without a ripple: the element dips under the finger and springs back. A quick tap
 * still gets a visible dip, because the release waits for the press to land first.
 */
@Composable
fun Modifier.tappable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interaction, pressedScale) {
        interaction.interactions.collectLatest { event ->
            when (event) {
                is PressInteraction.Press -> scale.animateTo(pressedScale, spring(1f, 1400f))
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    if (scale.value > pressedScale + 0.01f) scale.animateTo(pressedScale, tween(70))
                    scale.animateTo(1f, spring(0.42f, 520f))
                }
            }
        }
    }
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    color: Color = P1ll.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(color), content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = P1ll.colors.ink2) {
    Text(text, style = Type.label.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold), color = color, modifier = modifier)
}

@Composable
fun CircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = P1ll.colors.raised,
    tint: Color = P1ll.colors.ink,
) {
    Box(
        modifier
            .size(size)
            .tappable(pressedScale = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    background: Color = P1ll.colors.ink,
    content: Color = P1ll.colors.bg,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.32f, tween(220), label = "enabled")
    Row(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .tappable(enabled = enabled, pressedScale = 0.94f, onClick = onClick)
            .clip(CircleShape)
            .background(background)
            .height(48.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.bodyStrong, color = content)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = P1ll.colors.ink,
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .tappable(pressedScale = 0.94f, onClick = onClick)
            .clip(CircleShape)
            .background(P1ll.colors.raised)
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.label, color = color)
    }
}

/** Equal-width options with a sliding, springing thumb. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedback.current
    val c = P1ll.colors
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.raised)
            .padding(4.dp),
    ) {
        val each = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(each * index, spring(0.72f, 520f), label = "thumb")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(each)
                .height(38.dp)
                .shadow(if (c.dark) 0.dp else 1.5.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(12.dp))
                .background(if (c.dark) c.line else c.surface),
        )
        Row(Modifier.fillMaxWidth().height(38.dp)) {
            options.forEach { option ->
                val active = option == selected
                val color by animateColorAsState(if (active) c.ink else c.ink2, tween(200), label = "seg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .combinedClickable(interactionSource = null, indication = null) {
                            if (!active) {
                                feedback.tick()
                                onSelect(option)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(option), style = Type.label, color = color, maxLines = 1)
                }
            }
        }
    }
}

/** A switch whose thumb stretches as it travels. */
@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = P1ll.colors
    val feedback = LocalFeedback.current
    val track by animateColorAsState(if (checked) c.ink else c.raised, tween(220), label = "track")
    val progress by animateFloatAsState(if (checked) 1f else 0f, spring(0.62f, 420f), label = "thumb")
    val stretch = 1f - kotlin.math.abs(progress - 0.5f) * 2f
    Box(
        modifier
            .size(52.dp, 32.dp)
            .tappable(pressedScale = 0.92f) {
                feedback.tick()
                onChange(!checked)
            }
            .clip(CircleShape)
            .background(track)
            .padding(4.dp),
    ) {
        val thumbW = 24.dp + 8.dp * stretch.coerceIn(0f, 1f)
        Box(
            Modifier
                .offset { IntOffset(((44.dp - thumbW) * progress).roundToPx(), 0) }
                .size(thumbW, 24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(if (checked) c.bg else c.surface),
        )
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = P1ll.colors
    val bg by animateColorAsState(if (selected) c.ink else c.raised, tween(200), label = "chip")
    val fg by animateColorAsState(if (selected) c.bg else c.ink, tween(200), label = "chipText")
    Row(
        modifier
            .tappable(pressedScale = 0.92f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .height(40.dp)
            .widthIn(min = 40.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Type.label, color = fg)
        trailing?.invoke(this)
    }
}

/** Digits that roll individually: up when the number grows, down when it shrinks. */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = P1ll.colors.ink,
) {
    val text = value.toString()
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            val place = text.length - i
            androidx.compose.runtime.key(place) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        val up = targetState > initialState
                        val spec = spring<androidx.compose.ui.unit.IntOffset>(0.78f, 320f)
                        (slideInVertically(spec) { if (up) it else -it } + fadeIn(tween(160)))
                            .togetherWith(slideOutVertically(spec) { if (up) -it else it } + fadeOut(tween(120)))
                            .using(SizeTransform(clip = true))
                    },
                    label = "digit",
                ) { digit -> Text(digit.toString(), style = style, color = color) }
            }
        }
    }
}

@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    val c = P1ll.colors
    val feedback = LocalFeedback.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleButton(Icons.Minus, "Less", {
            if (value > range.first) { feedback.tick(); onChange(value - 1) }
        }, size = 36.dp)
        Row(Modifier.widthIn(min = 44.dp).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.Center) {
            RollingNumber(value, Type.headline)
            if (suffix != null) Text(" $suffix", style = Type.headline, color = c.ink2)
        }
        CircleButton(Icons.Plus, "More", {
            if (value < range.last) { feedback.tick(); onChange(value + 1) }
        }, size = 36.dp)
    }
}

@Composable
fun SettingRow(label: String, modifier: Modifier = Modifier, hint: String? = null, trailing: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.bodyStrong)
            if (hint != null) Text(hint, style = Type.label, color = P1ll.colors.ink2)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(P1ll.colors.line))
}
