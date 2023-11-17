package io.sellmair.pacemaker

import androidx.compose.runtime.Immutable
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.launchStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

@Immutable
data class HeartRateSensorsState(val nearbySensors: List<HeartRateSensorInfo>) : State {
    @Immutable
    data class HeartRateSensorInfo(
        val id: HeartRateSensorId,
        val name: String?
    )

    companion object Key : State.Key<HeartRateSensorsState> {
        override val default: HeartRateSensorsState
            get() = HeartRateSensorsState(emptyList())
    }
}

internal fun CoroutineScope.launchHeartRateSensorsStateActor(
    bluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launchStateProducer(HeartRateSensorsState) {
    emitAll(bluetoothService.await()
        .allSensorsNearby
        .map { sensors ->
            val nearbySensors = sensors
                .sortedBy { it.deviceId.value }
                .map { sensor ->
                    HeartRateSensorsState.HeartRateSensorInfo(
                        id = sensor.deviceId.toHeartRateSensorId(),
                        name = sensor.deviceName
                    )
                }
            HeartRateSensorsState(nearbySensors)
        })
}