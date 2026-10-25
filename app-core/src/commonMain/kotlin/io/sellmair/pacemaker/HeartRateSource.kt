package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.evas.State
import io.sellmair.evas.Event
import kotlin.time.Duration.Companion.minutes

/** The currently selected input for this participant. Only one source is active at a time. */
data class HeartRateSource(
    val id: String,
    val kind: Kind,
    val availability: Availability,
    val heartRate: HeartRate? = null,
    /** Unix epoch milliseconds at which this value was measured by the source. */
    val measuredAtEpochMillis: Long? = null,
    val connectionName: String? = null,
    /** Platform-local identity of an explicitly selected companion device. */
    val companionDeviceId: String? = null
) {
    enum class Kind { BLE_SENSOR, COMPANION_WATCH, ONBOARD_WATCH }
    enum class Availability { AVAILABLE, CONNECTING, DISCONNECTED, PERMISSION_REQUIRED, UNSUPPORTED }

    /** Allow normal gaps between sensor updates before expiring the last measurement. */
    fun currentHeartRate(nowEpochMillis: Long): HeartRate? {
        val value = heartRate ?: return null
        val measuredAt = measuredAtEpochMillis ?: return null
        val age = nowEpochMillis - measuredAt
        return value.takeIf { age >= 0 && age <= MAX_AGE.inWholeMilliseconds && availability == Availability.AVAILABLE }
    }

    companion object {
        val MAX_AGE = 1.minutes
    }
}

/** Explicit input choice for this device. A disconnected selected watch does not silently fall back to a strap. */
data class SelectedHeartRateSourceState(val source: HeartRateSource?) : State {
    companion object Key : State.Key<SelectedHeartRateSourceState> {
        override val default = SelectedHeartRateSourceState(null)
    }
}

data class HeartRateSourceSelectionIntent(val source: HeartRateSource?) : Event

fun MeState.hasFreshHeartRate(nowEpochMillis: Long): Boolean {
    val measuredAt = measuredAtEpochMillis ?: return false
    val age = nowEpochMillis - measuredAt
    return heartRate != null && age in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds
}

fun MeState.heartRateForBroadcast(nowEpochMillis: Long): HeartRate? = heartRate.takeIf { hasFreshHeartRate(nowEpochMillis) }

fun acceptsHeartRateMeasurement(
    selectedSource: HeartRateSource?, ownUserId: String, measurementUserId: String, sensorId: String
): Boolean = measurementUserId != ownUserId || selectedSource == null || selectedSource.id == sensorId
