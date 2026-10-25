package io.sellmair.pacemaker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.pacemaker.BluetoothState
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.HeartRateSensorState
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.UserColors
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.UtteranceState
import io.sellmair.pacemaker.WatchCompanionCandidate
import io.sellmair.pacemaker.WatchCompanionDeviceState
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.mainPage.LivePageContent
import io.sellmair.pacemaker.ui.mainPage.LiveScreenContentState
import io.sellmair.pacemaker.ui.settingsPage.DevicesPageContent
import io.sellmair.pacemaker.ui.settingsPage.DeviceSensorContentState
import io.sellmair.pacemaker.ui.settingsPage.DevicesScreenContentState
import io.sellmair.pacemaker.ui.settingsPage.ProfilePageContent
import io.sellmair.pacemaker.ui.settingsPage.ProfileScreenContentState

/** A deterministic, backend-free entry point for the iOS native UI test launch path. */
object IosPacemakerUiTestFixture {
    fun create(scenario: String) = ComposeUIViewController {
        val now = 1_735_689_600_000L
        val user = remember { User(UserId(42L), "Alex Rivera") }
        val mila = remember { User(UserId(43L), "Mila") }
        val alex = remember { User(UserId(44L), "Alex") }

        val initialRate = when (scenario) {
            "live-equal" -> HeartRate(150)
            "live-above" -> HeartRate(151)
            "live-missing" -> null
            else -> HeartRate(142)
        }
        val initialMeasuredAt = when (scenario) {
            "live-missing" -> null
            "live-future" -> now + 1L
            "live-expired" -> now - 60_001L
            "live-gap-boundary" -> now - 60_000L
            else -> now - 25_000L
        }
        val initialLimit = when (scenario) {
            "live-equal", "live-above" -> HeartRate(150)
            "profile-fractional" -> HeartRate(150.5f)
            "profile-min" -> HeartRate(40)
            "profile-max" -> HeartRate(200)
            else -> HeartRate(150)
        }

        var me by remember(scenario) {
            mutableStateOf(
                MeState(
                    me = user,
                    heartRate = initialRate,
                    heartRateLimit = initialLimit,
                    measuredAtEpochMillis = initialMeasuredAt
                )
            )
        }
        var displayNameDraft by remember(scenario) { mutableStateOf(me.me.name) }
        var hue by remember(scenario) {
            mutableStateOf(if (scenario == "profile-fractional") 145.7f else 118f)
        }
        var utteranceState by remember(scenario) { mutableStateOf(UtteranceState.All) }
        var selectedSource by remember(scenario) {
            mutableStateOf(
                when (scenario) {
                    "live-disconnected" -> HeartRateSource(
                        id = "fixture-sensor",
                        kind = HeartRateSource.Kind.BLE_SENSOR,
                        availability = HeartRateSource.Availability.DISCONNECTED,
                        connectionName = "Polar H10"
                    )
                    else -> null
                }
            )
        }

        val group = remember(me, scenario) {
            val self = UserState(
                user = user,
                isMe = true,
                heartRate = me.heartRate ?: HeartRate(142),
                heartRateLimit = me.heartRateLimit,
                color = UserColors.default(user.id),
                measuredAtEpochMillis = me.measuredAtEpochMillis ?: now
            )
            val peerGap = scenario == "peer-gap"
            GroupState(
                members = listOf(
                    self,
                    UserState(
                        user = mila,
                        isMe = false,
                        heartRate = HeartRate(136),
                        heartRateLimit = HeartRate(145),
                        color = UserColors.default(mila.id),
                        measuredAtEpochMillis = if (peerGap) now - 60_001L else now - 25_000L,
                        isConnectionLost = peerGap
                    ),
                    UserState(
                        user = alex,
                        isMe = false,
                        heartRate = HeartRate(148),
                        heartRateLimit = HeartRate(155),
                        color = UserColors.default(alex.id),
                        measuredAtEpochMillis = now - 20_000L
                    ),
                    // A duplicate self entry proves that the Live list omits the current user by ID.
                    self
                )
            )
        }

        val sensorId = remember { HeartRateSensorId("fixture-sensor") }
        val sensor = remember(user) {
            HeartRateSensorState(
                id = sensorId,
                name = "Polar H10",
                rssi = -48,
                heartRate = HeartRate(142),
                associatedUser = user,
                associatedHeartRateLimit = HeartRate(150)
            )
        }
        val sensorBluetooth = when (scenario) {
            "devices-permission" -> BluetoothState(
                isBluetoothEnabled = true,
                isBluetoothPermissionGranted = false
            )
            "devices-bluetooth-off" -> BluetoothState(
                isBluetoothEnabled = false,
                isBluetoothPermissionGranted = true
            )
            else -> BluetoothState(
                isBluetoothEnabled = true,
                isBluetoothPermissionGranted = true
            )
        }
        val watchAvailable = scenario != "devices-watch-unavailable"
        val watchCandidate = remember(watchAvailable) {
            WatchCompanionCandidate(
                id = "fixture-watch",
                name = "Apple Watch",
                paired = true,
                connected = watchAvailable
            )
        }
        val companion = remember(watchCandidate, watchAvailable) {
            WatchCompanionDeviceState(
                paired = true,
                connected = watchAvailable,
                name = "Apple Watch",
                candidates = listOf(watchCandidate),
                platformAvailable = watchAvailable
            )
        }
        val deviceState = DevicesScreenContentState(
            selectedSource = selectedSource,
            sensors = listOf(DeviceSensorContentState(sensor = sensor, connection = null)),
            companion = companion,
            bluetooth = sensorBluetooth
        )

        SharedRhythmTheme {
            PageRouter { page ->
                when (page) {
                    Page.Live -> LivePageContent(
                        state = LiveScreenContentState(
                            me = me,
                            group = group,
                            selectedSource = selectedSource,
                            utteranceState = utteranceState,
                            nowEpochMillis = now
                        ),
                        identityColor = Color(0xFFB6F34A),
                        onUtteranceStateChanged = { utteranceState = it }
                    )

                    Page.Devices -> DevicesPageContent(
                        state = deviceState,
                        onSourceSelected = { selectedSource = it },
                        onSensorConnect = {},
                        onSensorDisconnect = {}
                    )

                    Page.Profile -> ProfilePageContent(
                        state = ProfileScreenContentState(
                            me = me,
                            displayNameDraft = displayNameDraft,
                            hue = hue,
                            utteranceState = utteranceState
                        ),
                        onDisplayNameDraftChanged = { displayNameDraft = it },
                        onNameCommit = { me = me.copy(me = me.me.copy(name = it)) },
                        onHeartRateLimitChanged = { me = me.copy(heartRateLimit = it) },
                        onHueChanged = { hue = it },
                        onUtteranceStateChanged = { utteranceState = it }
                    )
                }
            }
        }
    }
}
