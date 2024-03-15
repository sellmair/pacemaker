package io.sellmair.pacemaker.ui

import io.sellmair.pacemaker.HeartRateUtteranceRequest
import io.sellmair.pacemaker.UtteranceEvent
import io.sellmair.pacemaker.utils.collectEventsAsync
import io.sellmair.pacemaker.utils.emit
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString
import pacemaker.app.generated.resources.*
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.heart_rate_info_utterance
import pacemaker.app.generated.resources.slow_down_heart_rate_utterance_fragment_me
import pacemaker.app.generated.resources.slow_down_heart_rate_utterance_fragment_other
import kotlin.math.roundToInt

fun CoroutineScope.launchHeartRateUtteranceActor() = collectEventsAsync<HeartRateUtteranceRequest> { request ->
    request.toUtterance().emit()
}

private suspend fun HeartRateUtteranceRequest.toUtterance() = UtteranceEvent(
    type = when (this) {
        is HeartRateUtteranceRequest.InfoHeartRateUtterance -> UtteranceEvent.Type.Info
        is HeartRateUtteranceRequest.SlowDownHeartRateUtterance -> UtteranceEvent.Type.Warning
    },
    message = this.createMessage()
)

private suspend fun HeartRateUtteranceRequest.createMessage(): String = when (this) {
    is HeartRateUtteranceRequest.InfoHeartRateUtterance -> createInfoHeartRateUtteranceMessage()
    is HeartRateUtteranceRequest.SlowDownHeartRateUtterance -> createSlowDownHeartRateUtteranceMessage()
}

private suspend fun HeartRateUtteranceRequest.InfoHeartRateUtterance.createInfoHeartRateUtteranceMessage(): String {
    return getString(
        Res.string.heart_rate_info_utterance,
        myHeartRate.value.roundToInt(),
        myHeartRateLimit.value.roundToInt()
    )
}

private suspend fun HeartRateUtteranceRequest.SlowDownHeartRateUtterance.createSlowDownHeartRateUtteranceMessage(): String {
    return getString(Res.string.slow_down) + " " + criticalStates
        .sortedBy { it.isMe }
        .map { state ->
            if (state.isMe) getString(
                Res.string.slow_down_heart_rate_utterance_fragment_me,
                state.heartRate.value.roundToInt()
            )
            else getString(
                Res.string.slow_down_heart_rate_utterance_fragment_other,
                state.user.name,
                state.heartRate.value.roundToInt()
            )
        }.joinToString(". ")
}
