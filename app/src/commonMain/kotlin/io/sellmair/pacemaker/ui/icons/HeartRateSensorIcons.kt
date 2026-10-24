package io.sellmair.pacemaker.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Chest-strap sensor icons sharing a 24 dp grid and a tintable, rounded outline. */
internal object HeartRateSensorIcons {
    val Sensor: ImageVector by lazy { sensorIcon("HeartRateSensor", withBadge = false) }

    val Searching: ImageVector by lazy {
        sensorIcon("HeartRateSensorSearching") {
            // Magnifying glass.
            moveTo(20f, 16.25f)
            curveTo(20f, 17.91f, 18.66f, 19.25f, 17f, 19.25f)
            curveTo(15.34f, 19.25f, 14f, 17.91f, 14f, 16.25f)
            curveTo(14f, 14.59f, 15.34f, 13.25f, 17f, 13.25f)
            curveTo(18.66f, 13.25f, 20f, 14.59f, 20f, 16.25f)
            close()
            moveTo(19.2f, 18.45f)
            lineTo(21.5f, 20.75f)
        }
    }

    val Connecting: ImageVector by lazy {
        sensorIcon("HeartRateSensorConnecting") {
            // Two arrows around an open ring.
            moveTo(21.3f, 15.75f)
            curveTo(20.6f, 14.1f, 18.65f, 13.2f, 16.9f, 13.85f)
            curveTo(15.55f, 14.35f, 14.6f, 15.5f, 14.25f, 16.8f)
            moveTo(14.1f, 14.5f)
            lineTo(14.25f, 16.8f)
            lineTo(16.55f, 16.65f)
            moveTo(14.7f, 18.25f)
            curveTo(15.4f, 19.9f, 17.35f, 20.8f, 19.1f, 20.15f)
            curveTo(20.45f, 19.65f, 21.4f, 18.5f, 21.75f, 17.2f)
            moveTo(19.45f, 17.35f)
            lineTo(21.75f, 17.2f)
            lineTo(21.9f, 19.5f)
        }
    }

    val Connected: ImageVector by lazy {
        sensorIcon("HeartRateSensorConnected") {
            statusRing()
            moveTo(15.9f, 17f)
            lineTo(17.35f, 18.45f)
            lineTo(20.15f, 15.65f)
        }
    }

    val Disconnecting: ImageVector by lazy {
        sensorIcon("HeartRateSensorDisconnecting") {
            statusRing()
            moveTo(16f, 17f)
            lineTo(20f, 17f)
        }
    }

    val Disconnected: ImageVector by lazy {
        sensorIcon("HeartRateSensorDisconnected") {
            statusRing()
            moveTo(15.2f, 19.8f)
            lineTo(20.8f, 14.2f)
        }
    }
}

private fun sensorIcon(
    name: String,
    withBadge: Boolean = true,
    badge: PathBuilder.() -> Unit = {}
): ImageVector = ImageVector.Builder(
    name = name,
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
        // Central transmitter. Leave room for the status badge at its lower right.
        moveTo(17.5f, if (withBadge) 10.75f else 14.5f)
        lineTo(17.5f, 9.5f)
        curveTo(17.5f, 7.29f, 15.71f, 5.5f, 13.5f, 5.5f)
        lineTo(10.5f, 5.5f)
        curveTo(8.29f, 5.5f, 6.5f, 7.29f, 6.5f, 9.5f)
        lineTo(6.5f, 14.5f)
        curveTo(6.5f, 16.71f, 8.29f, 18.5f, 10.5f, 18.5f)
        lineTo(if (withBadge) 12f else 13.5f, 18.5f)
        if (!withBadge) {
            curveTo(15.71f, 18.5f, 17.5f, 16.71f, 17.5f, 14.5f)
        }

        // Strap ends.
        moveTo(6.5f, 8.5f)
        lineTo(4f, 8.5f)
        curveTo(3.17f, 8.5f, 2.5f, 9.17f, 2.5f, 10f)
        lineTo(2.5f, 14f)
        curveTo(2.5f, 14.83f, 3.17f, 15.5f, 4f, 15.5f)
        lineTo(6.5f, 15.5f)
        moveTo(17.5f, 8.5f)
        lineTo(20f, 8.5f)
        curveTo(20.83f, 8.5f, 21.5f, 9.17f, 21.5f, 10f)
        lineTo(21.5f, if (withBadge) 11f else 14f)
        if (!withBadge) {
            curveTo(21.5f, 14.83f, 20.83f, 15.5f, 20f, 15.5f)
            lineTo(17.5f, 15.5f)
        }

        // Heartbeat trace.
        moveTo(8.5f, 12f)
        lineTo(10f, 12f)
        lineTo(11.25f, 9.5f)
        lineTo(13f, 14.5f)
        lineTo(14.25f, 12f)
        lineTo(15.5f, 12f)

        badge()
    }
}.build()

private fun PathBuilder.statusRing() {
    moveTo(22f, 17f)
    curveTo(22f, 19.21f, 20.21f, 21f, 18f, 21f)
    curveTo(15.79f, 21f, 14f, 19.21f, 14f, 17f)
    curveTo(14f, 14.79f, 15.79f, 13f, 18f, 13f)
    curveTo(20.21f, 13f, 22f, 14.79f, 22f, 17f)
    close()
}
