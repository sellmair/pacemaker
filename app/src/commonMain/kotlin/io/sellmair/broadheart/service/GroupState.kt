package io.sellmair.broadheart.service

import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorInfo
import kotlinx.serialization.Serializable

data class GroupState(val members: List<GroupMemberState>)

@Serializable
data class GroupMemberState(
    val user: User?,
    val currentHeartRate: HeartRate?,
    val upperHeartRateLimit: HeartRate?,
    val sensorInfo: HeartRateSensorInfo?
)