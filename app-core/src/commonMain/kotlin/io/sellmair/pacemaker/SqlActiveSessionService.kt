package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.value
import kotlinx.datetime.Instant

internal class SqlActiveSessionService(
    override val session: Session,
    private val database: PacemakerDatabase
) : ActiveSessionService {

    override suspend fun stop() {
        database.sessionQueries.endSession(SessionService.SessionClock.value().now().toString(), session.id.value)
    }

    override suspend fun save(
        user: User,
        heartRate: HeartRate,
        heartRateLimit: HeartRate?,
        measurementTime: Instant
    ) {
        database.sessionQueries.saveHeartRateMeasurement(
            session_id = session.id.value,
            user_id = user.id.value,
            time = measurementTime.toString(),
            heart_rate = heartRate.value.toDouble(),
            heart_rate_limit = heartRateLimit?.value?.toDouble()
        )
    }
}

