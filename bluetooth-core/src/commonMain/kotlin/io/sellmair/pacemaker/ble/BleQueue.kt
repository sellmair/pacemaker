package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.BleQueue.Timeout
import io.sellmair.pacemaker.utils.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * Queue used for processing all bluetooth operations.
 * This queue will run all dispatched/[enqueue]ed events on a single
 * loop on a given dispatcher.
 *
 * The timeout can be overwritten using [Timeout]
 */
internal class BleQueue(
    job: Job,
    private val context: Context = Context.empty,
) {

    /**
     * Timeout used for any given operation:
     * Can be passed by [BleQueue.context] or for each individual [BleQueue.enqueue] 'context
     */
    data class Timeout(val value: Duration) : Context.Element<Timeout> {
        override val key: Context.Key<Timeout> = Key

        companion object Key : Context.Key.WithDefault<Timeout> {
            override val default: Timeout = Timeout(30.seconds)
        }
    }

    /**
     * Title for a given operation
     */
    internal class OperationTitle(val value: String) : Context.Element<OperationTitle> {
        override val key: Context.Key<OperationTitle> = Key

        companion object Key : Context.Key<OperationTitle>
    }

    /**
     * Can be used to overwrite/extend the dispatcher used for the queue to process
     * incoming operations/actions
     */
    data class QueueDispatcher(val dispatcher: CoroutineDispatcher) : Context.Element<QueueDispatcher> {
        override val key: Context.Key<QueueDispatcher> = Key

        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        companion object Key : Context.Key.WithDefault<QueueDispatcher> {
            private val dispatcher: CoroutineDispatcher by lazy { newSingleThreadContext("BleQueue") }
            override val default: QueueDispatcher = QueueDispatcher(dispatcher)
        }
    }

    suspend fun <T> enqueue(
        context: Context = Context.empty, action: suspend CoroutineScope.() -> BleResult<T>
    ): BleResult<T> {
        check(executor.isActive) { "${BleQueue::class.simpleName}: Expected executor.isActive" }
        val operation = Operation(this.context + context, action)
        queue.send(operation)
        val result = operation.await()
        return result
    }

    private class Operation<T>(
        val context: Context,
        val action: suspend CoroutineScope.() -> BleResult<T>,
    ) {
        private val result = CompletableDeferred<BleResult<T>>()
        fun complete(result: BleResult<T>) = this.result.complete(result)
        suspend fun await() = result.await()
    }

    private val coroutineScope = CoroutineScope(Job(job) + context[QueueDispatcher].dispatcher)

    private val queue = Channel<Operation<*>>()

    private val executor = coroutineScope.launch {
        suspend fun <T> Operation<T>.execute() {
            val timeoutDuration = context[Timeout].value
            val result = try {
                withTimeout(timeoutDuration) {
                    coroutineScope {
                        action()
                    }
                }
            } catch (t: TimeoutCancellationException) {
                BleFailure.Timeout(timeoutDuration)
            } catch (t: Throwable) {
                BleFailure.Error(t)
            }

            complete(result)
        }

        queue.consumeEach { operation -> operation.execute() }
    }
}
