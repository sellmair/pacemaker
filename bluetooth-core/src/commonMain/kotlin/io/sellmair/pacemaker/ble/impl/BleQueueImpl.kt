package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.BleOperation
import io.sellmair.pacemaker.ble.BleQueue
import io.sellmair.pacemaker.ble.BleResult
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import io.sellmair.pacemaker.utils.warn
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.time.Duration

internal class BleQueueImpl(
    override val scope: CoroutineScope, private val operationTimeout: Duration
) : BleQueue, CoroutineScope by scope {

    private class CompletableOperation<T>(
        val operation: BleOperation<T>,
    ) {
        private val result = CompletableDeferred<BleQueue.Result<T>>()
        fun complete(result: BleQueue.Result<T>) = this.result.complete(result)
        suspend fun await() = result.await()
    }

    private val queue = Channel<CompletableOperation<*>>()


    override suspend fun <T> enqueue(operation: BleOperation<T>): BleQueue.Result<T> {
        check(executor.isActive) { "queue: Expected executor.isActive" }
        debug(operation, "enqueue")
        val completableOperation = CompletableOperation(operation)
        queue.send(completableOperation)
        return completableOperation.await()
    }


    private val executor = scope.launch(Dispatchers.ble) {
        suspend fun <T> CompletableOperation<T>.executeWithTimeout() {
            complete(
                try {
                    debug(operation, "executing")
                    withTimeout(operationTimeout) {
                        coroutineScope {
                            val context = object : BleQueue.Context, CoroutineScope by this {}
                            val result = with(operation) { context.invoke() }
                            debug(operation, "executed: $result")
                            when (result) {
                                is BleResult.Failure -> BleQueue.Result.Failure.Ble(result)
                                is BleResult.Success -> BleQueue.Result.Success(result.value)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    if (t is TimeoutCancellationException) {
                        debug(operation, "timeout")
                        BleQueue.Result.Failure.Timeout
                    } else {
                        debug(operation, "failure: ${t.message}")
                        BleQueue.Result.Failure.Error(t)
                    }
                }
            )
        }

        queue.consumeEach { operation -> operation.executeWithTimeout() }
    }

    companion object {
        private val log = LogTag.ble.forClass<BleQueue>()
        fun debug(operation: BleOperation<*>, message: String) = log.debug("${operation.description}: $message")
        fun warn(operation: BleOperation<*>, message: String) = log.warn("${operation.description}: $message")
    }
}