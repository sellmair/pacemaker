package io.sellmair.pacemaker

import io.sellmair.pacemaker.ActorIn.AddMeasurement
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.ConfigurationKey
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.StateProducerScope
import io.sellmair.pacemaker.utils.events
import io.sellmair.pacemaker.utils.launchStateProducer
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class GroupState(val members: List<UserState> = emptyList()) : Iterable<UserState>, State {
    override fun iterator(): Iterator<UserState> {
        return members.listIterator()
    }

    companion object Key : State.Key<GroupState> {
        override val default = GroupState(emptyList())
    }

    object KeepMeasurementDuration : ConfigurationKey.WithDefault<Duration> {
        override val default: Duration = 1.minutes
    }
}

internal fun CoroutineScope.launchGroupStateActor(
    userService: UserService
) {
    val actorIn = Channel<ActorIn>(Channel.UNLIMITED)
    val measurements = hashMapOf<UserId, ActorIn.AddMeasurement>()
    val meUserId = async { userService.me().id }

    suspend fun StateProducerScope<GroupState>.updateAndEmitGroup() {
        val userStates = measurements.mapNotNull { (userId, measurement) ->
            val user = userService.findUser(userId) ?: return@mapNotNull null
            UserState(
                user = user,
                isMe = meUserId.await() == user.id,
                heartRate = measurement.heartRate,
                heartRateLimit = userService.findHeartRateLimit(user)
            )
        }

        GroupState(userStates).emit()
    }

    /* Main actor */
    launchStateProducer(GroupState.Key, Dispatchers.Main.immediate) {
        actorIn.consumeEach { event ->
            when (event) {
                is AddMeasurement -> {
                    measurements[event.user.id] = event
                    updateAndEmitGroup()

                    /* Schedule invalidation of measurement after certain amount of time */
                    launch {
                        delay(GroupState.KeepMeasurementDuration.value())
                        actorIn.send(ActorIn.Discard(event.user.id, event))
                    }
                }

                is ActorIn.Discard -> {
                    /* Measurement has not been updated, but is now discared */
                    if (measurements[event.userId] == event.message) {
                        measurements.remove(event.userId)
                        updateAndEmitGroup()
                    }
                }

                is ActorIn.RecalculateGroup -> {
                    updateAndEmitGroup()
                }
            }
        }
    }

    /* Check for changes in UserService and refresh the group */
    launch(Dispatchers.Main.immediate) {
        userService.onChange.collect {
            actorIn.send(ActorIn.RecalculateGroup)
        }
    }

    /* Listen for incoming HeartRate measurements */
    launch {
        events<HeartRateMeasurementEvent> { event ->
            val user = userService.findUser(event.sensorId) ?: return@events
            actorIn.send(AddMeasurement(event.heartRate, user, event.time))
        }
    }

    /* Listen for incoming Pacemaker Broadcasts measurements */
    launch {
        events<PacemakerBroadcastPackageEvent> { event ->
            val user = userService.findUser(event.pkg.userId) ?: return@events
            actorIn.send(AddMeasurement(event.pkg.heartRate, user, event.pkg.receivedTime))
        }
    }
}

private sealed class ActorIn {
    data class AddMeasurement(val heartRate: HeartRate, val user: User, val time: Instant) : ActorIn()
    data class Discard(val userId: UserId, val message: ActorIn) : ActorIn()
    data object RecalculateGroup : ActorIn()
}