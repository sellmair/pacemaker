package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.Session
import io.sellmair.evas.State
import io.sellmair.evas.StateProducerStarted
import io.sellmair.evas.launchState
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

internal fun CoroutineScope.launchSessionStateActor(sessionService: SessionService): Job = launchState(
    SessionsState.Key, started = StateProducerStarted.Lazily
) {
    val sessionsStateFlow = sessionService.sessionsFlow
        .map { sessionServices -> sessionServices.map { it.session }.sortedBy { it.startTime } }
        .map { sessions -> SessionsState(sessions) }
        .distinctUntilChanged()

    emitAll(sessionsStateFlow)
}
