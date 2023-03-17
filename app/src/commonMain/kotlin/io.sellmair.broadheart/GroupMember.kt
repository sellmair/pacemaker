package io.sellmair.broadheart

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.model.User

data class GroupMember(
    val user: User?,
    val currentHeartRate: HeartRate?,
    val heartRateLimit: HeartRate?,
    val sensorInfo: HeartRateSensorInfo?
)