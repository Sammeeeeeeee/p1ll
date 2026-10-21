package io.github.sammeeeeeeee.p1ll.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** One consistent hand-drawn set: 24-unit grid, a 2.0 stroke, round ends. */
object Icons {
    val Plus by lazy { icon("M12 5v14M5 12h14") }
    val Minus by lazy { icon("M5.5 12h13") }
    val Close by lazy { icon("M6.5 6.5l11 11M17.5 6.5l-11 11") }
    val Check by lazy { icon("M5 12.5l4.5 4.5L19 7.5", stroke = 2.4f) }
    val Back by lazy { icon("M14.5 5.5L8 12l6.5 6.5") }
    val ChevronRight by lazy { icon("M9.5 5.5L16 12l-6.5 6.5") }
    val ChevronDown by lazy { icon("M6.5 9.5l5.5 5.5 5.5-5.5") }
    val Sliders by lazy {
        icon(
            "M4 8h9.5M18.5 8H20M4 16h2.5M11.5 16H20",
            "M16 8m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0",
            "M9 16m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0",
        )
    }
    val Trash by lazy {
        icon("M4.5 7h15M9.5 7V5.5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1V7M6.5 7l.8 11.2a1.5 1.5 0 0 0 1.5 1.3h6.4a1.5 1.5 0 0 0 1.5-1.3L17.5 7")
    }
    val Pencil by lazy { icon("M14 6l4 4M4.5 19.5l1-4.2L15.3 5.5a1.4 1.4 0 0 1 2 0l1.2 1.2a1.4 1.4 0 0 1 0 2L8.7 18.5z") }
    val Skip by lazy { icon("M6 6.5l5.5 5.5L6 17.5M13 6.5l5.5 5.5-5.5 5.5") }
    val Undo by lazy { icon("M9 14L4.5 9.5 9 5M4.5 9.5H14a5 5 0 0 1 0 10h-2") }
    val Clock by lazy { icon("M12 12m-8 0a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "M12 7.5V12l3 2") }
    val Calendar by lazy { icon("M5 7.5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2zM5 10h14M9 3.5v3M15 3.5v3") }
    val Bell by lazy { icon("M6.5 16.5V11a5.5 5.5 0 0 1 11 0v5.5l1.5 1.5h-14zM10 20.5h4") }
    val Box by lazy { icon("M4.5 8L12 4l7.5 4v8L12 20l-7.5-4zM4.5 8L12 12l7.5-4M12 12v8") }
    val Note by lazy { icon("M6.5 4.5h7l4 4v11h-11zM13.5 4.5v4h4M9 13h6M9 16.5h4") }
    val Flag by lazy { icon("M6 20.5V4.5M6 5h10.5l-2 3.5 2 3.5H6") }
}

private fun icon(vararg paths: String, stroke: Float = 2f): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
