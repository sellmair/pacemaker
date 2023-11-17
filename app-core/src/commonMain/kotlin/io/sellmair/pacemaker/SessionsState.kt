package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.launchStateProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

data class SessionsState(val sessions: List<Session>) : State {
    companion object Key : State.Key<SessionsState> {
        override val default: SessionsState = SessionsState(emptyList())
    }
}

internal fun CoroutineScope.launchSessionStateActor(sessionService: SessionService): Job = launchStateProducer(
    SessionsState.Key, started = Lazily
) {
    val sessionsStateFlow = sessionService.sessionsFlow
        .map { sessionServices -> sessionServices.map { it.session }.sortedBy { it.startTime } }
        .map { sessions -> SessionsState(sessions) }
        .distinctUntilChanged()

    emitAll(sessionsStateFlow)
}
