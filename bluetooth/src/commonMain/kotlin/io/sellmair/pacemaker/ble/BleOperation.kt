package io.sellmair.pacemaker.ble

typealias BleSimpleOperation = BleOperation<Unit>

interface BleOperation<out T> {
    val description: String
    suspend fun BleQueue.Context.invoke(): BleResult<T>
}

