package io.sellmair.pacemaker

import io.sellmair.evas.State
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.launchState
import io.sellmair.evas.flow
import io.sellmair.evas.value
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class MeState(
    val me: User,
    val heartRate: HeartRate?,
    val heartRateLimit: HeartRate,
    val measuredAtEpochMillis: Long? = null
) : State {
    companion object Key : State.Key<MeState?> {
        override val default: MeState? = null
    }
}

internal fun CoroutineScope.launchMeStateActor(userService: UserService, clock: Clock = Clock.System) {

    class Values(
        val me: User? = null,
        val measurement: HeartRateMeasurementEvent? = null,
        val measurementChanged: Boolean = false,
        val clearMeasurement: Boolean = false,
        val clearMeasurementAt: Instant? = null,
        val heartRateLimit: HeartRate? = null
    )

    val valuesChannel = Channel<Values>()
    var expiryJob: Job? = null

    collectEventsAsync<HeartRateMeasurementEvent> { measurement ->
        val now = clock.now()
        val age = now - measurement.time
        if (age < kotlin.time.Duration.ZERO || age > HeartRateSource.MAX_AGE) return@collectEventsAsync
        val user = userService.findUser(measurement.sensorId)
        val me = userService.me()
        if (user?.id == me.id) {
            val selectedSource = SelectedHeartRateSourceState.value()?.source
            if (!acceptsHeartRateMeasurement(selectedSource, me.id.value.toString(), user.id.value.toString(), measurement.sensorId.value)) return@collectEventsAsync
            valuesChannel.send(Values(me = me, measurement = measurement, measurementChanged = true))
            expiryJob?.cancel()
            expiryJob = launch {
                kotlinx.coroutines.delay(HeartRateSource.MAX_AGE - age)
                valuesChannel.send(Values(clearMeasurementAt = measurement.time))
            }
        }
    }

    launch {
        userService.findHeartRateLimitFlow(userService.me()).collect { heartRateLimit ->
            valuesChannel.send(Values(heartRateLimit = heartRateLimit))
        }
    }

    launch {
        userService.onSaveUser.conflate().collect {
            valuesChannel.send(Values(me = userService.me()))
        }
    }

    launch {
        SelectedHeartRateSourceState.flow().map { state ->
            state.source?.let { it.id to it.companionDeviceId }
        }.distinctUntilChanged().collect {
            valuesChannel.send(Values(clearMeasurement = true))
        }
    }

    launchState(MeState) {
        var me = userService.me()
        var heartRateLimit: HeartRate? = null
        var currentMeasurement: HeartRateMeasurementEvent? = null

        valuesChannel.consumeEach { update ->
            if (update.clearMeasurementAt != null && currentMeasurement?.time != update.clearMeasurementAt) return@consumeEach
            me = update.me ?: me
            heartRateLimit = update.heartRateLimit ?: heartRateLimit
            if (update.measurementChanged) currentMeasurement = update.measurement
            if (update.clearMeasurement) currentMeasurement = null

            MeState(
                me = me,
                heartRate = currentMeasurement?.takeIf {
                    clock.now() - it.time < HeartRateSource.MAX_AGE
                }?.heartRate,
                heartRateLimit = heartRateLimit ?: return@consumeEach,
                measuredAtEpochMillis = currentMeasurement?.time?.toEpochMilliseconds()
            ).emit()
        }
    }
}
