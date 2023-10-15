package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.utils.Context
import kotlinx.coroutines.CoroutineScope

typealias BleSimpleOperation = BleOperation<Unit>

interface BleOperation<out T> {
    val description: String
    suspend fun CoroutineScope.invoke(): BleResult<T>
}

internal suspend infix fun <T> BleQueue.enqueue(operation: BleOperation<T>): BleResult<T> {
    return this.enqueue(
        context = Context(BleQueue.OperationTitle(operation.description)),
        action = { with(operation) { invoke() } }
    )
}
