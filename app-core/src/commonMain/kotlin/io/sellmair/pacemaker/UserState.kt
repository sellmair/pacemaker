package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User

data class UserState(
    val user: User,
    val isMe: Boolean,
    val heartRate: HeartRate,
    val heartRateLimit: HeartRate?,
    val color: HSLColor
)