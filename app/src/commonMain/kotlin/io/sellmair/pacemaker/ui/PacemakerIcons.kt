package io.sellmair.pacemaker.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Compose-native copies of the small, stroked marks in design/icons/ui. */
object PacemakerIcons {
    val Mark: ImageVector = ImageVector.Builder(
        name = "PacemakerMark",
        defaultWidth = 24.dp,
        defaultHeight = 14.6.dp,
        viewportWidth = 664f,
        viewportHeight = 404f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString("M0 130H140L292 0H340L468 170L520 130H664V184H540L480 234H434L307 68L167 184H0Z")
                .toNodes(),
            fill = SolidColor(Color.Black)
        )
        addPath(
            pathData = PathParser().parsePathString("M0 300H140L292 170H340L468 340L520 300H664V354H540L480 404H434L307 238L167 354H0Z")
                .toNodes(),
            fill = SolidColor(Color.Black)
        )
    }.build()

    val Rhythm = lineIcon("M2 9h4l4-4 5 6 3-2h4M2 15h4l4-4 5 6 3-2h4")
    val Settings: ImageVector = ImageVector.Builder(
        name = "PacemakerSettings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString("M4 7h16M4 17h16").toNodes(),
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        addPath(
            pathData = PathParser().parsePathString("M7 4h4v6H7z").toNodes(),
            fill = SolidColor(Color.Black)
        )
        addPath(
            pathData = PathParser().parsePathString("M14 14h4v6h-4z").toNodes(),
            fill = SolidColor(Color.Black)
        )
    }.build()
    val Group = lineIcon(
        "M3 20v-2a6 6 0 0 1 12 0v2M16 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 4v2",
        "M12 8a3 3 0 1 1-6 0a3 3 0 0 1 6 0"
    )
    val Sensor = lineIcon("M6 7h12v10H6zM2 9v6M22 9v6M9 12h6")
    val Watch = lineIcon("M6 6h12v12H6zM9 2h6l1 4M9 22h6l1-4M8 6l1-4M8 18l1 4M12 9v3l2 1")
    val Audio = lineIcon("M4 9h4l4-4v14l-4-4H4ZM16 8a6 6 0 0 1 0 8M19 5a10 10 0 0 1 0 14")
    val AudioOff = lineIcon("M4 9h4l4-4v14l-4-4H4ZM17 9l5 6M22 9l-5 6")
    val Limit = lineIcon("M3 7h18M3 17h18M8 4v6M16 14v6")
    val Warning = lineIcon("M12 3 2 21h20ZM12 9v5M12 17v1")
    val Disconnect = lineIcon("m3 3 18 18M8 6l3-3a5 5 0 0 1 7 7l-1 1M7 13l-1 1a4 4 0 0 0 6 6l2-2")
    val Info = lineIcon("M12 11v6M12 7v1", "M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0")
    val Check = lineIcon("m5 12 4 4L19 6")
    val Plus = lineIcon("M12 4v16M4 12h16")
    val Minus = lineIcon("M4 12h16")

    private fun lineIcon(path: String, additionalPaths: String? = null): ImageVector =
        ImageVector.Builder(
            name = "PacemakerIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            additionalPaths?.let {
                addPath(
                    pathData = PathParser().parsePathString(it).toNodes(),
                    fill = SolidColor(Color.Transparent),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
}
