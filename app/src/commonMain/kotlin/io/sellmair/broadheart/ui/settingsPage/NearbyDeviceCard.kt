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
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.displayColor
import io.sellmair.broadheart.displayColorLight
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.service.GroupMemberState
import io.sellmair.broadheart.ui.toColor
import io.sellmair.broadheart.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.broadheart.ui.widget.HeartRateScale
import io.sellmair.broadheart.ui.widget.ScaleSide
import io.sellmair.broadheart.ui.widget.UserHead

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDeviceCard(
    me: User,
    state: GroupMemberState,
    onEvent: (SettingsPageEvent) -> Unit = {}
) {
    var contextMenuOpen by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = { contextMenuOpen = !contextMenuOpen },
        colors = CardDefaults.cardColors(
            containerColor = state.user?.displayColor?.copy(lightness = .97f)?.toColor() ?: Color.White
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
            if (state.user != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserHead(state)
                    Spacer(Modifier.size(8.dp))
                    Text(state.user.name, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(4.dp))
                if (state.currentHeartRate != null) {
                    Icon(
                        Icons.Default.Favorite, "HR",
                        tint = state.displayColorLight.toColor(),
                        modifier = Modifier
                            .size(12.dp)
                    )
                    Text(state.currentHeartRate.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (state.upperHeartRateLimit != null) {
                    Icon(
                        Icons.Default.Warning, "HR Limit",
                        tint = state.displayColorLight.toColor(),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(state.upperHeartRateLimit.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(state.displayColor.toColor())
                )
                Spacer(modifier = Modifier.width(8.dp))


                if (state.sensorInfo != null) {
                    Icon(
                        Icons.Outlined.Sensors, "HR Limit",
                        tint = state.displayColorLight.toColor(),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(state.sensorInfo.id.value)
                    Spacer(modifier = Modifier.width(8.dp))


                    if (state.sensorInfo.rssi != null) {
                        Icon(
                            Icons.Outlined.CellTower, "Signal Strength",
                            tint = state.displayColorLight.toColor(),
                            modifier = Modifier.size(12.dp)
                        )
                        Text("${state.sensorInfo.rssi} db")
                    }
                }
            }

            if (contextMenuOpen) {
                Spacer(modifier = Modifier.height(24.dp))
                if (state.sensorInfo == null) return@Column

                /* Connect to my account button */
                if (state.user == null) {
                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageEvent.LinkSensor(me, state.sensorInfo.id))
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = me.displayColor.copy(lightness = .97f).toColor()
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Connect to my account")
                    }

                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageEvent.CreateAdhocUser(state.sensorInfo.id))
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


                if (state.user?.isMe == true) {
                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageEvent.UnlinkSensor(state.sensorInfo.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Disconnect from my account")
                    }
                }

                if (state.user?.isAdhoc == true) {
                    ElevatedButton(
                        onClick = {
                            onEvent(SettingsPageEvent.DeleteAdhocUser(state.user))
                            onEvent(SettingsPageEvent.UnlinkSensor(state.sensorInfo.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Delete adhoc user")
                    }

                    var adhocUserName by remember { mutableStateOf(state.user.name) }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = adhocUserName,
                        onValueChange = { newUserName ->
                            adhocUserName = newUserName
                            onEvent(SettingsPageEvent.UpdateAdhocUser(state.user.copy(name = newUserName)))
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
                            state = state,
                            range = range,
                            horizontalCenterBias = .35f,
                            side = ScaleSide.Right,
                            onLimitChanged = { newHeartRateLimit ->
                                onEvent(SettingsPageEvent.UpdateAdhocUserLimit(state.user, newHeartRateLimit))
                            }
                        )
                    }
                }
            }

        }
    }
}