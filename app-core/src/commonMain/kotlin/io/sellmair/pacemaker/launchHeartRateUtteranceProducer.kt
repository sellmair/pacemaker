package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.utils.Event
import io.sellmair.pacemaker.utils.emit
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


sealed class HeartRateUtteranceRequest : Event {
    /**
     * At least one person is in 'critical state', exceeding his limit
     */
    class SlowDownHeartRateUtterance(val criticalStates: List<UserState>) : HeartRateUtteranceRequest()

    /**
     * Just a status update
     */
    class InfoHeartRateUtterance(val myHeartRate: HeartRate, val myHeartRateLimit: HeartRate) : HeartRateUtteranceRequest()
}

internal fun CoroutineScope.launchHeartRateUtteranceProducer() = launch {

    var group: GroupState? = null
    var criticalUserStates = listOf<UserState>()


    /* Text To Speech: Tell user who is over the limit */
    launch {
        while (true) {
            delay(15.seconds)
            val criticalStates = criticalUserStates.toList()
            if (criticalStates.isNotEmpty()) {
                HeartRateUtteranceRequest.SlowDownHeartRateUtterance(criticalStates).emit()
            }
        }
    }

    /* Text To Speech: Tell heart rate every minute */
    launch {
        while (true) {
            delay(1.minutes)
            val me = group?.members.orEmpty().firstOrNull { it.isMe }
            val heartRate = me?.heartRate?.value?.roundToInt() ?: continue
            val limit = me.heartRateLimit?.value?.roundToInt() ?: continue

            HeartRateUtteranceRequest.InfoHeartRateUtterance(
                myHeartRate = HeartRate(heartRate),
                myHeartRateLimit = HeartRate(limit)
            ).emit()
        }
    }

    /* Collect critical member states */
    GroupState.get().collect { state ->
        group = state
        criticalUserStates = state.members.filter { memberState ->
            val currentHeartRate = memberState.heartRate
            val currentHeartRateLimit = memberState.heartRateLimit ?: return@filter false
            currentHeartRate > currentHeartRateLimit
        }
    }
}
