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

/** Pill colours, tuned in pairs so each reads well on both backgrounds. Pills store the index, so new ones go on the end. */
val PillColors = listOf(
    Tone(Color(0xFFD9663A), Color(0xFFF08A63)), // terracotta
    Tone(Color(0xFFD29A26), Color(0xFFEAB850)), // ochre
    Tone(Color(0xFF1F8F6A), Color(0xFF45C795)), // emerald
    Tone(Color(0xFF12808A), Color(0xFF3CBCC6)), // teal
    Tone(Color(0xFF4D6FA8), Color(0xFF7E9BD0)), // denim
    Tone(Color(0xFF8656C9), Color(0xFFAC8BEA)), // violet
    Tone(Color(0xFFC8507A), Color(0xFFEA7EA3)), // rose
    Tone(Color(0xFF6B6F78), Color(0xFF9EA2AB)), // graphite
    Tone(Color(0xFFC83C3C), Color(0xFFEC5A5A)), // red
    Tone(Color(0xFF6C9A24), Color(0xFF9CCB55)), // lime
    Tone(Color(0xFF2A86C8), Color(0xFF63B4EC)), // sky
    Tone(Color(0xFF8C5E3C), Color(0xFFC89670)), // cocoa
)

/** [PillColors] indices in picker order, round the colour wheel. */
val PillColorOrder = listOf(8, 0, 11, 1, 9, 2, 3, 10, 4, 5, 6, 7)

fun pillColor(index: Int, dark: Boolean): Color = PillColors[index.mod(PillColors.size)].pick(dark)

/** The app colour: chart squares, buttons, switches, the now line. */
val Accents = listOf(
    Accent("Emerald", Tone(Color(0xFF1C8A63), Color(0xFF3FCB93))),
    Accent("Terracotta", Tone(Color(0xFFCF5F36), Color(0xFFF08A63))),
    Accent("Teal", Tone(Color(0xFF0F7F88), Color(0xFF3CBCC6))),
    Accent("Violet", Tone(Color(0xFF7A4FC4), Color(0xFFA585EA))),
    Accent("Rose", Tone(Color(0xFFBF4A73), Color(0xFFEA7EA3))),
    Accent("Ochre", Tone(Color(0xFFB9831A), Color(0xFFEAB850))),
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

fun onColor(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF15171E) else Color.White

fun lightPalette(accent: Color) = Palette(
    bg = Color(0xFFF2F0EC),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFE7E4DE),
    line = Color(0xFFE0DCD5),
    ink = Color(0xFF15171E),
    ink2 = Color(0xFF6E7079),
    ink3 = Color(0xFFABADB4),
    cell = Color(0xFFEAE7E1),
    cellVoid = Color(0xFFF3F1ED),
    accent = accent,
    onAccent = onColor(accent),
    accentSoft = accent.copy(alpha = 0.1f).compositeOver(Color(0xFFF2F0EC)),
    warn = Color(0xFFD27A1E),
    danger = Color(0xFFD14545),
    dark = false,
)

fun darkPalette(accent: Color) = Palette(
    bg = Color(0xFF121317),
    surface = Color(0xFF1C1D23),
    raised = Color(0xFF27282F),
    line = Color(0xFF2F3038),
    ink = Color(0xFFF2F2F4),
    ink2 = Color(0xFFA0A2AB),
    ink3 = Color(0xFF64666F),
    cell = Color(0xFF2A2B32),
    cellVoid = Color(0xFF202127),
    accent = accent,
    onAccent = onColor(accent),
    accentSoft = accent.copy(alpha = 0.14f).compositeOver(Color(0xFF121317)),
    warn = Color(0xFFF0A04B),
    danger = Color(0xFFFF6B6B),
    dark = true,
)
