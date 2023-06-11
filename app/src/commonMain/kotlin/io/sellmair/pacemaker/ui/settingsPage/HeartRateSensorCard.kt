@file:OptIn(ExperimentalAnimationApi::class, FlowPreview::class)

package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent.CreateAdhocUser
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent.DeleteAdhocUser
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent.UpdateAdhocUser
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent.UpdateAdhocUserLimit
import io.sellmair.pacemaker.HeartRateSensorViewModel
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connected
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connecting
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Disconnected
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.HSLColor
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.pacemaker.ui.widget.HeartRateScale
import io.sellmair.pacemaker.ui.widget.ScaleSide
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Composable
internal fun HeartRateSensorCard(
    me: User,
    viewModel: HeartRateSensorViewModel,
    modifier: Modifier = Modifier,
    onEvent: (ApplicationIntent.SettingsPageIntent) -> Unit = {}
) {
    HeartRateSensorCard(
        me = me,
        sensorName = viewModel.name,
        sensorId = viewModel.id,
        rssi = viewModel.rssi.collectAsState().value,
        heartRate = viewModel.heartRate.collectAsState().value,
        associatedUser = viewModel.associatedUser.collectAsState().value,
        associatedHeartRateLimit = viewModel.associatedHeartRateLimit.collectAsState().value,
        connectIfPossible = viewModel.connection.connectIfPossible.collectAsState().value,
        connectionState = viewModel.connection.connectionState
            .debounce { if (it in setOf(Disconnected, Connected)) 500.milliseconds else 0.seconds }
            .collectAsState(Disconnected)
            .value,
        modifier = modifier,
        onEvent = onEvent,
        onConnectClicked = { viewModel.connection.onConnectClicked() },
        onDisconnectClicked = { viewModel.connection.onDisconnectClicked() }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeartRateSensorCard(
    me: User,
    sensorName: String?,
    sensorId: HeartRateSensorId,
    rssi: Int?,
    heartRate: HeartRate?,
    associatedUser: User?,
    associatedHeartRateLimit: HeartRate?,
    connectIfPossible: Boolean,
    connectionState: ConnectionState?,
    modifier: Modifier = Modifier,
    onEvent: (ApplicationIntent.SettingsPageIntent) -> Unit = {},
    onConnectClicked: () -> Unit = {},
    onDisconnectClicked: () -> Unit = {}
) {

    var expanded: Boolean by remember { mutableStateOf(false) }
    var adhocUserViewVisible by mutableStateOf(associatedUser?.isAdhoc == true)

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = { expanded = !expanded }
    ) {
        Box(modifier = Modifier.fillMaxWidth().defaultMinSize(50.dp)) {
            MeshBackdrop(
                modifier = Modifier.fillMaxWidth().matchParentSize()
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartRateCardTitle(
                        sensorName = sensorName,
                        sensorId = sensorId,
                        associatedUser = associatedUser,
                        onEvent = onEvent
                    )

                    Spacer(Modifier.weight(1f))

                    Box {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !connectIfPossible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                Icons.Outlined.MonitorHeart,
                                tint = Color.White,
                                contentDescription = "Heart Rate Sensor",
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = connectIfPossible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                Icons.Filled.MonitorHeart,
                                tint = Color.White,
                                contentDescription = "Heart Rate Sensor",
                            )
                        }
                    }

                }

                Spacer(Modifier.height(8.dp))

                SensorLiveInformation(
                    visible = heartRate != null && connectionState == Connected,
                    icon = Icons.Outlined.FavoriteBorder,
                    text = "${heartRate?.value?.roundToInt() ?: ""}"
                )

                SensorLiveInformation(
                    visible = rssi != null,
                    icon = Icons.Outlined.CellTower,
                    text = "$rssi db"
                )

                SensorLiveInformation(
                    visible = associatedHeartRateLimit != null,
                    icon = Icons.Outlined.Warning,
                    text = "${associatedHeartRateLimit?.value?.roundToInt() ?: ""}"
                )

                Spacer(Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(clip = false),
                    exit = shrinkVertically(clip = false)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        IconButton(
                            modifier = Modifier.animateEnterExit(
                                exit = slideOutHorizontally { -it } + fadeOut(),
                                enter = slideInHorizontally { -it } + fadeIn()
                            ),
                            onClick = {
                                if (associatedUser?.isAdhoc == true) {
                                    onEvent(DeleteAdhocUser(associatedUser))
                                    adhocUserViewVisible = false
                                    onDisconnectClicked()
                                } else {
                                    onEvent(CreateAdhocUser(sensorId))
                                    adhocUserViewVisible = true
                                }
                            },
                        ) {
                            Icon(
                                if (associatedUser?.isAdhoc == true) Icons.Outlined.PersonRemove
                                else Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = me.displayColorLight.copy(lightness = .95f).toColor()
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        ConnectDisconnectButton(
                            modifier = Modifier.animateEnterExit(
                                exit = slideOutHorizontally { it } + fadeOut(),
                                enter = slideInHorizontally { it } + fadeIn()
                            ),
                            color = me.displayColor,
                            connectionState = connectionState,
                            onConnectClicked = onConnectClicked,
                            onDisconnectClicked = onDisconnectClicked
                        )
                    }
                }

            }
        }

        AnimatedVisibility(
            visible = expanded && adhocUserViewVisible
        ) {
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
                    color = associatedUser?.displayColor?.toColor() ?: Color.Gray,
                    heartRateLimit = HeartRate(130f),
                    range = range,
                    horizontalCenterBias = .35f,
                    side = ScaleSide.Right,
                    onLimitChanged = { newHeartRateLimit ->
                        if (associatedUser != null)
                            onEvent(UpdateAdhocUserLimit(associatedUser, newHeartRateLimit))
                    }
                )
            }
        }

    }
}

