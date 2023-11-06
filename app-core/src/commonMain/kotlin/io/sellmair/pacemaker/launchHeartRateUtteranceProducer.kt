package io.sellmair.pacemaker

import UtteranceEvent
import io.sellmair.pacemaker.utils.emit
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


internal fun CoroutineScope.launchHeartRateUtteranceProducer() = launch {

    var group: GroupState? = null
    var criticalUserStates = listOf<UserState>()


    /* Text To Speech: Tell user who is over the limit */
    launch {
        while (true) {
            delay(15.seconds)
            val criticalStates = criticalUserStates.toList()
            if (criticalStates.isNotEmpty()) {
                launch textToSpeech@{
                    val message = "Slow down! ${
                        if (criticalStates.singleOrNull()?.isMe == true) {
                            "You are at " +
                                "${criticalStates.singleOrNull()?.heartRate?.value?.roundToInt()} bpm"
                        } else criticalStates.joinToString(", ") {
                            "${it.user.name} is at ${it.heartRate.value.roundToInt()} bpm"
                        }
                    }"

                    UtteranceEvent(UtteranceEvent.Type.Warning, message).emit()
                }
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
                val message = "Your heart rate is at: $heartRate. The current limit is: $limit"
                UtteranceEvent(UtteranceEvent.Type.Info, message).emit()
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
