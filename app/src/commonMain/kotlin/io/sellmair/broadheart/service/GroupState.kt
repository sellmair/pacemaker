package io.sellmair.broadheart.service

import io.sellmair.broadheart.User
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.hrSensor.HrSensorInfo

data class GroupState(val members: List<GroupMemberState>)

class GroupMemberState(
    val user: User?,
    val currentHeartRate: HeartRate?,
    val upperHeartRateLimit: HeartRate?,
    val sensorInfo: HrSensorInfo?
)