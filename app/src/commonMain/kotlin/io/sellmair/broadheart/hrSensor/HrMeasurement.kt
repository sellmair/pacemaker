
package io.sellmair.broadheart.hrSensor

import kotlin.time.TimeMark

data class HrMeasurement(
    val heartRate: HeartRate,
    val sensorInfo: HrSensorInfo,
    val receivedTime: TimeMark
)