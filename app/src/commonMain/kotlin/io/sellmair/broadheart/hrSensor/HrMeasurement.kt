package io.sellmair.broadheart.hrSensor

import kotlinx.serialization.Serializable
import kotlin.time.TimeMark

@Serializable
data class HrMeasurement(
    val heartRate: HeartRate,
    val sensorInfo: HrSensorInfo,
    val receivedTime: TimeMark
)