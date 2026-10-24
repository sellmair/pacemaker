package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.evas.compose.composeValue
import io.sellmair.pacemaker.HeartRateSensorConnectionState
import io.sellmair.pacemaker.HeartRateSensorState
import io.sellmair.pacemaker.HeartRateSensorsState
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.HeartRateSourceSelectionIntent
import io.sellmair.pacemaker.WATCH_COMPANION_SOURCE_ID
import io.sellmair.pacemaker.WatchCompanionDeviceState
import io.sellmair.pacemaker.ui.icons.WatchIcons
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.MeColor
import io.sellmair.pacemaker.ui.MeColorLight
import io.sellmair.pacemaker.ui.widget.HeartRateSensorIcon
import io.sellmair.pacemaker.ui.widget.HeartRateSensorIconState
import io.sellmair.evas.emit
import io.sellmair.pacemaker.ui.widget.Launching
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.nearby_heart_rate_sensors
import pacemaker.app.generated.resources.please_stand_by
import pacemaker.app.generated.resources.searching_for_hr_sensors
import pacemaker.app.generated.resources.use_watch_source
import pacemaker.app.generated.resources.use_sensor_source
import pacemaker.app.generated.resources.use_automatic_source
import pacemaker.app.generated.resources.selected_heart_rate_source
import pacemaker.app.generated.resources.watch_connected
import pacemaker.app.generated.resources.watch_paired_offline
import pacemaker.app.generated.resources.watch_not_paired
import pacemaker.app.generated.resources.watch_connect
import pacemaker.app.generated.resources.watch_disconnect
import pacemaker.app.generated.resources.watch_transport_unavailable

@Composable
internal fun SettingsPageContent(
    me: User,
    heartRateSensors: List<HeartRateSensorsState.HeartRateSensorInfo>,
    selectedSource: HeartRateSource?,
    companionState: WatchCompanionDeviceState
) {
    Column(Modifier.fillMaxSize()) {
        SettingsPageHeader(me = me)

        Spacer(Modifier.height(24.dp))

        Box {
            SettingsPageDevicesList(
                me = me,
                heartRateSensors = heartRateSensors,
                selectedSource = selectedSource,
                companionState = companionState
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsPageDevicesList(
    me: User,
    heartRateSensors: List<HeartRateSensorsState.HeartRateSensorInfo>,
    selectedSource: HeartRateSource?,
    companionState: WatchCompanionDeviceState
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartRateSensorIcon(
                state = HeartRateSensorIconState.Sensor,
                tint = MeColor(),
                modifier = Modifier.size(24.dp),
                decorative = true
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.nearby_heart_rate_sensors),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))


        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "watch companion") {
                val watchSelected = selectedSource?.kind == HeartRateSource.Kind.COMPANION_WATCH
                if (companionState.candidates.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(imageVector = WatchIcons.Watch, contentDescription = null,
                                modifier = Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MeColor()))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(companionState.name, fontWeight = FontWeight.Bold)
                                Text(
                                    if (companionState.platformAvailable) stringResource(Res.string.watch_not_paired)
                                    else stringResource(Res.string.watch_transport_unavailable),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    companionState.candidates.forEach { candidate ->
                        val candidateSelected = watchSelected && selectedSource?.companionDeviceId == candidate.id
                        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(imageVector = WatchIcons.Watch, contentDescription = null,
                                    modifier = Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MeColor()))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(candidate.name, fontWeight = FontWeight.Bold)
                                    Text(stringResource(if (candidate.connected) Res.string.watch_connected else Res.string.watch_paired_offline, candidate.name), fontSize = 12.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = Launching {
                                    if (candidateSelected) HeartRateSourceSelectionIntent(null).emit()
                                    else HeartRateSourceSelectionIntent(HeartRateSource(
                                        WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
                                        HeartRateSource.Availability.CONNECTING, connectionName = candidate.name,
                                        companionDeviceId = candidate.id
                                    )).emit()
                                }) {
                                    Text(stringResource(if (candidateSelected) Res.string.watch_disconnect else Res.string.watch_connect))
                                }
                            }
                        }
                    }
                }
            }

            if (heartRateSensors.isEmpty()) {
                item(key = "empty placeholder") {
                    Spacer(Modifier.height(48.dp))
                    Column(
                        Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(24.dp))

                            HeartRateSensorIcon(
                                state = HeartRateSensorIconState.Searching,
                                tint = MeColorLight(),
                                modifier = Modifier.size(64.dp),
                                decorative = true
                            )
                            Spacer(Modifier.height(16.dp))

                            Text(
                                stringResource(Res.string.searching_for_hr_sensors),
                                fontSize = 12.sp
                            )

                            Text(
                                stringResource(Res.string.please_stand_by),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Light
                            )

                            Spacer(Modifier.height(24.dp))

                        }
                    }
                }
            }

            items(heartRateSensors, key = { it.id.value }) { sensor ->
                val sensorState = HeartRateSensorState.Key(sensor).composeValue()

                val sensorConnectionState = HeartRateSensorConnectionState.Key(sensor.id).composeValue()
                sensorConnectionState ?: return@items

                Box(
                    modifier = Modifier.padding(24.dp).animateItem()
                ) {
                    Column {
                        Button(onClick = Launching {
                            HeartRateSourceSelectionIntent(HeartRateSource(
                                sensor.id.value, HeartRateSource.Kind.BLE_SENSOR,
                                HeartRateSource.Availability.AVAILABLE, connectionName = sensor.name
                            )).emit()
                        }) { Text(stringResource(Res.string.use_sensor_source)) }
                        HeartRateSensorCard(me = me, sensorState, sensorConnectionState)
                    }
                }
            }

            item(key = "Progress") {
                Spacer(Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = MeColor(),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 1.dp
                )

                Spacer(Modifier.height(128.dp))
            }
        }
    }
}
