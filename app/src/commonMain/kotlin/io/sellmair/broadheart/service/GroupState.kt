package io.sellmair.broadheart.service

import io.sellmair.broadheart.User
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.hrSensor.HrSensorInfo
import kotlinx.serialization.Serializable

data class GroupState(val members: List<GroupMemberState>)

@Serializable
class GroupMemberState(
    val user: User?,
    val currentHeartRate: HeartRate?,
    val upperHeartRateLimit: HeartRate?,
    val sensorInfo: HrSensorInfo?
)