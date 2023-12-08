package io.sellmair.pacemaker.utils

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface State {
    interface Key<T : State?> {
        val default: T
    }

    interface Producer {
        fun <T : State?> launchIfApplicable(key: Key<T>, state: MutableStateFlow<T>)
    }
}

fun StateBus(): StateBus = StateBusImpl()

interface StateBus : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Key
    fun <T : State?> setState(key: State.Key<T>, value: T)
    fun <T : State?> getState(key: State.Key<T>): StateFlow<T>
    suspend fun <T: State?> setState(key: State.Key<T>, values: Flow<T>)

    fun registerProducer(producer: State.Producer)


    companion object Key : CoroutineContext.Key<StateBus>
}

private class StateBusImpl : StateBus {

    private val lock = reentrantLock()

    private val states = hashMapOf<State.Key<*>, MutableStateFlow<*>>()

    private val producers = mutableListOf<State.Producer>()

    override fun <T : State?> setState(key: State.Key<T>, value: T) {
        getOrCreateMutableStateFlow(key).value = value
    }

    override suspend fun <T : State?> setState(key: State.Key<T>, values: Flow<T>) {
        getOrCreateMutableStateFlow(key).emitAll(values)
    }

    @Suppress("UNCHECKED_CAST")
    override fun registerProducer(producer: State.Producer) = lock.withLock {
        producers.add(producer)
        states.forEach { (key, state) ->
            key as State.Key<State?>
            state as MutableStateFlow<State?>
            producer.launchIfApplicable(key, state)
        }
    }

    override fun <T : State?> getState(key: State.Key<T>): StateFlow<T> {
        return getOrCreateMutableStateFlow(key).asStateFlow()
    }

    private fun <T : State?> getOrCreateMutableStateFlow(key: State.Key<T>): MutableStateFlow<T> = lock.withLock {
        @Suppress("UNCHECKED_CAST")
        return states.getOrPut(key) {
            MutableStateFlow(key.default).also { state ->
                producers.forEach { producer ->
                    producer.launchIfApplicable(key, state)
                }
            }
        } as MutableStateFlow<T>
    }
}


val CoroutineContext.stateBus: StateBus
    get() = this[StateBus] ?: error("Missing ${StateBus::class.simpleName}")


suspend fun <T : State?> State.Key<T>.get(): StateFlow<T> {
    return coroutineContext.stateBus.getState(this)
}

suspend infix fun <T : State?> State.Key<T>.set(value: T) {
    return coroutineContext.stateBus.setState(this, value)
}
