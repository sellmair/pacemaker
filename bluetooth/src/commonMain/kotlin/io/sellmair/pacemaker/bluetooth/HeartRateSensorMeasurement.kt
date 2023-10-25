package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import kotlinx.datetime.Instant

data class HeartRateSensorMeasurement(
    val heartRate: HeartRate,
    val sensorInfo: HeartRateSensorInfo,
    val receivedTime: Instant
)