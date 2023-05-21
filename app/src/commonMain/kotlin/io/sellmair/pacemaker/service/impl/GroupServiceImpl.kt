package io.sellmair.pacemaker.service.impl

import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.GroupMember
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.service.GroupService
import io.sellmair.pacemaker.service.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

class GroupServiceImpl(
    private val userService: UserService
) : GroupService {
    private val measurements = mutableListOf<HeartRateMeasurement>()
    private val foreignStates = mutableMapOf<UserId, GroupMember>()
    private val _groupState = MutableStateFlow(Group(emptyList()))
    override val group: StateFlow<Group> = _groupState.asStateFlow()

    override suspend fun add(measurement: HeartRateMeasurement) {
        measurements.add(measurement)
        invalidate()
    }

    override suspend fun add(foreignState: GroupMember) {
        foreignStates[foreignState.user?.id ?: return] = foreignState
    }

    override suspend fun invalidate() {
        val nextState = withContext(Dispatchers.Default) {
            calculateGroupState(userService, measurements.toList(), foreignStates.values.toList())
        }

        _groupState.emit(nextState)
    }
}

private suspend fun calculateGroupState(
    userService: UserService,
    measurements: List<HeartRateMeasurement>,
    foreignStates: List<GroupMember>
): Group {

    val measurementsWithinLastMinute = measurements
        .filter { hrMeasurement -> hrMeasurement.receivedTime.elapsedNow() < 1.minutes }
        .sortedBy { hrMeasurement -> hrMeasurement.receivedTime.elapsedNow() }
        .distinctBy { hrMeasurement -> hrMeasurement.sensorInfo.id }

    val memberStates = measurementsWithinLastMinute
        .map { hrMeasurement ->
            val user = foreignStates.find { it.sensorInfo?.id == hrMeasurement.sensorInfo.id }?.user
                ?: userService.findUser(hrMeasurement.sensorInfo)
            GroupMember(
                user = user,
                currentHeartRate = hrMeasurement.heartRate,
                heartRateLimit = user?.let { userService.findUpperHeartRateLimit(it) },
                sensorInfo = hrMeasurement.sensorInfo
            )
        }
        .plus(foreignStates)
        .distinctBy { it.user ?: it.sensorInfo?.id }


    return Group(memberStates)
}