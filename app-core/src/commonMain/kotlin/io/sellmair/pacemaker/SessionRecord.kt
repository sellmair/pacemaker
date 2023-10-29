package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.SessionId
import io.sellmair.pacemaker.model.UserId
import kotlinx.datetime.Instant

data class SessionRecord(
    val sessionId: SessionId,
    val userId: UserId,
    val time: Instant,
    val heartRate: HeartRate,
    val heartRateLimit: HeartRate?
)