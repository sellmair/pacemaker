package io.sellmair.pacemaker.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.sellmair.pacemaker.ui.LocalEventBus
import io.sellmair.pacemaker.ui.LocalStateBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun Launching(action: suspend () -> Unit): () -> Unit {
    val scope = rememberPacemakerCoroutineScope()
    return {
        scope.launch {
            action()
        }
    }
}


@Composable
fun<T> Launching(action: suspend (value: T) -> Unit): (T) -> Unit {
    val scope = rememberPacemakerCoroutineScope()
    return { value -> 
        scope.launch {
            action(value)
        }
    }
}


@Composable
fun rememberPacemakerCoroutineScope(): CoroutineScope {
    val eventBus = LocalEventBus.current ?: EmptyCoroutineContext
    val stateBus = LocalStateBus.current ?: EmptyCoroutineContext
    return rememberCoroutineScope { Dispatchers.Main.immediate + eventBus + stateBus }
}
