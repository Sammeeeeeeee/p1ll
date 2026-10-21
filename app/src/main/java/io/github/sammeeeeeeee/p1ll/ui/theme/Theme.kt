package io.github.sammeeeeeeee.p1ll.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.sammeeeeeeee.p1ll.data.Prefs
import io.github.sammeeeeeeee.p1ll.data.ThemeMode

val LocalPalette = staticCompositionLocalOf { lightPalette(Accents[0].tone.light) }

object P1ll {
    val colors: Palette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
}

/** Shared motion vocabulary: springs for anything touched, eased tweens for anything that just changes. */
object Motion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    fun <T> snappy(): AnimationSpec<T> = spring(dampingRatio = 0.74f, stiffness = 700f)
    fun <T> gentle(): AnimationSpec<T> = spring(dampingRatio = 0.92f, stiffness = 260f)
    fun <T> bouncy(): AnimationSpec<T> = spring(dampingRatio = 0.5f, stiffness = 480f)
    fun <T> soft(): AnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 140f)
}

@Composable
fun isDark(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
private fun animate(target: Color) = animateColorAsState(target, tween(450, easing = Motion.emphasized), label = "palette").value

@Composable
fun P1llTheme(prefs: Prefs = Prefs(), content: @Composable () -> Unit) {
    val dark = isDark(prefs.theme)
    val accent = Accents[prefs.accent.mod(Accents.size)].tone.pick(dark)
    val target = if (dark) darkPalette(accent) else lightPalette(accent)
    val palette = target.copy(
        bg = animate(target.bg),
        surface = animate(target.surface),
        raised = animate(target.raised),
        line = animate(target.line),
        ink = animate(target.ink),
        ink2 = animate(target.ink2),
        ink3 = animate(target.ink3),
        cell = animate(target.cell),
        cellVoid = animate(target.cellVoid),
        accent = animate(target.accent),
        onAccent = animate(target.onAccent),
        accentSoft = animate(target.accentSoft),
    )
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        primaryContainer = palette.accentSoft,
        onPrimaryContainer = palette.ink,
        secondary = palette.ink2,
        secondaryContainer = palette.raised,
        onSecondaryContainer = palette.ink,
        tertiary = palette.accent,
        background = palette.bg,
        onBackground = palette.ink,
        surface = palette.surface,
        onSurface = palette.ink,
        surfaceVariant = palette.raised,
        onSurfaceVariant = palette.ink2,
        surfaceContainerLowest = palette.surface,
        surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surface,
        surfaceContainerHigh = palette.surface,
        surfaceContainerHighest = palette.raised,
        outline = palette.ink3,
        outlineVariant = palette.line,
        error = palette.danger,
    )
    MaterialTheme(colorScheme = scheme, typography = M3Type) {
        CompositionLocalProvider(
            LocalPalette provides palette,
            LocalContentColor provides palette.ink,
            LocalTextStyle provides Type.body.copy(color = palette.ink),
            LocalTextSelectionColors provides TextSelectionColors(palette.accent, palette.accent.copy(alpha = 0.25f)),
            content = content,
        )
    }
}
