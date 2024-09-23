package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.randomUserId
import io.sellmair.evas.Event
import io.sellmair.evas.collectEvents
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

sealed class AdhocUserIntent : Event {
    data class CreateAdhocUser(val sensor: HeartRateSensorId) : AdhocUserIntent()
    data class UpdateAdhocUser(val user: User) : AdhocUserIntent()
    data class DeleteAdhocUser(val user: User) : AdhocUserIntent()
    data class UpdateAdhocUserLimit(val user: User, val limit: HeartRate) : AdhocUserIntent()
}

internal fun CoroutineScope.launchAdhocUserActor(userService: UserService): Job = launch(Dispatchers.Main.immediate) {
    suspend fun createAdhocUser(intent: AdhocUserIntent.CreateAdhocUser) {
        val id = randomUserId()
        val adhocUser = User(
            id = id,
            name = "Adhoc ${id.value.absoluteValue % 1000}",
            isAdhoc = true
        )
        userService.saveUser(adhocUser)
        userService.linkSensor(adhocUser, intent.sensor)
        userService.saveHeartRateLimit(adhocUser, HeartRate(130))
    }

    suspend fun updateAdhocUser(intent: AdhocUserIntent.UpdateAdhocUser) {
        userService.saveUser(intent.user)
    }

    suspend fun deleteAdhocUser(intent: AdhocUserIntent.DeleteAdhocUser) {
        userService.deleteUser(intent.user)
    }

    suspend fun updateAdhocUserLimit(intent: AdhocUserIntent.UpdateAdhocUserLimit) {
        userService.saveHeartRateLimit(intent.user, intent.limit)
    }

    collectEvents<AdhocUserIntent> { intent ->
        when (intent) {
            is AdhocUserIntent.CreateAdhocUser -> createAdhocUser(intent)
            is AdhocUserIntent.DeleteAdhocUser -> deleteAdhocUser(intent)
            is AdhocUserIntent.UpdateAdhocUser -> updateAdhocUser(intent)
            is AdhocUserIntent.UpdateAdhocUserLimit -> updateAdhocUserLimit(intent)
        }
    }
}
