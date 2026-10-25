package io.sellmair.pacemaker

import io.sellmair.evas.*
import io.sellmair.pacemaker.ActorIn.AddMeasurement
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock

data class GroupState(val members: List<UserState> = emptyList()) : Iterable<UserState>, State {
    override fun iterator(): Iterator<UserState> {
        return members.listIterator()
    }

    companion object Key : State.Key<GroupState> {
        override val default = GroupState(emptyList())
        val CONNECTION_LOST_RETENTION = 1.minutes
    }

    object KeepMeasurementDuration : ConfigurationKey.WithDefault<Duration> {
        override val default: Duration = 1.minutes
    }
}

internal fun CoroutineScope.launchGroupStateActor(
    userService: UserService, actorContext: CoroutineContext = Dispatchers.Main.immediate,
    clock: Clock = Clock.System
) {
    val actorIn = Channel<ActorIn>(Channel.UNLIMITED)
    val measurements = linkedMapOf<UserId, AddMeasurement>()
    val lostConnections = mutableSetOf<UserId>()
    val expiryJobs = mutableMapOf<UserId, Job>()
    val meUserId = async { userService.me().id }
    val colors = hashMapOf<UserId, HSLColor>()

    suspend fun StateProducerScope<GroupState>.updateAndEmitGroup() {
        val userStates = measurements.mapNotNull { (userId, measurement) ->
            val user = userService.findUser(userId) ?: return@mapNotNull null
            UserState(
                user = user,
                isMe = meUserId.await() == user.id,
                heartRate = measurement.heartRate,
                heartRateLimit = userService.findHeartRateLimit(user),
                color = colors.getOrElse(userId) { UserColors.default(userId) },
                measuredAtEpochMillis = measurement.time.toEpochMilliseconds(),
                isConnectionLost = userId in lostConnections
            )
        }

        GroupState(userStates).emit()
    }

    /* Main actor */
    launchState(GroupState.Key, actorContext) {
        actorIn.consumeEach { event ->
            when (event) {
                is AddMeasurement -> {
                    val previous = measurements[event.user.id]
                    if (previous != null && event.time < previous.time) return@consumeEach
                    measurements[event.user.id] = event
                    lostConnections.remove(event.user.id)
                    expiryJobs.remove(event.user.id)?.cancel()
                    updateAndEmitGroup()

                    /* Schedule invalidation of measurement after certain amount of time */
                    expiryJobs[event.user.id] = launch {
                        val elapsed = clock.now() - event.time
                        val remaining = (event.keepFor - elapsed).coerceAtLeast(Duration.ZERO)
                        delay(remaining)
                        actorIn.send(ActorIn.MarkConnectionLost(event.user.id, event))
                    }
                }

                is ActorIn.MarkConnectionLost -> {
                    if (measurements[event.userId] === event.message) {
                        if (event.userId == meUserId.await()) {
                            measurements.remove(event.userId)
                            expiryJobs.remove(event.userId)
                        } else {
                            lostConnections.add(event.userId)
                            expiryJobs[event.userId] = launch {
                                delay(GroupState.CONNECTION_LOST_RETENTION)
                                actorIn.send(ActorIn.Discard(event.userId, event.message))
                            }
                        }
                        updateAndEmitGroup()
                    }
                }

                is ActorIn.Discard -> {
                    if (measurements[event.userId] === event.message) {
                        measurements.remove(event.userId)
                        lostConnections.remove(event.userId)
                        expiryJobs.remove(event.userId)
                        updateAndEmitGroup()
                    }
                }

                is ActorIn.RecalculateGroup -> {
                    updateAndEmitGroup()
                }

                is ActorIn.ClearOwn -> {
                    val ownId = meUserId.await()
                    measurements.remove(ownId)
                    expiryJobs.remove(ownId)?.cancel()
                    updateAndEmitGroup()
                }
            }
        }
    }

    /* Check for changes in UserService and refresh the group */
    launch(actorContext) {
        userService.onChange.collect {
            actorIn.send(ActorIn.RecalculateGroup)
        }
    }

    launch(actorContext) {
        SelectedHeartRateSourceState.flow().map { state ->
            state.source?.let { it.id to it.companionDeviceId }
        }.distinctUntilChanged().collect {
            actorIn.send(ActorIn.ClearOwn)
        }
    }

    /* Listen for incoming HeartRate measurements */
    collectEventsAsync<HeartRateMeasurementEvent> { event ->
        val age = clock.now() - event.time
        if (age < Duration.ZERO || age > HeartRateSource.MAX_AGE) return@collectEventsAsync
        val user = userService.findUser(event.sensorId) ?: return@collectEventsAsync
        // Source selection applies to this device's own participant. Other users may
        // still be using independent straps and must continue to appear in the group.
        val ownUserId = MeState.value()?.me?.id
        val selectedSource = SelectedHeartRateSourceState.value()?.source
        if (!acceptsHeartRateMeasurement(selectedSource, ownUserId?.value?.toString().orEmpty(), user.id.value.toString(), event.sensorId.value)) return@collectEventsAsync
        actorIn.send(AddMeasurement(event.heartRate, user, event.time, GroupState.KeepMeasurementDuration.value()))
    }


    /* Listen for incoming Pacemaker Broadcasts measurements */
    collectEventsAsync<PacemakerBroadcastPackageEvent> { event ->
        val user = userService.findUser(event.pkg.userId) ?: return@collectEventsAsync
        event.pkg.userColorHue?.let { hue -> colors[event.pkg.userId] = UserColors.fromHue(hue) }
        actorIn.send(AddMeasurement(event.pkg.heartRate, user, event.pkg.receivedTime, GroupState.KeepMeasurementDuration.value()))
    }

    /* Listen for changes of the current users color */
    launch(actorContext) {
        MeColorState.flow().filterNotNull().collect { meColor ->
            colors[meUserId.await()] = meColor.color
            actorIn.send(ActorIn.RecalculateGroup)
        }
    }
}

private sealed class ActorIn {
    data class AddMeasurement(val heartRate: HeartRate, val user: User, val time: Instant, val keepFor: Duration) : ActorIn()
    data class MarkConnectionLost(val userId: UserId, val message: AddMeasurement) : ActorIn()
    data class Discard(val userId: UserId, val message: AddMeasurement) : ActorIn()
    data object RecalculateGroup : ActorIn()
    data object ClearOwn : ActorIn()
}
