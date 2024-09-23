package io.sellmair.pacemaker

import androidx.compose.runtime.staticCompositionLocalOf
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.ConfigurationKey
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

val LocalSessionService = staticCompositionLocalOf<SessionService?> { null }

interface SessionService {
    object SessionClock : ConfigurationKey.WithDefault<Clock> {
        override val default: Clock = Clock.System
    }

    suspend fun createSession(): ActiveSessionService
    suspend fun getSessions(): List<StoredSessionService>

    val sessionsFlow: Flow<List<StoredSessionService>>
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
    suspend fun getUsers(): List<User>
    suspend fun getHeartRateMeasurements(user: User): List<SessionRecord>
}
