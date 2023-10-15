package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.BleQueue.Timeout
import io.sellmair.pacemaker.utils.Configuration
import io.sellmair.pacemaker.utils.currentConfiguration
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
context(Configuration)
internal class BleQueue(job: Job) {

    /**
     * Timeout used for any given operation:
     * Can be passed by [BleQueue.context] or for each individual [BleQueue.enqueue] 'context
     */
    data class Timeout(val value: Duration) : Configuration.Element<Timeout> {
        override val key: Configuration.Key<Timeout> = Key

        companion object Key : Configuration.Key.WithDefault<Timeout> {
            override val default: Timeout = Timeout(30.seconds)
        }
    }

    /**
     * Title for a given operation
     */
    internal class OperationTitle(val value: String) : Configuration.Element<OperationTitle> {
        override val key: Configuration.Key<OperationTitle> = Key

        companion object Key : Configuration.Key<OperationTitle>
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    object QueueDispatcher : Configuration.Key.WithDefault<CoroutineDispatcher> {
        override val default: CoroutineDispatcher by lazy { newSingleThreadContext("BleQueue") }
    }

    suspend fun <T> enqueue(
        configuration: Configuration = Configuration.empty,
        action: suspend CoroutineScope.() -> BleResult<T>
    ): BleResult<T> {
        check(executor.isActive) { "${BleQueue::class.simpleName}: Expected executor.isActive" }
        val operation = Operation(currentConfiguration() + configuration, action)
        queue.send(operation)
        val result = operation.await()
        return result
    }

    private class Operation<T>(
        val configuration: Configuration,
        val action: suspend CoroutineScope.() -> BleResult<T>,
    ) {
        private val result = CompletableDeferred<BleResult<T>>()
        fun complete(result: BleResult<T>) = this.result.complete(result)
        suspend fun await() = result.await()
    }

    private val coroutineScope = CoroutineScope(Job(job) + get(QueueDispatcher))

    private val queue = Channel<Operation<*>>()

    private val executor = coroutineScope.launch {
        suspend fun <T> Operation<T>.execute() {
            val timeoutDuration = configuration[Timeout].value
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
