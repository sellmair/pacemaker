package io.sellmair.pacemaker.bluetooth

import io.sellmair.evas.Event
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
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