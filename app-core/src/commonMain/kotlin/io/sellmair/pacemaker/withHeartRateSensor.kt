package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateSensorId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

internal suspend fun HeartRateSensorBluetoothService.withHeartRateSensor(
    id: HeartRateSensorId, block: suspend (sensor: HeartRateSensor?) -> Unit
) {
    allSensorsNearby
        .map { sensors -> sensors.find { it.deviceId.toHeartRateSensorId() == id } }
        .distinctUntilChangedBy { sensor -> sensor?.deviceId }
        .collectLatest { sensorOrNull -> block(sensorOrNull) }
}
