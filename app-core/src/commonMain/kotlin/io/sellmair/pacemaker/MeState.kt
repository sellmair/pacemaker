package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.emit
import io.sellmair.pacemaker.utils.events
import io.sellmair.pacemaker.utils.launchStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

data class MeState(
    val me: User,
    val heartRate: HeartRate?,
    val heartRateLimit: HeartRate
) : State {
    companion object Key : State.Key<MeState?> {
        override val default: MeState? = null
    }
}

internal fun CoroutineScope.launchMeStateActor(userService: UserService) {

    class Values(
        val me: User? = null,
        val heartRate: HeartRate? = null,
        val heartRateLimit: HeartRate? = null
    )

    val valuesChannel = Channel<Values>()

    /* Collect measruements */
    launch {
        events<HeartRateMeasurementEvent> { measurement ->
            val user = userService.findUser(measurement.sensorId)
            val me = userService.me()
            if (user?.id == me.id) {
                valuesChannel.send(Values(me = me, heartRate = measurement.heartRate))
            }
        }
    }

    /* Collect changes to HR limit */
    launch {
        userService.findHeartRateLimitFlow(userService.me()).collect { heartRateLimit ->
            valuesChannel.send(Values(heartRateLimit = heartRateLimit))
        }
    }

    /* Collect changes in user service (maybe 'me' was updated?) */
    launch {
        userService.onChange.conflate().collect {
            valuesChannel.send(Values(me = userService.me()))
        }
    }


    /* Update MeState */
    launchStateProducer(MeState) {
        var values = Values(me = userService.me())

        valuesChannel.consumeEach { update ->
            values = Values(
                me = update.me ?: values.me,
                heartRate = update.heartRate ?: values.heartRate,
                heartRateLimit = update.heartRateLimit ?: values.heartRateLimit
            )

            MeState(
                me = values.me ?: return@consumeEach,
                heartRate = values.heartRate,
                heartRateLimit = values.heartRateLimit ?: return@consumeEach,
            ).emit()
        }
    }
}

