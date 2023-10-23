package io.sellmair.pacemaker.model

import kotlinx.datetime.Instant

data class HeartRateMeasurement(
    val heartRate: HeartRate,
    val sensorInfo: HeartRateSensorInfo,
    val receivedTime: Instant
)