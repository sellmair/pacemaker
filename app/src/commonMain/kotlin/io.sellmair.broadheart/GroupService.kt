package io.sellmair.broadheart

import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.minutes

interface GroupService {
    val group: StateFlow<Group>
    suspend fun add(measurement: HeartRateMeasurement)
    suspend fun add(foreignState: GroupMember)
    suspend fun invalidate()
}

suspend fun calculateGroupState(
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