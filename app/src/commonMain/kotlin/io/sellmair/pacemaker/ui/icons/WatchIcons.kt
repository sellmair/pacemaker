package io.sellmair.pacemaker.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A wrist-worn heart-rate source, matching the rounded sensor icon outlines. */
internal object WatchIcons {
    val Watch: ImageVector by lazy {
        ImageVector.Builder(
            name = "HeartRateWatch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Tapered straps meet the case without crossing its outline.
                moveTo(8.25f, 5f)
                lineTo(8.8f, 2.8f)
                curveTo(8.92f, 2.33f, 9.35f, 2f, 9.84f, 2f)
                lineTo(14.16f, 2f)
                curveTo(14.65f, 2f, 15.08f, 2.33f, 15.2f, 2.8f)
                lineTo(15.75f, 5f)
                moveTo(8.25f, 19f)
                lineTo(8.8f, 21.2f)
                curveTo(8.92f, 21.67f, 9.35f, 22f, 9.84f, 22f)
                lineTo(14.16f, 22f)
                curveTo(14.65f, 22f, 15.08f, 21.67f, 15.2f, 21.2f)
                lineTo(15.75f, 19f)

                // Rounded case.
                moveTo(8.5f, 5f)
                lineTo(15.5f, 5f)
                curveTo(17.29f, 5f, 18.75f, 6.46f, 18.75f, 8.25f)
                lineTo(18.75f, 15.75f)
                curveTo(18.75f, 17.54f, 17.29f, 19f, 15.5f, 19f)
                lineTo(8.5f, 19f)
                curveTo(6.71f, 19f, 5.25f, 17.54f, 5.25f, 15.75f)
                lineTo(5.25f, 8.25f)
                curveTo(5.25f, 6.46f, 6.71f, 5f, 8.5f, 5f)
                close()

                // Crown and heart-rate trace stay distinct at small sizes.
                moveTo(20.25f, 10.75f)
                lineTo(20.25f, 13.25f)
                moveTo(8f, 12f)
                lineTo(9.75f, 12f)
                lineTo(11f, 9.5f)
                lineTo(13f, 14.5f)
                lineTo(14.25f, 12f)
                lineTo(16f, 12f)
            }
        }.build()
    }
}
