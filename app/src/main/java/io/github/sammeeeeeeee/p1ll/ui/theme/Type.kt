package io.github.sammeeeeeeee.p1ll.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import io.github.sammeeeeeeee.p1ll.R

/** Red Hat Display: headings, names and big numbers. Static instances, cut by tools/fonts.py. */
val Display = FontFamily(
    Font(R.font.redhat_display_500, FontWeight.Medium),
    Font(R.font.redhat_display_650, FontWeight(650)),
)

/** Between SemiBold and Bold: where Red Hat Display looks most settled at heading sizes. */
private val Heading = FontWeight(650)

/** Red Hat Text: everything you read. */
val Body = FontFamily(
    Font(R.font.redhat_text_400, FontWeight.Normal),
    Font(R.font.redhat_text_500, FontWeight.Medium),
    Font(R.font.redhat_text_600, FontWeight.SemiBold),
)

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

object Type {
    val hero = TextStyle(
        fontFamily = Display,
        fontWeight = Heading,
        fontSize = 108.sp,
        lineHeight = 100.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = trim,
    )
    val number = TextStyle(
        fontFamily = Display,
        fontWeight = Heading,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = trim,
    )
    val title = TextStyle(
        fontFamily = Display,
        fontWeight = Heading,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    )
    val headline = TextStyle(
        fontFamily = Display,
        fontWeight = Heading,
        fontSize = 18.sp,
        lineHeight = 23.sp,
    )
    val body = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    )
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val label = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = "tnum",
    )
    val small = label.copy(fontSize = 12.5.sp, lineHeight = 17.sp)
    /** Clock times, with tabular digits so the timeline's column lines up. */
    val time = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
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
