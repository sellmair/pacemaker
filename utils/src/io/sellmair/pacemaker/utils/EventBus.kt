package io.sellmair.pacemaker.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

interface Event

fun EventBus(): EventBus = EventBusImpl()

interface EventBus : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Key
    val events: SharedFlow<Event>
    suspend fun emit(event: Event)

    companion object Key : CoroutineContext.Key<EventBus>
}

private class EventBusImpl : EventBus {
    private val eventsImpl = MutableSharedFlow<Event>()
    override val events: SharedFlow<Event> = eventsImpl.asSharedFlow()

    override suspend fun emit(event: Event) {
        eventsImpl.emit(event)
    }
}

val CoroutineContext.eventBus: EventBus
    get() = this[EventBus] ?: error("Missing ${EventBus::class.simpleName}")


suspend inline fun <reified T : Event> events(): Flow<T> {
    return coroutineContext.eventBus.events.filterIsInstance<T>().buffer(Channel.UNLIMITED)
}

suspend inline fun <reified T : Event> collectEvents(noinline collector: suspend (T) -> Unit) {
    events<T>().collect(collector)
}

inline fun <reified T : Event> CoroutineScope.collectEventsAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline collector: suspend (T) -> Unit
): Job = launch(context = context) { collectEvents<T>(collector) }

suspend fun Event.emit() {
    coroutineContext.eventBus.emit(this)
}

val <T> FlowCollector<T>.emit: suspend T.() -> Unit get() = { emit(this) }