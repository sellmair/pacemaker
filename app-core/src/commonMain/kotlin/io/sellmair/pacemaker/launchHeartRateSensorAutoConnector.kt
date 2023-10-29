package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchHeartRateSensorAutoConnector(
    userService: UserService,
    heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launch {
    val meId = userService.me().id
    heartRateSensorBluetoothService.await().newSensorsNearby
        .collect { sensor ->
            val user = userService.findUser(sensor.deviceId.toHeartRateSensorId()) ?: return@collect
            if (user.id == meId || user.isAdhoc) {
                sensor.connectIfPossible(true)
            }
        }
}