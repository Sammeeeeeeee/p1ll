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
private fun fredoka(weight: Int) = Font(
    R.font.fredoka,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(100f)),
)

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Rounded and bubbly: headings, names and big numbers. */
val Display = FontFamily(fredoka(400), fredoka(500), fredoka(600), fredoka(700))

/** Rounded and calm, with tabular digits by default: everything you read. */
val Body = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800), nunito(900))

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

object Type {
    val hero = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 112.sp,
        lineHeight = 104.sp,
        lineHeightStyle = trim,
    )
    val number = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        lineHeightStyle = trim,
    )
    val title = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    )
    val headline = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )
    val body = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    )
    val bodyStrong = body.copy(fontWeight = FontWeight.Bold)
    val label = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    val small = label.copy(fontSize = 12.5.sp, lineHeight = 17.sp)
    /** Clock times: heavy Nunito, whose digits are all the same width so columns line up. */
    val time = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    )
}

val M3Type = Typography().let { t ->
    fun TextStyle.body() = copy(fontFamily = Body)
    fun TextStyle.display() = copy(fontFamily = Display)
    Typography(
        displayLarge = t.displayLarge.display(),
        displayMedium = t.displayMedium.display(),
        displaySmall = t.displaySmall.display(),
        headlineLarge = t.headlineLarge.display(),
        headlineMedium = t.headlineMedium.display(),
        headlineSmall = t.headlineSmall.display(),
        titleLarge = t.titleLarge.display(),
        titleMedium = t.titleMedium.body(),
        titleSmall = t.titleSmall.body(),
        bodyLarge = t.bodyLarge.body(),
        bodyMedium = t.bodyMedium.body(),
        bodySmall = t.bodySmall.body(),
        labelLarge = t.labelLarge.body(),
        labelMedium = t.labelMedium.body(),
        labelSmall = t.labelSmall.body(),
    )
}
