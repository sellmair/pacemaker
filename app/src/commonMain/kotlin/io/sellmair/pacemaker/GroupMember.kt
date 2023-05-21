package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import io.sellmair.pacemaker.model.User

data class GroupMember(
    val user: User?,
    val currentHeartRate: HeartRate?,
    val heartRateLimit: HeartRate?,
    val sensorInfo: HeartRateSensorInfo?
)