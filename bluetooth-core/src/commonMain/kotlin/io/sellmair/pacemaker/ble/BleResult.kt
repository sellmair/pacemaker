package io.sellmair.pacemaker.ble

import kotlin.time.Duration


sealed interface BleResult<out T>

fun BleResult(code: BleStatusCode): BleResult<Unit> {
    return if (code.isSuccess) BleSuccess()
    else BleFailure.StatusCode(code)
}

/**
 * Used for operations that do not provide a value
 * e.g. enabling notifications for a given BLE characteristic
 */
typealias BleUnit = BleResult<Unit>


fun BleSuccess() = BleSuccess(Unit)

/**
 * Successful result of a given operation containing value [T]
 */
class BleSuccess<out T>(val value: T) : BleResult<T> {

    override fun toString(): String {
        return "BleSuccess($value)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BleSuccess<*>) return false
        if (other.value != value) return false
        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

/**
 * Failure result for a given BLE operation
 */
sealed interface BleFailure : BleResult<Nothing> {
    /**
     * BLE operation failed with a provided [status] code
     */
    class StatusCode internal constructor(val status: BleStatusCode) : BleFailure {

        override fun toString(): String {
            return "BleFailure.StatusCode($status)"
        }

        override fun equals(other: Any?): Boolean {
            if (other !is StatusCode) return false
            if (other.status != status) return false
            return true
        }

        override fun hashCode(): Int {
            return status.hashCode()
        }
    }

    /**
     * BLE operation failed with exception [reason]
     */
    class Error internal constructor(val reason: Throwable) : BleFailure {
        override fun toString(): String {
            return "BleFailure.Error(${reason.message})"
        }
    }

    class Message constructor(val message: String) : BleFailure {
        override fun toString(): String {
            return "BleFailure.Message($message)"
        }
    }

    /**
     * BLE operation timed out
     */
    class Timeout internal constructor(val timeoutDuration: Duration) : BleFailure {
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Timeout) return false
            if (other.timeoutDuration != timeoutDuration) return false
            return true
        }

        override fun hashCode(): Int {
            return timeoutDuration.hashCode()
        }

        override fun toString(): String {
            return "BleFailure.Timeout(duration: $timeoutDuration)"
        }
    }

    /**
     * BLE operation was rejected as BLE queue is either busy or was closed
     */
    data object Rejected : BleFailure
}


inline fun <T, R> BleResult<T>.map(transform: (T) -> R): BleResult<R> {
    return when (this) {
        is BleSuccess<T> -> BleSuccess(transform(value))
        is BleFailure -> this
    }
}

inline fun <T, R> BleResult<T>.flatMap(transform: (T) -> BleResult<R>): BleResult<R> {
    return when (this) {
        is BleSuccess<T> -> transform(value)
        is BleFailure -> this
    }
}

inline fun <T> BleResult<T>.getOr(onFailure: (failure: BleFailure) -> T): T {
    return when (this) {
        is BleSuccess<T> -> this.value
        is BleFailure -> onFailure(this)
    }
}

val BleResult<*>.isSuccess get() = when(this) {
    is BleFailure -> false
    is BleSuccess -> true
}

val BleResult<*>.isFailure get() = when(this) {
    is BleFailure -> true
    is BleSuccess -> false
}