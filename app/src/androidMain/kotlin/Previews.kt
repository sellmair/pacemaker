import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.settingsPage.HeartRateSensorCard

@Preview
@Composable
fun HeartRateSensorPreview() {
    HeartRateSensorCard(
        me = User(
            isMe = true,
            id = UserId(0),
            name = "Sebastian Sellmair",
            isAdhoc = false,
        ),
        sensorName = "Polar H10 8B18CA22",
        sensorId = HeartRateSensorId("this is a sensor id"),
        rssi = 80,
        heartRate = HeartRate(64f),
        associatedUser = null,
        associatedHeartRateLimit = null,
        connectIfPossible = false,
        connectionState = BleConnectable.ConnectionState.Disconnected
    )
}