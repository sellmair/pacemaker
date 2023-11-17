@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker

import androidx.compose.runtime.Immutable
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.HeartRateSensorMeasurement
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.launchStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration.Companion.minutes

@Immutable
data class HeartRateSensorState(
    val id: HeartRateSensorId,
    val name: String?,
    val rssi: Int?,
    val heartRate: HeartRate?,
    val associatedUser: User?,
    val associatedHeartRateLimit: HeartRate?
) : State {
    data class Key(val sensorInfo: HeartRateSensorsState.HeartRateSensorInfo) : State.Key<HeartRateSensorState> {
        override val default: HeartRateSensorState
            get() = HeartRateSensorState(
                id = sensorInfo.id,
                name = sensorInfo.name,
                heartRate = null,
                associatedUser = null,
                rssi = null,
                associatedHeartRateLimit = null
            )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun CoroutineScope.launchHeartRateSensorStateActor(
    userService: UserService,
    bluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launchStateProducer(keepActive = 1.minutes) { key: HeartRateSensorState.Key ->
    bluetoothService.await().withHeartRateSensor(key.sensorInfo.id) { sensor ->
        if (sensor == null) return@withHeartRateSensor key.default.emit()

        val associatedUserFlow = userService.findUserFlow(sensor.deviceId.toHeartRateSensorId())

        val associatedHeartRateLimitFlow = associatedUserFlow.flatMapLatest { associatedUser ->
            if (associatedUser == null) flowOf<HeartRate?>(null)
            else userService.findHeartRateLimitFlow(associatedUser)
        }

        val heartRateFlow = sensor.heartRate
            .map<HeartRateSensorMeasurement, HeartRate?> { it.heartRate }
            .onStart { emit(null) }

        val heartRateSensorStateFlow = combine(
            heartRateFlow,
            sensor.rssi,
            associatedUserFlow,
            associatedHeartRateLimitFlow
        ) { heartRate, rssi, associatedUser, associatedHeartRateLimit ->
            HeartRateSensorState(
                id = key.sensorInfo.id,
                name = key.sensorInfo.name,
                rssi = rssi,
                heartRate = heartRate,
                associatedUser = associatedUser,
                associatedHeartRateLimit = associatedHeartRateLimit
            )
        }

        emitAll(heartRateSensorStateFlow)
    }
}

