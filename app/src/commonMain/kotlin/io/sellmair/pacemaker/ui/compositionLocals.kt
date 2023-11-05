package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import io.sellmair.pacemaker.SessionService
import io.sellmair.pacemaker.utils.Event
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.StateBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


val LocalStateBus = staticCompositionLocalOf<StateBus?> { null }
val LocalEventBus = staticCompositionLocalOf<EventBus?> { null }

val LocalSessionService = staticCompositionLocalOf<SessionService?> { null }

@Composable
fun <T : State?> State.Key<T>.get(): StateFlow<T> {
    return LocalStateBus.current?.getState(this) ?: MutableStateFlow(default)
}

@Composable
fun <T : State?> State.Key<T>.collectAsState(): androidx.compose.runtime.State<T> {
    return get().collectAsState()
}

@Composable
fun Event.emit() {
    val eventBus = LocalEventBus.current ?: return
    rememberCoroutineScope().launch {
        eventBus.emit(this@emit)
    }
}
