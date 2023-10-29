package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.SessionId
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.value

internal class SqlSessionService(private val database: PacemakerDatabase) : SessionService {
    override suspend fun createSession(): ActiveSessionService = database.transactionWithResult {
        val startTime = SessionService.SessionClock.value().now()
        database.sessionQueries.newSession(startTime.toString())
        val id = database.sessionQueries.lastSessionId().executeAsOne()
        SqlActiveSessionService(Session(SessionId(id), startTime, null), database)
    }

    override suspend fun getSessions(): List<StoredSessionService> {
        return emptyList()
    }
}