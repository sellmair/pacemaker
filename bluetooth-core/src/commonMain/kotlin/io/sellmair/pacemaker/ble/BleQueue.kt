package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.BleQueue.Timeout
import io.sellmair.pacemaker.utils.ConfigurationKey
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * Queue used for processing all bluetooth operations.
 * This queue will run all dispatched/[enqueue]ed events on a single
 * loop on a given dispatcher.
 *
 * The timeout can be overwritten using [Timeout]
 */
internal class BleQueue(context: CoroutineContext) {

    /**
     * Timeout used for any given operation
     */
    object Timeout : ConfigurationKey.WithDefault<Duration> {
        override val default: Duration = 30.seconds
    }

    /**
     * Title for a given operation
     */
    internal object OperationTitle : ConfigurationKey<String>

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    object QueueDispatcher : ConfigurationKey.WithDefault<CoroutineDispatcher> {
        override val default: CoroutineDispatcher by lazy { newSingleThreadContext("BleQueue") }
    }

    suspend fun <T> enqueue(action: suspend CoroutineScope.() -> BleResult<T>): BleResult<T> {
        check(executor.isActive) { "${BleQueue::class.simpleName}: Expected executor.isActive" }

        val operation = Operation(
            title = OperationTitle.value(),
            timeout = Timeout.value(),
            action = action
        )

        queue.send(operation)
        val result = operation.await()
        return result
    }

    private class Operation<T>(
        val title: String?,
        val timeout: Duration,
        val action: suspend CoroutineScope.() -> BleResult<T>,
    ) {
        private val result = CompletableDeferred<BleResult<T>>()
        fun complete(result: BleResult<T>) = this.result.complete(result)
        suspend fun await() = result.await()
    }

    private val coroutineScope = CoroutineScope(Job(context.job))

    private val queue = Channel<Operation<*>>()

    private val executor = coroutineScope.launch {
        suspend fun <T> Operation<T>.execute() {
            val result = try {
                withTimeout(timeout) {
                    coroutineScope {
                        action()
                    }
                }
            } catch (t: TimeoutCancellationException) {
                BleFailure.Timeout(timeout)
            } catch (t: Throwable) {
                BleFailure.Error(t)
            }

            complete(result)
        }

        withContext(QueueDispatcher.value()) {
            queue.consumeEach { operation -> operation.execute() }
        }
    }
}
