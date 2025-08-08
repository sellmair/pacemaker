package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import kotlin.time.Instant

data class HeartRateSensorMeasurement(
    val heartRate: HeartRate,
    val sensorInfo: HeartRateSensorInfo,
    val receivedTime: Instant
)
