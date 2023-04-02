package io.sellmair.pacemaker.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface Ble {
    suspend fun scanForPeripherals(service: BleServiceDescriptor): Flow<BleConnectable>
    suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService
    fun close()
}

interface BleWritable {
    suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray)
}

interface BleConnectable : BleWritable {
    enum class ConnectionState {
        Disconnected,
        Connectable,
        Connecting,
        Connected
    }

    val service: BleServiceDescriptor

    val connectedScope: CoroutineScope
    val connectionState: StateFlow<ConnectionState>
    val connectIfPossible: StateFlow<Boolean>
    fun connectIfPossible(connect: Boolean)
}

interface BlePeripheralService : BleWritable {
    class ReceivedWrite(
        val deviceId: BleDeviceId,
        val characteristic: BleCharacteristicDescriptor,
        val data: ByteArray
    )

    val service: BleServiceDescriptor
    val receivedWrites: SharedFlow<ReceivedWrite>

    suspend fun startAdvertising()
}
