package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.MeColor
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.nearby_heart_rate_sensors
import pacemaker.app.generated.resources.please_stand_by
import pacemaker.app.generated.resources.searching_for_hr_sensors

@Composable
internal fun SettingsPageContent(
    me: User,
    heartRateSensors: List<HeartRateSensorsState.HeartRateSensorInfo>
) {
    Column(Modifier.fillMaxSize()) {
        SettingsPageHeader(me = me)

        Spacer(Modifier.height(24.dp))

        Box {
            SettingsPageDevicesList(
                me = me,
                heartRateSensors = heartRateSensors,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsPageDevicesList(
    me: User,
    heartRateSensors: List<HeartRateSensorsState.HeartRateSensorInfo>,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(Res.string.nearby_heart_rate_sensors),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))


        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

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
                    modifier = Modifier.padding(24.dp).animateItemPlacement()
                ) {
                    HeartRateSensorCard(
                        me = me,
                        sensorState,
                        sensorConnectionState
                    )
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
