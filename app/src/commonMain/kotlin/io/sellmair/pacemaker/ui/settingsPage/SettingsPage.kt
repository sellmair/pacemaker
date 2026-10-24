package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.composeState
import io.sellmair.evas.emit
import io.sellmair.evas.set
import io.sellmair.pacemaker.BluetoothState
import io.sellmair.pacemaker.HeartRateSensorConnectionIntent
import io.sellmair.pacemaker.HeartRateSensorConnectionState
import io.sellmair.pacemaker.HeartRateSensorState
import io.sellmair.pacemaker.HeartRateSensorsState
import io.sellmair.pacemaker.HeartRateSourceSelectionIntent
import io.sellmair.pacemaker.MeColorIntent
import io.sellmair.pacemaker.MeColorState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.SelectedHeartRateSourceState
import io.sellmair.pacemaker.UtteranceState
import io.sellmair.pacemaker.UpdateMeIntent
import io.sellmair.pacemaker.UserColors
import io.sellmair.pacemaker.WatchCompanionDeviceState
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.Page
import io.sellmair.pacemaker.ui.StartupScreen
import io.sellmair.pacemaker.ui.StartupStatus
import io.sellmair.pacemaker.ui.widget.Launching

@Composable
internal fun SettingsPage(page: Page) {
    val meState = MeState.composeValue()
    if (meState == null) {
        StartupScreen(StartupStatus.Connecting)
        return
    }

    when (page) {
        Page.Devices -> {
            val sensors = HeartRateSensorsState.composeValue().nearbySensors.map { info ->
                DeviceSensorContentState(
                    sensor = HeartRateSensorState.Key(info).composeValue(),
                    connection = HeartRateSensorConnectionState.Key(info.id).composeValue()
                )
            }
            DevicesPageContent(
                state = DevicesScreenContentState(
                    selectedSource = SelectedHeartRateSourceState.composeValue().source,
                    sensors = sensors,
                    companion = WatchCompanionDeviceState.composeValue(),
                    bluetooth = BluetoothState.composeValue()
                ),
                onSourceSelected = Launching { source -> HeartRateSourceSelectionIntent(source).emit() },
                onSensorConnect = Launching { id -> HeartRateSensorConnectionIntent.Connect(id).emit() },
                onSensorDisconnect = Launching { id -> HeartRateSensorConnectionIntent.Disconnect(id).emit() }
            )
        }

        Page.Profile -> {
            val me = meState.me
            var draft by rememberSaveable(me.id.value.toString()) { mutableStateOf(me.name) }
            val hue = MeColorState.composeValue()?.color?.hue ?: UserColors.defaultHue(me.id)
            val utteranceState by UtteranceState.composeState()
            ProfilePageContent(
                state = ProfileScreenContentState(
                    me = meState,
                    displayNameDraft = draft,
                    hue = hue,
                    utteranceState = utteranceState
                ),
                onDisplayNameDraftChanged = { draft = it },
                onNameCommit = Launching { name -> UpdateMeIntent.UpdateMe(me.copy(name = name)).emit() },
                onHeartRateLimitChanged = Launching { value -> UpdateMeIntent.UpdateHeartRateLimit(value).emit() },
                onHueChanged = Launching { value -> MeColorIntent.ChangeHue(value).emit() },
                onUtteranceStateChanged = Launching { value -> UtteranceState.set(value) }
            )
        }

        Page.Live -> Unit
    }
}