@Composable
internal fun SensorLiveInformation(
    visible: Boolean,
    icon: ImageVector,
    text: String
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(clip = false, expandFrom = Alignment.CenterVertically) + fadeIn(),
        exit = shrinkVertically(clip = false, shrinkTowards = Alignment.CenterVertically) + fadeOut()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
internal fun HeartRateCardTitle(
    sensorName: String?,
    sensorId: HeartRateSensorId,
    associatedUser: User?,
    onEvent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    Column {
        AnimatedVisibility(
            visible = associatedUser != null,
            enter = expandVertically(clip = false) + fadeIn(),
            exit = shrinkVertically(clip = false) + fadeOut()
        ) {
            /* Remember last non null user */
            val userState = remember { mutableStateOf(associatedUser) }
            if(associatedUser != null) userState.value = associatedUser
            val user = userState.value?: return@AnimatedVisibility

            var userName by remember { mutableStateOf(user.name) }

            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color.White,
                backgroundColor = Color.White.copy(alpha = 0.4f)
            )

            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                BasicTextField(
                    readOnly = !user.isAdhoc,
                    value = user.name,
                    cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    singleLine = true,
                    decorationBox = { it() },
                    onValueChange = { newName ->
                        userName = newName
                        onEvent(UpdateAdhocUser(user.copy(name = newName)))
                    }
                )
            }
        }

        val labelTextSize by animateFloatAsState(if (associatedUser != null) 12f else 18f)

        Text(
            text = sensorName ?: sensorId.value,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = labelTextSize.sp
        )
    }
}

@Composable
internal fun ConnectDisconnectButton(
    color: HSLColor,
    connectionState: ConnectionState?,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        modifier = modifier,
        enabled = connectionState != null && connectionState != Connecting,
        onClick = onClick@{
            when (connectionState) {
                null -> Unit
                Disconnected -> onConnectClicked()
                Connecting -> Unit
                Connected -> onDisconnectClicked()
            }
        },
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (connectionState == Connected) Color.Gray
            else color.copy(lightness = .4f).toColor(),
        ),
    ) {
        val lightColor = color.copy(lightness = .95f)

        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                visible = connectionState == null || connectionState == Disconnected,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Text("Connect", color = lightColor.toColor())
            }

            AnimatedVisibility(
                visible = connectionState == Connected,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Text("Disconnect", color = lightColor.copy(saturation = 0.1f).toColor())
            }

            AnimatedVisibility(
                visible = connectionState == Connecting,
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                CircularProgressIndicator(
                    color = lightColor.toColor(),
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
