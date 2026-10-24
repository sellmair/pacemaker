import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.settingsPage.HeartRateSensorCard
import io.sellmair.pacemaker.ui.widget.HeartRateSensorIcon
import io.sellmair.pacemaker.ui.widget.HeartRateSensorIconState

@Preview(showBackground = true)
@Composable
fun HeartRateSensorIconsPreview() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeartRateSensorIconState.entries.forEach { state ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeartRateSensorIcon(state, Modifier.size(24.dp), Color(0xFF6650A4))
                HeartRateSensorIcon(state, Modifier.size(64.dp), Color(0xFF6650A4))
                Text(state.name)
            }
        }
    }
}

@Preview
@Composable
fun HeartRateSensorPreview() {
    HeartRateSensorCard(
        me = User(
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
