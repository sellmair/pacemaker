package io.sellmair.pacemaker.ui.widget

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.ui.icons.HeartRateSensorIcons
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.*

internal enum class HeartRateSensorIconState {
    Sensor, Searching, Connecting, Connected, Disconnecting, Disconnected
}

@Composable
internal fun HeartRateSensorIcon(
    state: HeartRateSensorIconState,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    decorative: Boolean = false
) {
    val image = when (state) {
        HeartRateSensorIconState.Sensor -> HeartRateSensorIcons.Sensor
        HeartRateSensorIconState.Searching -> HeartRateSensorIcons.Searching
        HeartRateSensorIconState.Connecting -> HeartRateSensorIcons.Connecting
        HeartRateSensorIconState.Connected -> HeartRateSensorIcons.Connected
        HeartRateSensorIconState.Disconnecting -> HeartRateSensorIcons.Disconnecting
        HeartRateSensorIconState.Disconnected -> HeartRateSensorIcons.Disconnected
    }
    val description = when (state) {
        HeartRateSensorIconState.Sensor -> Res.string.heart_rate_sensor
        HeartRateSensorIconState.Searching -> Res.string.searching_for_hr_sensors
        HeartRateSensorIconState.Connecting -> Res.string.heart_rate_sensor_connecting
        HeartRateSensorIconState.Connected -> Res.string.heart_rate_sensor_connected
        HeartRateSensorIconState.Disconnecting -> Res.string.heart_rate_sensor_disconnecting
        HeartRateSensorIconState.Disconnected -> Res.string.heart_rate_sensor_not_connected
    }
    Icon(image, if (decorative) null else stringResource(description), modifier, tint)
}
