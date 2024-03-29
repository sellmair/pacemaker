package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.utils.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

typealias BleSimpleOperation = BleOperation<Unit>

interface BleOperation<out T> {
    val description: String
    suspend fun CoroutineScope.invoke(): BleResult<T>
}

internal suspend infix fun <T> BleQueue.enqueue(operation: BleOperation<T>): BleResult<T> {
    return withContext(BleQueue.OperationTitle.invoke(operation.description)) {
        enqueue { with(operation) { invoke() } }
    }
}
