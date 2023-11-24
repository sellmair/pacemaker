package io.sellmair.pacemaker

import androidx.compose.runtime.Immutable
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connected
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Disconnected
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.utils.Event
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.collectEventsAsync
import io.sellmair.pacemaker.utils.launchStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Immutable
data class HeartRateSensorConnectionState(
    val connectIfPossible: Boolean,
    val connectionState: BleConnectable.ConnectionState?
) : State {
    data class Key(val sensorId: HeartRateSensorId) : State.Key<HeartRateSensorConnectionState?> {
        override val default: HeartRateSensorConnectionState? = null
    }
}

sealed class HeartRateSensorConnectionIntent : Event {
    abstract val id: HeartRateSensorId

    data class Connect(override val id: HeartRateSensorId) : HeartRateSensorConnectionIntent()
    data class Disconnect(override val id: HeartRateSensorId) : HeartRateSensorConnectionIntent()
}

internal fun CoroutineScope.launchHeartRateSensorConnectionStateActor(
    userService: UserService,
    bluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launchStateProducer(Dispatchers.Main.immediate, keepActive = 1.minutes) { key: HeartRateSensorConnectionState.Key ->
    bluetoothService.await().withHeartRateSensor(key.sensorId) { sensor ->
        if (sensor == null) return@withHeartRateSensor
        launchHeartRateSensorConnectionIntentActor(userService, sensor)
        emitAll(createHeartRateSensorConnectionStateFlow(sensor))
    }
}

private fun CoroutineScope.launchHeartRateSensorConnectionIntentActor(userService: UserService, sensor: HeartRateSensor) =
    collectEventsAsync<HeartRateSensorConnectionIntent> { intent ->
        if (intent.id != sensor.deviceId.toHeartRateSensorId()) return@collectEventsAsync

        when (intent) {
            is HeartRateSensorConnectionIntent.Connect -> {
                if (userService.findUser(sensor.deviceId.toHeartRateSensorId()) == null) {
                    userService.linkSensor(userService.me(), sensor.deviceId.toHeartRateSensorId())
                }
            }

            is HeartRateSensorConnectionIntent.Disconnect -> {
                if (userService.findUser(sensor.deviceId.toHeartRateSensorId()) == userService.me()) {
                    userService.unlinkSensor(sensor.deviceId.toHeartRateSensorId())
                }
            }
        }
    }

@OptIn(FlowPreview::class)
private fun CoroutineScope.createHeartRateSensorConnectionStateFlow(sensor: HeartRateSensor): Flow<HeartRateSensorConnectionState> {
    val connectIfPossibleFlow = sensor.connectIfPossible
    val connectionStateFlow = MutableStateFlow<BleConnectable.ConnectionState?>(sensor.connectionState.value)
    val debouncedConnectionStateFlow = connectionStateFlow
        .debounce { if (it in setOf(Disconnected, Connected)) 500.milliseconds else 0.seconds }

    launch {
        sensor.connectionState.collect { state -> connectionStateFlow.value = state }
    }

    /* State prediction for disconnecting */
    collectEventsAsync<HeartRateSensorConnectionIntent.Disconnect> { intent ->
        if (intent.id != sensor.deviceId.toHeartRateSensorId()) return@collectEventsAsync
        if (connectionStateFlow.value != Disconnected) {
            connectionStateFlow.value = null
        }
    }

    /* State prediction for connecting */
    collectEventsAsync<HeartRateSensorConnectionIntent.Connect> { intent ->
        if (intent.id != sensor.deviceId.toHeartRateSensorId()) return@collectEventsAsync
        if (connectionStateFlow.value == Disconnected) {
            connectionStateFlow.value = BleConnectable.ConnectionState.Connecting
        }
    }


    return combine(connectIfPossibleFlow, debouncedConnectionStateFlow) { connectIfPossible, connectionState ->
        HeartRateSensorConnectionState(connectIfPossible, connectionState)
    }.distinctUntilChanged()
}