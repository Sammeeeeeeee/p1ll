package io.github.sammeeeeeeee.p1ll.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import io.github.sammeeeeeeee.p1ll.R

@OptIn(ExperimentalTextApi::class)
private fun instrument(weight: Int, width: Float = 100f) = Font(
    R.font.instrument_sans,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

val Instrument = FontFamily(instrument(400), instrument(500), instrument(600), instrument(700))

/** The narrow cut, for big numbers. */
val InstrumentNarrow = FontFamily(instrument(600, 75f), instrument(700, 75f))

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

object Type {
    val hero = TextStyle(
        fontFamily = InstrumentNarrow,
        fontWeight = FontWeight.Bold,
        fontSize = 104.sp,
        lineHeight = 96.sp,
        letterSpacing = (-3).sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = trim,
    )
    val number = TextStyle(
        fontFamily = InstrumentNarrow,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = trim,
    )
    val title = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp,
    )
    val headline = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp,
    )
    val body = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val bodyStrong = body.copy(fontWeight = FontWeight.Medium)
    val label = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.sp,
        fontFeatureSettings = "tnum",
    )
    val small = label.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val caps = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.3.sp,
    )
}

val M3Type = Typography().let { t ->
    fun TextStyle.i() = copy(fontFamily = Instrument)
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = InstrumentNarrow, fontWeight = FontWeight.Bold),
        displayMedium = t.displayMedium.copy(fontFamily = InstrumentNarrow, fontWeight = FontWeight.Bold),
        displaySmall = t.displaySmall.i(),
        headlineLarge = t.headlineLarge.i(),
        headlineMedium = t.headlineMedium.i(),
        headlineSmall = t.headlineSmall.i(),
        titleLarge = t.titleLarge.i(),
        titleMedium = t.titleMedium.i(),
        titleSmall = t.titleSmall.i(),
        bodyLarge = t.bodyLarge.i(),
        bodyMedium = t.bodyMedium.i(),
        bodySmall = t.bodySmall.i(),
        labelLarge = t.labelLarge.i(),
        labelMedium = t.labelMedium.i(),
        labelSmall = t.labelSmall.i(),
    )
}
