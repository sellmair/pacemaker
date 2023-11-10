package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
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
    heartRateSensorBluetoothService.await().newSensorsNearby.collect { sensor ->
        /*
        A sensor that is already connected at startup:
        Maybe the sensor is already used for another running app. We therefore can safely assume
        that such sensor can be linked ot our account!
         */
        if (sensor.connectionState.value == BleConnectable.ConnectionState.Connected) {
            userService.linkSensor(userService.me(), sensor.deviceId.toHeartRateSensorId())
        }

        val user = userService.findUser(sensor.deviceId.toHeartRateSensorId()) ?: return@collect
        if (user.id == meId || user.isAdhoc) {
            sensor.connectIfPossible(true)
        }
    }
}