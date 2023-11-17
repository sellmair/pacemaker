package io.sellmair.pacemaker.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass
import kotlin.time.Duration

private typealias Producer<K, T> = suspend ColdStateProducerScope<T>.(key: K) -> Unit

inline fun <reified K : State.Key<T>, T : State?> CoroutineScope.launchStateProducer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    keepActive: Duration = Duration.ZERO,
    noinline onInactive: suspend ColdStateProducerScope<T>.(key: K) -> Unit = OnInactive.resetValue(),
    noinline produce: suspend ColdStateProducerScope<T>.(key: K) -> Unit
): Job {
    val newCoroutineContext = (this.coroutineContext + coroutineContext).let { base -> base + Job(base.job) }
    val coroutineScope = CoroutineScope(newCoroutineContext)
    val producer = ColdStateProducerImpl(
        coroutineScope = coroutineScope,
        keyClazz = K::class,
        keepActive = keepActive,
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
    fun <K : State.Key<T>, T : State?> resetValue(): Producer<K, T> = { key -> key.default.emit() }
}


@PublishedApi
internal class ColdStateProducerImpl<K : State.Key<T>, T : State?>(
    private val coroutineScope: CoroutineScope,
    private val keyClazz: KClass<K>,
    private val keepActive: Duration = Duration.ZERO,
    private val onInactive: suspend ColdStateProducerScope<T>.(key: K) -> Unit,
    private val onActive: suspend ColdStateProducerScope<T>.(key: K) -> Unit
) : State.Producer {
    @Suppress("UNCHECKED_CAST")
    override fun <X : State?> launchIfApplicable(key: State.Key<X>, state: MutableStateFlow<X>) {
        if (!keyClazz.isInstance(key)) return
        key as K
        state as MutableStateFlow<T>
        coroutineScope.launch { produceState(key, state) }
    }

    @OptIn(FlowPreview::class)
    private suspend fun produceState(key: K, state: MutableStateFlow<T>) {
        state.subscriptionCount
            .map { subscriptionCount -> subscriptionCount > 0 }
            .distinctUntilChanged()
            .debounce { isSubscribed -> if (!isSubscribed) keepActive else Duration.ZERO }
            .distinctUntilChanged()
            .collectLatest { isActive ->
                if (isActive && coroutineContext.isActive) {
                    state.emitAll(coldStateFlow { onActive(key) })
                }

                if (!isActive) {
                    state.emitAll(coldStateFlow { onInactive(key) })
                }
            }
    }
}

@OptIn(ExperimentalTypeInference::class)
private fun <T : State?> coldStateFlow(@BuilderInference block: suspend ColdStateProducerScope<T>.() -> Unit): Flow<T> {
    return channelFlow {
        with(ColdStateProducerScope(this)) {
            block()
        }
    }
}

class ColdStateProducerScope<T : State?> internal constructor(
    private val scope: ProducerScope<T>
) : ProducerScope<T> by scope {
    suspend fun T.emit() = channel.send(this)
    suspend infix fun emitAll(flow: Flow<T>) {
        flow.collect { element -> channel.send(element) }
    }
}

