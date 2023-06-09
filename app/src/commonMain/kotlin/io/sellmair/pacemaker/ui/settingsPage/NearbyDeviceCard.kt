package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent
import io.sellmair.pacemaker.HeartRateSensorViewModel
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.nameAbbreviation
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.pacemaker.ui.widget.HeartRateScale
import io.sellmair.pacemaker.ui.widget.ScaleSide
import io.sellmair.pacemaker.ui.widget.UserHead

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeartRateSensorCard(
    me: User,
    heartRateSensor: HeartRateSensorViewModel,
    onEvent: (SettingsPageIntent) -> Unit = {}
) {
    val user = heartRateSensor.associatedUser.collectAsState().value
    val currentHeartRate = heartRateSensor.heartRate.collectAsState().value
    val heartRateLimit = heartRateSensor.associatedHeartRateLimit.collectAsState().value
    val sensorId = heartRateSensor.id
    val sensorName = heartRateSensor.name
    val rssi = heartRateSensor.rssi.collectAsState().value

    var contextMenuOpen by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = { contextMenuOpen = !contextMenuOpen },
        colors = CardDefaults.cardColors(
            containerColor = user?.displayColor?.copy(lightness = .97f)?.toColor() ?: Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            user?.let { user ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserHead(
                        abbreviation = user.nameAbbreviation,
                        color = user.displayColor.toColor()
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(user.name, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(4.dp))
                if (currentHeartRate != null) {
                    Icon(
                        Icons.Default.Favorite, "HR",
                        tint = heartRateSensor.displayColorLight.toColor(),
                        modifier = Modifier
                            .size(12.dp)
                    )
                    Text(currentHeartRate.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (heartRateLimit != null) {
                    Icon(
                        Icons.Default.Warning, "HR Limit",
                        tint = heartRateSensor.displayColorLight.toColor(),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(heartRateLimit.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(heartRateSensor.displayColor.toColor())
                )
                Spacer(modifier = Modifier.width(8.dp))


                Icon(
                    Icons.Outlined.Sensors, "HR Limit",
                    tint = heartRateSensor.displayColorLight.toColor(),
                    modifier = Modifier.size(12.dp)
                )
                Text(sensorName ?: sensorId.value)
                Spacer(modifier = Modifier.width(8.dp))


                if (rssi != null) {
                    Icon(
                        Icons.Outlined.CellTower, "Signal Strength",
                        tint = heartRateSensor.displayColorLight.toColor(),
                        modifier = Modifier.size(12.dp)
                    )
                    Text("${rssi} db")
                }
            }

            if (contextMenuOpen) {
                Spacer(modifier = Modifier.height(24.dp))

                val deviceState = heartRateSensor.state.collectAsState().value

                if (deviceState != BleConnectable.ConnectionState.Connected && user != null) {
                    ElevatedButton(
                        enabled = deviceState == BleConnectable.ConnectionState.Disconnected,
                        onClick = { heartRateSensor.tryConnect() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = me.displayColor.copy(lightness = .97f).toColor()
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Connect to Sensor")
                    }
                }

                if (deviceState == BleConnectable.ConnectionState.Connected) {
                    ElevatedButton(
                        onClick = { heartRateSensor.tryDisconnect() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = me.displayColor.copy(lightness = .97f).toColor()
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Disconnect from Sensor")
                    }
                }

                /* Connect to my account button */
                if (user == null) {
                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageIntent.LinkSensor(me, sensorId))
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = me.displayColor.copy(lightness = .97f).toColor()
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Link to my account")
                    }

                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageIntent.CreateAdhocUser(sensorId))
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = me.displayColor.copy(lightness = .97f).toColor()
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Create adhoc user")
                    }
                }


                if (user?.isMe == true) {
                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageIntent.UnlinkSensor(sensorId))
                            heartRateSensor.tryDisconnect()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Disconnect from my account")
                    }
                }

                if (user?.isAdhoc == true && heartRateLimit != null) {
                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageIntent.DeleteAdhocUser(user))
                            onEvent(SettingsPageIntent.UnlinkSensor(sensorId))
                            heartRateSensor.tryDisconnect()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Delete adhoc user")
                    }

                    var adhocUserName by remember { mutableStateOf(user.name) }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = adhocUserName,
                        onValueChange = { newUserName ->
                            adhocUserName = newUserName
                            onEvent(SettingsPageIntent.UpdateAdhocUser(user.copy(name = newUserName)))
                        },
                        label = { Text("adhoc user name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val range = HeartRate(100)..HeartRate(170)
                    HeartRateScale(
                        range = range,
                        horizontalCenterBias = .35f,
                        modifier = Modifier
                            .padding(vertical = 48.dp)
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        ChangeableMemberHeartRateLimit(
                            user = user,
                            heartRateLimit = heartRateLimit,
                            range = range,
                            horizontalCenterBias = .35f,
                            side = ScaleSide.Right,
                            onLimitChanged = { newHeartRateLimit ->
                                onEvent(SettingsPageIntent.UpdateAdhocUserLimit(user, newHeartRateLimit))
                            }
                        )
                    }
                }
            }

        }
    }
}