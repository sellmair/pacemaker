package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.impl.BleQueueImpl
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun BleQueue(scope: CoroutineScope, operationTimeout: Duration = 30.seconds): BleQueue =
    BleQueueImpl(scope, operationTimeout)

interface BleQueue {
    val scope: CoroutineScope

    sealed interface Result<out T> {
        data class Success<T>(val value: T) : Result<T>

        sealed interface Failure : Result<Nothing> {
            data class Ble(val reason: BleResult.Failure) : Failure {
                override fun toString(): String = "Ble($reason)"
            }

            data class Error(val reason: Throwable) : Failure {
                override fun toString(): String = "Error(${reason.message})"
            }

            object Timeout : Failure {
                override fun toString(): String = "Timeout"
            }
        }
    }

    interface Context : CoroutineScope

    suspend infix fun <T> enqueue(operation: BleOperation<T>): Result<T>
}

fun <T, R> BleQueue.Result<T>.map(mapper: (T) -> R): BleQueue.Result<R> {
    return when (this) {
        is BleQueue.Result.Success<T> -> BleQueue.Result.Success(mapper(value))
        is BleQueue.Result.Failure -> this
    }
}

fun <T> BleQueue.Result<T>.invokeOnSuccess(action: (T) -> Unit): BleQueue.Result<T> {
    return apply { if (this is BleQueue.Result.Success<T>) action(value) }
}

fun <T> BleQueue.Result<T>.invokeOnFailure(action: (BleQueue.Result.Failure) -> Unit): BleQueue.Result<T> {
    return apply { if (this is BleQueue.Result.Failure) action(this) }
}

fun <T> BleQueue.Result<T>.fold(
    onSuccess: (T) -> Unit = {},
    onFailure: (BleQueue.Result.Failure) -> Unit = {}
): Unit {
    return when (this) {
        is BleQueue.Result.Success<T> -> onSuccess(value)
        is BleQueue.Result.Failure -> onFailure(this)
    }
}


