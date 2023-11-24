package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.Event
import io.sellmair.pacemaker.utils.collectEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface HeartRateSensorLinkingIntent : Event {
    data class LinkSensor(val user: User, val sensor: HeartRateSensorId) : HeartRateSensorLinkingIntent
    data class UnlinkSensor(val sensor: HeartRateSensorId) : HeartRateSensorLinkingIntent
}


internal fun CoroutineScope.launchHeartRateSensorLinkingActor(userService: UserService): Job = launch(Dispatchers.Main.immediate) {
    collectEvents<HeartRateSensorLinkingIntent> { intent ->
        when (intent) {
            is HeartRateSensorLinkingIntent.LinkSensor -> userService.linkSensor(intent.user, intent.sensor)
            is HeartRateSensorLinkingIntent.UnlinkSensor -> userService.unlinkSensor(intent.sensor)
        }
    }
}