package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchHeartRateSensorAutoConnector(
    userService: UserService,
    heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launch {
    heartRateSensorBluetoothService.await().newSensorsNearby.collect { sensor ->
        /*
        A sensor that is already connected at startup:
        Maybe the sensor is already used for another running app. We therefore can safely assume
        that such sensor can be linked ot our account!
        */
        if (sensor.connectionState.value == BleConnectable.ConnectionState.Connected) {
            userService.linkSensor(userService.me(), sensor.deviceId.toHeartRateSensorId())
        }
        launchHeartRateSensorAutoConnector(userService, sensor)
    }
}

private fun CoroutineScope.launchHeartRateSensorAutoConnector(userService: UserService, sensor: HeartRateSensor): Job = launch {
    val meId = userService.me().id

    userService.findUserFlow(sensor.deviceId.toHeartRateSensorId()).collect { userOrNull ->
        if (userOrNull != null && (userOrNull.id == meId || userOrNull.isAdhoc)) {
            sensor.connectIfPossible(true)
        } else {
            sensor.connectIfPossible(false)
        }
    }
}