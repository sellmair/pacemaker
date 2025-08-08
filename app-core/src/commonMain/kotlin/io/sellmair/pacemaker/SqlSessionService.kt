package io.sellmair.pacemaker

import app.cash.sqldelight.coroutines.asFlow
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.SessionId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class SqlSessionService(private val database: SafePacemakerDatabase) : SessionService {
    override suspend fun createSession(): ActiveSessionService = database {
        val startTime = SessionService.SessionClock.value().now()
        transactionWithResult {
            sessionQueries.newSession(startTime.toString())
            val id = sessionQueries.lastSessionId().executeAsOne()
            SqlActiveSessionService(Session(SessionId(id), startTime, null), database)
        }
    }

    override suspend fun getSessions(): List<StoredSessionService> {
        return database {
            sessionQueries.allSessions().executeAsList().map { dbSession ->
                SqlStoredSessionService(
                    session = dbSession.toSession(),
                    database = database
                )
            }
        }
    }

    override val sessionsFlow: Flow<List<StoredSessionService>> = database.flow {
        sessionQueries.allSessions()
            .asFlow().map { query -> query.executeAsList() }
            .map { values -> values.map { it.toStroedSessionService() } }
    }

    private fun Db_session.toStroedSessionService() = SqlStoredSessionService(
        session = toSession(),
        database = database
    )

}

private class SqlStoredSessionService(
    override val session: Session,
    private val database: SafePacemakerDatabase
) : StoredSessionService {

    override suspend fun getUsers(): List<User> {
        return database {
            sessionQueries.findUsers(session_id = session.id.value).executeAsList()
                .mapNotNull { userId -> userQueries.findUserById(userId).executeAsOneOrNull() }
                .map { it.toUser() }
        }
    }

    override suspend fun getHeartRateMeasurements(user: User): List<SessionRecord> = database {
        sessionQueries.findHeartRateMeasurements(session.id.value).executeAsList().map { dbRecord ->
            SessionRecord(
                sessionId = session.id,
                userId = UserId(dbRecord.user_id),
                time = Instant.parse(dbRecord.time),
                heartRate = HeartRate(dbRecord.heart_rate.toFloat()),
                heartRateLimit = dbRecord.heart_rate_limit?.let { HeartRate(it.toFloat()) }
            )
        }
    }
}

private fun Db_session.toSession() = Session(
    id = SessionId(id),
    startTime = Instant.parse(start_time),
    endTime = end_time?.let { Instant.parse(end_time) }
)
