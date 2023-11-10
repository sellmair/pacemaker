package io.sellmair.pacemaker

import app.cash.sqldelight.coroutines.asFlow
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.SessionId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

internal class SqlSessionService(private val database: PacemakerDatabase) : SessionService {
    override suspend fun createSession(): ActiveSessionService = database.transactionWithResult {
        val startTime = SessionService.SessionClock.value().now()
        database.sessionQueries.newSession(startTime.toString())
        val id = database.sessionQueries.lastSessionId().executeAsOne()
        SqlActiveSessionService(Session(SessionId(id), startTime, null), database)
    }

    override suspend fun getSessions(): List<StoredSessionService> {
        return database.sessionQueries.allSessions().executeAsList().map { dbSession ->
            SqlStoredSessionService(
                session = dbSession.toSession(),
                database = database
            )
        }
    }

    override val sessionsFlow: Flow<List<StoredSessionService>> = database.sessionQueries.allSessions()
        .asFlow().map { query -> query.executeAsList() }
        .map { values -> values.map { it.toStroedSessionService() } }

    private fun Db_session.toStroedSessionService() = SqlStoredSessionService(
        session = toSession(),
        database = database
    )

}

private class SqlStoredSessionService(
    override val session: Session,
    private val database: PacemakerDatabase
) : StoredSessionService {

    override suspend fun getUsers(): List<User> {
        return withContext(Dispatchers.IO) {
            database.sessionQueries.findUsers(session_id = session.id.value).executeAsList()
                .mapNotNull { userId -> database.userQueries.findUserById(userId).executeAsOneOrNull() }
                .map { it.toUser() }
        }
    }

    override suspend fun getHeartRateMeasurements(user: User): List<SessionRecord> {
        return database.sessionQueries.findHeartRateMeasurements(session.id.value).executeAsList().map { dbRecord ->
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

