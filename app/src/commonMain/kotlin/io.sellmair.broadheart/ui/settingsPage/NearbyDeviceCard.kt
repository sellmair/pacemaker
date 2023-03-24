package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.sellmair.broadheart.ApplicationIntent.SettingsPageIntent
import io.sellmair.broadheart.HeartRateSensorViewModel
import io.sellmair.broadheart.NearbyDeviceViewModel
import io.sellmair.broadheart.bluetooth.BlePeripheral
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.nameAbbreviation
import io.sellmair.broadheart.ui.displayColor
import io.sellmair.broadheart.ui.displayColorLight
import io.sellmair.broadheart.ui.toColor
import io.sellmair.broadheart.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.broadheart.ui.widget.HeartRateScale
import io.sellmair.broadheart.ui.widget.ScaleSide
import io.sellmair.broadheart.ui.widget.UserHead

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NearbyDeviceCard(
    me: User,
    device: NearbyDeviceViewModel,
    onEvent: (SettingsPageIntent) -> Unit = {}
) {
    val user = device.associatedUser.collectAsState().value
    val currentHeartRate = device.heartRate.collectAsState().value
    val heartRateLimit = device.associatedHeartRateLimit.collectAsState().value
    val sensorId = device.id
    val rssi = device.rssi.collectAsState().value

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
                        tint = device.displayColorLight.toColor(),
                        modifier = Modifier
                            .size(12.dp)
                    )
                    Text(currentHeartRate.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (heartRateLimit != null) {
                    Icon(
                        Icons.Default.Warning, "HR Limit",
                        tint = device.displayColorLight.toColor(),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(heartRateLimit.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(device.displayColor.toColor())
                )
                Spacer(modifier = Modifier.width(8.dp))


                Icon(
                    Icons.Outlined.Sensors, "HR Limit",
                    tint = device.displayColorLight.toColor(),
                    modifier = Modifier.size(12.dp)
                )
                Text(sensorId.value)
                Spacer(modifier = Modifier.width(8.dp))


                if (rssi != null) {
                    Icon(
                        Icons.Outlined.CellTower, "Signal Strength",
                        tint = device.displayColorLight.toColor(),
                        modifier = Modifier.size(12.dp)
                    )
                    Text("${rssi.value} db")
                }
            }

            if (contextMenuOpen) {
                Spacer(modifier = Modifier.height(24.dp))

                if (device is HeartRateSensorViewModel) {
                    val deviceState = device.state.collectAsState().value

                    if (deviceState == BlePeripheral.State.Connectable) {
                        ElevatedButton(
                            onClick = { device.tryConnect() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = me.displayColor.copy(lightness = .97f).toColor()
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text("Connect to Sensor")
                        }
                    }

                    if (deviceState == BlePeripheral.State.Connected) {
                        ElevatedButton(
                            onClick = { device.tryDisconnect() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = me.displayColor.copy(lightness = .97f).toColor()
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text("Disconnect from Sensor")
                        }
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