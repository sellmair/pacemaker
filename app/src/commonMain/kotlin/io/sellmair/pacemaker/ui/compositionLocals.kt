package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import io.sellmair.pacemaker.SessionService
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.StateBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


val LocalStateBus = staticCompositionLocalOf<StateBus?> { null }
val LocalEventBus = staticCompositionLocalOf<EventBus?> { null }

val LocalSessionService = staticCompositionLocalOf<SessionService?> { null }

@Composable
fun <T : State?> State.Key<T>.get(): StateFlow<T> {
    return LocalStateBus.current?.getState(this) ?: MutableStateFlow(default)
}

@Composable
fun <T : State?> State.Key<T>.set(value: T) {
    LocalStateBus.current?.setState(this, value)
}

@Composable
fun <T : State?> State.Key<T>.collectAsState(): androidx.compose.runtime.State<T> {
    return get().collectAsState()
}
