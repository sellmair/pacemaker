package io.sellmair.pacemaker.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface State {
    interface Key<T : State?> {
        val default: T
    }
}

fun StateBus(): StateBus = StateBusImpl()

interface StateBus : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Key
    fun <T : State?> setState(key: State.Key<T>, value: T)
    fun <T : State?> getState(key: State.Key<T>): StateFlow<T>

    companion object Key : CoroutineContext.Key<StateBus>
}

private class StateBusImpl : StateBus {

    private val states = hashMapOf<State.Key<*>, MutableStateFlow<*>>()

    override fun <T : State?> setState(key: State.Key<T>, value: T) {
        getOrCreateMutableStateFlow(key).value = value
    }

    override fun <T : State?> getState(key: State.Key<T>): StateFlow<T> {
        return getOrCreateMutableStateFlow(key).asStateFlow()
    }

    private fun <T : State?> getOrCreateMutableStateFlow(key: State.Key<T>): MutableStateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return states.getOrPut(key) { MutableStateFlow(key.default) } as MutableStateFlow<T>
    }
}

val CoroutineContext.stateBus: StateBus
    get() = this[StateBus] ?: error("Missing ${StateBus::class.simpleName}")


suspend operator fun <T : State?> State.Key<T>.plusAssign(value: T) {
    coroutineContext.stateBus.setState(this, value)
}

suspend fun <T : State?> State.Key<T>.get(): StateFlow<T> {
    return coroutineContext.stateBus.getState(this)
}