package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.ConfigurationKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface SessionService {
    object SessionClock : ConfigurationKey.WithDefault<Clock> {
        override val default: Clock = Clock.System
    }

    suspend fun createSession(): ActiveSessionService
    suspend fun getSessions(): List<StoredSessionService>
}

interface ActiveSessionService {
    val session: Session
    suspend fun stop()
    suspend fun save(
        user: User, heartRate: HeartRate, heartRateLimit: HeartRate?, measurementTime: Instant,
    )
}

interface StoredSessionService {
    val session: Session
    fun getUsers(): List<User>
    fun getHeartRateMeasurements(user: User): List<SessionRecord>
}
