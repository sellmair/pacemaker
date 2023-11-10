package io.sellmair.pacemaker.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass
import kotlin.time.Duration

private typealias Producer<K, T> = suspend FlowCollector<T>.(key: K) -> Unit

inline fun <reified K : State.Key<T>, T : State?> CoroutineScope.launchStateProducer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    noinline onInactive: suspend FlowCollector<T>.(key: K) -> Unit = OnInactive.resetValue(),
    noinline produce: suspend FlowCollector<T>.(key: K) -> Unit
): Job {
    val newCoroutineContext = (this.coroutineContext + coroutineContext).let { base -> base + Job(base.job) }
    val coroutineScope = CoroutineScope(newCoroutineContext)
    val producer = ColdStateProducerImpl(
        coroutineScope = coroutineScope,
        keyClazz = K::class,
        onInactive = onInactive,
        onActive = produce,
    )
    newCoroutineContext.stateBus.registerProducer(producer)
    return newCoroutineContext.job
}


fun <T : State?> CoroutineScope.launchStateProducer(
    key: State.Key<T>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    started: SharingStarted = SharingStarted.Eagerly,
    produce: suspend FlowCollector<T>.() -> Unit
): Job {
    val newCoroutineContext = (this.coroutineContext + coroutineContext).let { base -> base + Job(base.job) }
    val coroutineScope = CoroutineScope(newCoroutineContext)

    val hotFlow = flow(produce).shareIn(coroutineScope, started, replay = 1)

    coroutineScope.launch {
        currentCoroutineContext().stateBus.setState(key, hotFlow)
    }

    return coroutineScope.coroutineContext.job
}

object OnInactive {
    fun <K : State.Key<T>, T : State?> keepValue(): Producer<K, T> = {}

    fun <K : State.Key<T>, T : State?> resetValue(resetDelay: Duration = Duration.ZERO): Producer<K, T> = { key ->
        delay(resetDelay)
        emit(key.default)
    }
}


@PublishedApi
internal class ColdStateProducerImpl<K : State.Key<T>, T : State?>(
    private val coroutineScope: CoroutineScope,
    private val keyClazz: KClass<K>,
    private val onInactive: suspend FlowCollector<T>.(key: K) -> Unit,
    private val onActive: suspend FlowCollector<T>.(key: K) -> Unit
) : State.Producer {
    @Suppress("UNCHECKED_CAST")
    override fun <X : State?> launchIfApplicable(key: State.Key<X>, state: MutableStateFlow<X>) {
        if (!keyClazz.isInstance(key)) return
        key as K
        state as MutableStateFlow<T>
        coroutineScope.launch { produceState(key, state) }
    }

    private suspend fun produceState(key: K, state: MutableStateFlow<T>) {
        state.subscriptionCount
            .map { subscriptionCount -> subscriptionCount > 0 }
            .distinctUntilChanged()
            .collectLatest { isSubscribed ->
                if (isSubscribed && coroutineContext.isActive) {
                    state.emitAll(flow { onActive(key) })
                }

                if(!isSubscribed) {
                    state.emitAll(flow { onInactive(key) })
                }
            }
    }
}
