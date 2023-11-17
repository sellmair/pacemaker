package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.launchStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

data class HeartRateSensorViewModelState(
    val sensors: List<HeartRateSensorViewModel>
) : State {
    companion object Key : State.Key<HeartRateSensorViewModelState> {
        override val default: HeartRateSensorViewModelState
            get() = HeartRateSensorViewModelState(emptyList())
    }
}

internal fun CoroutineScope.launchHeartRateSensorViewModelStateActor(
    userService: UserService,
    bluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launchStateProducer(HeartRateSensorViewModelState) {
    var viewModelCache: Map<BleDeviceId, HeartRateSensorViewModel> = hashMapOf()
    
    bluetoothService.await().allSensorsNearby.collect { sensors ->
        viewModelCache = sensors.associate { sensor ->
            sensor.deviceId to viewModelCache.getOrElse(sensor.deviceId) {
                HeartRateSensorViewModelImpl(this@launchHeartRateSensorViewModelStateActor, sensor, userService)
            }
        }

        emit(HeartRateSensorViewModelState(
            viewModelCache.entries.sortedBy { (key, _) -> key.value }
                .map { (_, viewModel) -> viewModel }
        ))
    }
}
