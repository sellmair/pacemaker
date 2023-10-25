package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.utils.Event
import kotlinx.datetime.Instant

data class HeartRateMeasurementEvent(
    val heartRate: HeartRate,
    val sensorId: HeartRateSensorId,
    val time: Instant
): Event

fun HeartRateSensorMeasurement.toEvent() : HeartRateMeasurementEvent {
    return HeartRateMeasurementEvent(
        heartRate = heartRate,
        sensorId = sensorInfo.id,
        time = receivedTime
    )
}