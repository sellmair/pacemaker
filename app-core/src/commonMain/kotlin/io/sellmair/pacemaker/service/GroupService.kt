package io.sellmair.pacemaker.service

import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

context(Configuration, CoroutineScope)
fun GroupService(
    userService: UserService
): GroupService = GroupServiceImpl(userService)

interface GroupService {
    val group: StateFlow<Group>
    fun add(measurement: HeartRateMeasurement)

    object KeepMeasurementDuration : Configuration.Key.WithDefault<Duration> {
        override val default: Duration = 1.minutes
    }
}

context(Configuration, CoroutineScope)
private class GroupServiceImpl(
    private val userService: UserService
) : GroupService {
    private val groupImpl = MutableStateFlow(Group())
    override val group: StateFlow<Group> = groupImpl.asStateFlow()

    sealed class Event {
        data class AddMeasurement(val measurement: HeartRateMeasurement) : Event()
        data class DiscardMeasurement(val userId: UserId, val measurement: HeartRateMeasurement) : Event()
        data object RecalculateGroup : Event()
    }

    private val events = Channel<Event>(Channel.UNLIMITED)

    override fun add(measurement: HeartRateMeasurement) {
        events.trySend(Event.AddMeasurement(measurement))
    }

    init {
        val measurements = hashMapOf<UserId, HeartRateMeasurement>()

        suspend fun emitGroup() {
            val userStates = measurements.mapNotNull { (userId, measurement) ->
                val user = userService.findUser(userId) ?: return@mapNotNull null
                UserState(
                    user = user,
                    heartRate = measurement.heartRate,
                    heartRateLimit = userService.findHeartRateLimit(user)
                )
            }

            groupImpl.value = Group(userStates)
        }

        launch(Dispatchers.Main.immediate) {
            events.consumeEach { event ->
                when (event) {
                    is Event.AddMeasurement -> {
                        val user = userService.findUser(event.measurement.sensorInfo.id) ?: return@consumeEach
                        measurements[user.id] = event.measurement
                        emitGroup()

                        /* Schedule invalidation of measurement after certain amount of time */
                        launch {
                            delay(get(GroupService.KeepMeasurementDuration))
                            events.send(Event.DiscardMeasurement(user.id, event.measurement))
                        }
                    }

                    is Event.DiscardMeasurement -> {
                        /* Measurement has not been updated, but is now discared */
                        if (measurements[event.userId] == event.measurement) {
                            measurements.remove(event.userId)
                            emitGroup()
                        }
                    }

                    is Event.RecalculateGroup -> {
                        emitGroup()
                    }
                }
            }
        }
    }

    init {
        launch(Dispatchers.Main.immediate) {
            userService.onChange
                .onEach { events.send(Event.RecalculateGroup) }
                .collect()
        }
    }
}
