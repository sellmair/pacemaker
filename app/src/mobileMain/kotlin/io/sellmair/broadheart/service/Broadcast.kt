package io.sellmair.broadheart.service

import io.sellmair.broadheart.model.UserId
import io.sellmair.broadheart.model.HeartRate

data class Broadcast(
    val userId: UserId,
    val userName: String,
    val currentHeartRate: HeartRate,
    val heartRateLimit: HeartRate
)

