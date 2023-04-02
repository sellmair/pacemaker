package io.sellmair.pacemaker.ble

typealias BleSimpleResult = BleResult<Unit>

sealed class BleResult<out T> {
    data class Success<T>(val value: T) : BleResult<T>()

    sealed class Failure : BleResult<Nothing>() {
        data class Code(val status: BleStatusCode) : Failure()
        data class Error(val reason: Throwable) : Failure()
        data class Message(val message: String) : Failure()
    }

    final override fun toString(): String = when (this) {
        is Failure.Code -> "Failure: $status"
        is Failure.Error -> "Failure: ${reason.message}"
        is Failure.Message -> "Failure: $message"
        is Success -> "Success"
    }

    companion object {
        val Success = BleResult.Success(Unit)
    }
}
