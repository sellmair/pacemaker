package io.sellmair.pacemaker.ui.timelinePage

import io.sellmair.pacemaker.SessionService
import io.sellmair.pacemaker.StoredSessionService
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.SessionId
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class TimelinePageViewModel(
    private val coroutineScope: CoroutineScope,
    private val sessionService: SessionService,
) {
    val sessions: StateFlow<List<TimelineSessionViewModel>> = sessionService.sessionsFlow
        .map { sessions ->
            sessions.associateBy { it.session.id }
                .mapValues { (id, service) ->
                    sessionViewModelsCache[id] ?: TimelineSessionViewModel(coroutineScope, service)
                }
        }
        .flowOn(Dispatchers.IO)
        .onEach { viewModels -> sessionViewModelsCache = viewModels }
        .map { it.values.toList().sortedByDescending { viewModel -> viewModel.session.startTime } }
        .stateIn(coroutineScope, Eagerly, emptyList())


    private var sessionViewModelsCache: Map<SessionId, TimelineSessionViewModel> = emptyMap()

}

class TimelineSessionViewModel(
    coroutineScope: CoroutineScope,
    service: StoredSessionService
) {
    val session: Session = service.session

    val users: StateFlow<List<User>> = flow { emit(service.getUsers()) }
        .flowOn(Dispatchers.IO)
        .stateIn(coroutineScope, Eagerly, emptyList())
}