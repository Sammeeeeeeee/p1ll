package io.github.sammeeeeeeee.p1ll.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

@Immutable
data class Tone(val light: Color, val dark: Color) {
    fun pick(dark: Boolean) = if (dark) this.dark else light
}

@Immutable
data class Accent(val name: String, val tone: Tone)

/** Pill colours, tuned in pairs so each reads well on both backgrounds. */
val PillColors = listOf(
    Tone(Color(0xFFFF6B57), Color(0xFFFF8A78)), // coral
    Tone(Color(0xFFFF9F1C), Color(0xFFFFB84D)), // tangerine
    Tone(Color(0xFFF2C12E), Color(0xFFFFD95A)), // sunshine
    Tone(Color(0xFF2EC27E), Color(0xFF5BE0A0)), // mint
    Tone(Color(0xFF3D8BFF), Color(0xFF6FAAFF)), // sky
    Tone(Color(0xFF7C5CFF), Color(0xFFA08BFF)), // grape
    Tone(Color(0xFFFF5FA2), Color(0xFFFF8CC0)), // bubblegum
    Tone(Color(0xFF14B8B0), Color(0xFF4FDCD3)), // lagoon
)

fun pillColor(index: Int, dark: Boolean): Color = PillColors[index.mod(PillColors.size)].pick(dark)

/** The app colour: chart squares, buttons, switches, the now line. */
val Accents = listOf(
    Accent("Grass", Tone(Color(0xFF1FAA5C), Color(0xFF4ADE8A))),
    Accent("Sky", Tone(Color(0xFF2F7BFF), Color(0xFF6AA6FF))),
    Accent("Tangerine", Tone(Color(0xFFFF7A1A), Color(0xFFFF9B52))),
    Accent("Grape", Tone(Color(0xFF7A52FF), Color(0xFFA48CFF))),
    Accent("Bubblegum", Tone(Color(0xFFF24A92), Color(0xFFFF7DB8))),
    Accent("Lagoon", Tone(Color(0xFF0FA89F), Color(0xFF3FD9CF))),
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
    /** Accent washed into the background, for soft coloured panels. */
    val accentSoft: Color,
    val warn: Color,
    val danger: Color,
    val dark: Boolean,
)

fun onColor(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF2B2140) else Color.White

fun lightPalette(accent: Color) = Palette(
    bg = Color(0xFFFDF4EA),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFF5EADC),
    line = Color(0xFFEEDFCD),
    ink = Color(0xFF2B2140),
    ink2 = Color(0xFF7B6E8C),
    ink3 = Color(0xFFB9ADC2),
    cell = Color(0xFFF0E6DA),
    cellVoid = Color(0xFFF7F0E7),
    accent = accent,
    onAccent = onColor(accent),
    accentSoft = accent.copy(alpha = 0.12f).compositeOver(Color(0xFFFDF4EA)),
    warn = Color(0xFFF08A1C),
    danger = Color(0xFFE5484D),
    dark = false,
)

fun darkPalette(accent: Color) = Palette(
    bg = Color(0xFF16121F),
    surface = Color(0xFF221C2E),
    raised = Color(0xFF2D263B),
    line = Color(0xFF372F47),
    ink = Color(0xFFF8F2FB),
    ink2 = Color(0xFFADA2BC),
    ink3 = Color(0xFF6F657D),
    cell = Color(0xFF30293D),
    cellVoid = Color(0xFF282233),
    accent = accent,
    onAccent = onColor(accent),
    accentSoft = accent.copy(alpha = 0.16f).compositeOver(Color(0xFF16121F)),
    warn = Color(0xFFFFA94D),
    danger = Color(0xFFFF6B6E),
    dark = true,
)
