package io.sellmair.broadheart.service

import io.sellmair.broadheart.hrSensor.HrMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

interface GroupService {
    val groupState: StateFlow<GroupState>
    suspend fun add(measurement: HrMeasurement)
    suspend fun updateState()
}

class DefaultGroupService(
    private val userService: UserService
) : GroupService {
    private val measurements = mutableListOf<HrMeasurement>()
    private val _groupState = MutableStateFlow(GroupState(emptyList()))
    override val groupState: StateFlow<GroupState> = _groupState.asStateFlow()

    override suspend fun add(measurement: HrMeasurement) {
        measurements.add(measurement)
    }

    override suspend fun updateState() {
        val nextState = withContext(Dispatchers.Default) {
            calculateGroupState(userService, measurements.toList())
        }

        _groupState.emit(nextState)
    }
}

suspend fun calculateGroupState(userService: UserService, measurements: List<HrMeasurement>): GroupState {
    val measurementsWithinLastMinute = measurements
        .filter { hrMeasurement -> hrMeasurement.receivedTime.elapsedNow() < 1.minutes }
        .sortedBy { hrMeasurement -> hrMeasurement.receivedTime.elapsedNow() }
        .distinctBy { hrMeasurement -> hrMeasurement.sensorInfo.id }

    val memberStates = measurementsWithinLastMinute
        .map { hrMeasurement ->
            val user = userService.findUser(hrMeasurement.sensorInfo)
            GroupMemberState(
                user = user,
                currentHeartRate = hrMeasurement.heartRate,
                upperHeartRateLimit = user?.let { userService.findUpperHeartRateLimit(it) },
                sensorInfo = hrMeasurement.sensorInfo
            )
        }
        .distinctBy { it.user ?: it.sensorInfo?.id }

    return GroupState(memberStates)
}