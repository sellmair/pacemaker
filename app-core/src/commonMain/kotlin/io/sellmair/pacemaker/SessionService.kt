package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.SessionId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.ConfigurationKey
import io.sellmair.pacemaker.utils.value
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface SessionService {

    data class StoredSessionHeartRateMeasurement(
        val sessionId: SessionId,
        val userId: UserId,
        val time: Instant,
        val heartRate: HeartRate
    )

    object SessionClock : ConfigurationKey.WithDefault<Clock> {
        override val default: Clock = Clock.System
    }

    suspend fun createSession(): ActiveSessionService
}

interface ActiveSessionService {
    suspend fun stop()
    suspend fun save(user: User, measurement: HeartRate, measurementTime: Instant)
}

private class SqlSessionService(private val database: PacemakerDatabase) : SessionService {
    override suspend fun createSession(): ActiveSessionService = database.transactionWithResult {
        val startTime = SessionService.SessionClock.value().now()
        database.sessionQueries.newSession(startTime.toString())
        val id = database.sessionQueries.lastSessionId().executeAsOne()
        SqlActiveSessionService(Session(SessionId(id), startTime, null), database)
    }
}

private class SqlActiveSessionService(
    private val session: Session,
    private val database: PacemakerDatabase
) : ActiveSessionService {

    override suspend fun stop() {
        database.sessionQueries.endSession(SessionService.SessionClock.value().now().toString(), session.id.value)
    }

    override suspend fun save(user: User, measurement: HeartRate, measurementTime: Instant) {
        database.sessionQueries.saveHeartRateMeasurement(
            session_id = session.id.value,
            user_id = user.id.value,
            time = measurementTime.toString(),
            heart_rate = measurement.value.toDouble()
        )
    }
}

