package io.sellmair.pacemaker.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlin.coroutines.CoroutineContext
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
    return coroutineContext.eventBus.events.filterIsInstance<T>()
}

suspend inline fun <reified T : Event> events(noinline collector: suspend (T) -> Unit) {
    coroutineContext.eventBus.events.filterIsInstance<T>().collect(collector)
}

suspend fun Event.emit() {
    coroutineContext.eventBus.emit(this)
}