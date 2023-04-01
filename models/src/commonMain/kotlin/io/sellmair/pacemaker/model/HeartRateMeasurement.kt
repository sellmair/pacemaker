package io.sellmair.pacemaker.model

import kotlinx.serialization.Serializable
import kotlin.time.TimeMark

@Serializable
data class HeartRateMeasurement(
    val heartRate: HeartRate,
    val sensorInfo: HeartRateSensorInfo,
    val receivedTime: TimeMark
)