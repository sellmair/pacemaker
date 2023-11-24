@file:OptIn(ExperimentalAnimationApi::class, FlowPreview::class)

package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.AdhocUserIntent
import io.sellmair.pacemaker.HeartRateSensorConnectionIntent
import io.sellmair.pacemaker.HeartRateSensorConnectionState
import io.sellmair.pacemaker.HeartRateSensorState
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.*
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.HSLColor
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.ui.widget.*
import io.sellmair.pacemaker.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.pacemaker.ui.widget.GradientBackdrop
import io.sellmair.pacemaker.ui.widget.HeartRateScale
import io.sellmair.pacemaker.utils.emit
import kotlinx.coroutines.FlowPreview
import kotlin.math.roundToInt
import kotlin.reflect.KProperty


@Composable
internal fun HeartRateSensorCard(
    me: User,
    heartRateSensorState: HeartRateSensorState,
    heartRateSensorConnectionState: HeartRateSensorConnectionState,
    modifier: Modifier = Modifier,
) {
    HeartRateSensorCard(
        me = me,
        sensorName = heartRateSensorState.name,
        sensorId = heartRateSensorState.id,
        rssi = heartRateSensorState.rssi,
        heartRate = heartRateSensorState.heartRate,
        associatedUser = heartRateSensorState.associatedUser,
        associatedHeartRateLimit = heartRateSensorState.associatedHeartRateLimit,
        connectIfPossible = heartRateSensorConnectionState.connectIfPossible,
        connectionState = heartRateSensorConnectionState.connectionState,
        modifier = modifier,
        onConnectClicked = Launching { HeartRateSensorConnectionIntent.Connect(heartRateSensorState.id).emit() },
        onDisconnectClicked = Launching { HeartRateSensorConnectionIntent.Disconnect(heartRateSensorState.id).emit() }
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
    onConnectClicked: () -> Unit = {},
    onDisconnectClicked: () -> Unit = {}
) {

    var expanded: Boolean by remember { mutableStateOf(false) }
    var adhocUserViewVisible by mutableStateOf(associatedUser?.isAdhoc == true)

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = if (expanded) 6.dp else 3.dp
        ),
        onClick = { expanded = !expanded }
    ) {
        Box(modifier = Modifier.fillMaxWidth().defaultMinSize(50.dp)) {
            GradientBackdrop(
                modifier = Modifier.fillMaxWidth().matchParentSize()
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartRateCardTitle(
                        sensorName = sensorName,
                        sensorId = sensorId,
                        associatedUser = associatedUser,
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
                    enter = expandVertically(clip = false).plus(fadeIn()),
                    exit = shrinkVertically(clip = false).plus(fadeOut())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        /*
                        Connect/Disconnect as 'adhoc' user
                         */
                        AnimatedVisibility(visible = associatedUser == null || associatedUser.isAdhoc) {
                            IconButton(
                                modifier = Modifier.animateEnterExit(
                                    exit = slideOutHorizontally { -it } + fadeOut(),
                                    enter = slideInHorizontally { -it } + fadeIn()
                                ),
                                onClick = Launching {
                                    if (associatedUser?.isAdhoc == true) {
                                        AdhocUserIntent.DeleteAdhocUser(associatedUser).emit()
                                        adhocUserViewVisible = false
                                        onDisconnectClicked()
                                    } else {
                                        AdhocUserIntent.CreateAdhocUser(sensorId).emit()
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
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        /*
                        Connect/Disconnect to 'me' (aka my account)
                         */
                        AnimatedVisibility(
                            visible = associatedUser == null || associatedUser.id == me.id
                        ) {
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
        }

        AnimatedVisibility(
            visible = expanded && adhocUserViewVisible
        ) {
            val range = HeartRate(100)..HeartRate(200)

            HeartRateScale(
                range = range,
                horizontalCenterBias = .35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                ChangeableMemberHeartRateLimit(
                    color = associatedUser?.displayColor?.toColor() ?: Color.Gray,
                    heartRateLimit = HeartRate(130f),
                    range = range,
                    horizontalCenterBias = .35f,
                    side = ScaleSide.Right,
                    onLimitChanged = Launching { newHeartRateLimit ->
                        if (associatedUser != null) {
                            AdhocUserIntent.UpdateAdhocUserLimit(associatedUser, newHeartRateLimit).emit()
                        }
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
) {
    Column {
        AnimatedVisibility(
            visible = associatedUser != null,
            enter = expandVertically(clip = false) + fadeIn(),
            exit = shrinkVertically(clip = false) + fadeOut()
        ) {
            /* Remember last non null user */
            val userState = remember { mutableStateOf(associatedUser) }
            if (associatedUser != null) userState.value = associatedUser
            val user = userState.value ?: return@AnimatedVisibility

            var userName by remember { mutableStateOf(user.name) }

            val customTextSelectionColors = TextSelectionColors(
                handleColor = Color.White,
                backgroundColor = Color.White.copy(alpha = 0.4f)
            )

            val focusManager = LocalFocusManager.current

            CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                BasicTextField(
                    readOnly = !user.isAdhoc,
                    value = userName,
                    keyboardActions = KeyboardActions(onDone = {
                        this.defaultKeyboardAction(ImeAction.Done)
                        focusManager.clearFocus()
                    }),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    singleLine = true,
                    decorationBox = { it() },
                    onValueChange = Launching { newName ->
                        userName = newName
                        AdhocUserIntent.UpdateAdhocUser(user.copy(name = newName)).emit()
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
            containerColor = if (connectionState == Connected) color.copy(lightness = .6f).toColor()
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
