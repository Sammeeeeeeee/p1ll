package io.github.sammeeeeeeee.p1ll.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class Tone(val light: Color, val dark: Color) {
    fun pick(dark: Boolean) = if (dark) this.dark else light
}

@Immutable
data class Accent(val name: String, val tone: Tone)

/** Pill colours, tuned in pairs so each reads well on both backgrounds. */
val PillColors = listOf(
    Tone(Color(0xFFF2654F), Color(0xFFFF7D69)),
    Tone(Color(0xFFEC962A), Color(0xFFFFB347)),
    Tone(Color(0xFFD4AC17), Color(0xFFF0CF4A)),
    Tone(Color(0xFF2CA86F), Color(0xFF4DD597)),
    Tone(Color(0xFF338EEA), Color(0xFF5DADFF)),
    Tone(Color(0xFF6A5DE8), Color(0xFF928AFF)),
    Tone(Color(0xFFE0519A), Color(0xFFFF7AB8)),
    Tone(Color(0xFF66707C), Color(0xFF9CA5B0)),
)

fun pillColor(index: Int, dark: Boolean): Color = PillColors[index.mod(PillColors.size)].pick(dark)

/** Chart colours. */
val Accents = listOf(
    Accent("Moss", Tone(Color(0xFF1E9A55), Color(0xFF3FD07A))),
    Accent("Ocean", Tone(Color(0xFF2A6DE6), Color(0xFF5E9DFF))),
    Accent("Ember", Tone(Color(0xFFE5602A), Color(0xFFFF8A4C))),
    Accent("Orchid", Tone(Color(0xFFAE48CE), Color(0xFFD27BF0))),
    Accent("Rose", Tone(Color(0xFFDD4479), Color(0xFFFF6F9C))),
    Accent("Ink", Tone(Color(0xFF1A1A1E), Color(0xFFF1F0EC))),
)

@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    /** A chart day with doses, none taken yet. */
    val cell: Color,
    /** A chart day with nothing scheduled. */
    val cellVoid: Color,
    val accent: Color,
    val onAccent: Color,
    val warn: Color,
    val danger: Color,
    val dark: Boolean,
)

fun onColor(c: Color): Color = if (c.luminance() > 0.45f) Color(0xFF111113) else Color.White

fun lightPalette(accent: Color) = Palette(
    bg = Color(0xFFF3F2EE),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFEAE8E3),
    line = Color(0xFFE2E0DA),
    ink = Color(0xFF151518),
    ink2 = Color(0xFF6C6A66),
    ink3 = Color(0xFFA7A49D),
    cell = Color(0xFFE4E2DC),
    cellVoid = Color(0xFFECEAE5),
    accent = accent,
    onAccent = onColor(accent),
    warn = Color(0xFFCF7A22),
    danger = Color(0xFFD14343),
    dark = false,
)

fun darkPalette(accent: Color) = Palette(
    bg = Color(0xFF0D0D0F),
    surface = Color(0xFF18181B),
    raised = Color(0xFF232327),
    line = Color(0xFF2A2A2E),
    ink = Color(0xFFF2F1EE),
    ink2 = Color(0xFF9D9B96),
    ink3 = Color(0xFF66645F),
    cell = Color(0xFF27272B),
    cellVoid = Color(0xFF1C1C1F),
    accent = accent,
    onAccent = onColor(accent),
    warn = Color(0xFFF0A04B),
    danger = Color(0xFFFF6B6B),
    dark = true,
)
