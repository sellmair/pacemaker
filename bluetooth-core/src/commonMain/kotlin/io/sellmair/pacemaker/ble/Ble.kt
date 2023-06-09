package io.sellmair.pacemaker.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface Ble {
    val scope: CoroutineScope
    fun close()
    suspend fun createCentralService(service: BleServiceDescriptor): BleCentralService
    suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService
}

interface BleWritable {
    suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray): BleQueue.Result<Unit>
}

class BleReceivedValue(
    val deviceId: BleDeviceId,
    val characteristic: BleCharacteristicDescriptor,
    val data: ByteArray
)

interface BleConnectable {
    enum class ConnectionState {
        Disconnected,
        Connecting,
        Connected
    }

    val deviceName: String?
    val deviceId: BleDeviceId
    val service: BleServiceDescriptor

    val connection: SharedFlow<BleConnection>
    val connectionState: StateFlow<ConnectionState>
    val connectIfPossible: StateFlow<Boolean>
    fun connectIfPossible(connect: Boolean)
}

interface BleConnection : BleWritable {
    val deviceId: BleDeviceId
    val scope: CoroutineScope
    val service: BleServiceDescriptor
    val receivedValues: SharedFlow<BleReceivedValue>


    suspend fun enableNotifications(characteristic: BleCharacteristicDescriptor): BleQueue.Result<Unit>
    suspend fun requestRead(characteristic: BleCharacteristicDescriptor): BleQueue.Result<ByteArray>
}

interface BlePeripheralService : BleWritable {
    val service: BleServiceDescriptor
    val receivedWrites: SharedFlow<BleReceivedValue>

    suspend fun startAdvertising()
}

interface BleCentralService {
    /**
     * Will replay all connectables on subscribe!
     */
    val connectables: SharedFlow<BleConnectable>
    fun startScanning()
}